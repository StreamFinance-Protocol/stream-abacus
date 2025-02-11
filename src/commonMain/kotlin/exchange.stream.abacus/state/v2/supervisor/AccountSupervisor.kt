package exchange.stream.abacus.state.v2.supervisor

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import exchange.stream.abacus.output.Compliance
import exchange.stream.abacus.output.ComplianceAction
import exchange.stream.abacus.output.ComplianceStatus
import exchange.stream.abacus.output.Notification
import exchange.stream.abacus.output.PerpetualState
import exchange.stream.abacus.output.Restriction
import exchange.stream.abacus.output.UsageRestriction
import exchange.stream.abacus.processor.router.skip.SkipRoutePayloadProcessor
import exchange.stream.abacus.protocols.LocalTimerProtocol
import exchange.stream.abacus.protocols.QueryType
import exchange.stream.abacus.protocols.ThreadingType
import exchange.stream.abacus.protocols.TransactionCallback
import exchange.stream.abacus.protocols.TransactionType
import exchange.stream.abacus.responses.SocketInfo
import exchange.stream.abacus.state.app.adaptors.V4TransactionErrors
import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.state.manager.ApiData
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.state.manager.HumanReadableCancelOrderPayload
import exchange.stream.abacus.state.manager.HumanReadableDepositPayload
import exchange.stream.abacus.state.manager.HumanReadablePlaceOrderPayload
import exchange.stream.abacus.state.manager.HumanReadableSubaccountTransferPayload
import exchange.stream.abacus.state.manager.HumanReadableTriggerOrdersPayload
import exchange.stream.abacus.state.manager.HumanReadableWithdrawPayload
import exchange.stream.abacus.state.manager.StatsigConfig
import exchange.stream.abacus.state.manager.pendingCctpWithdraw
import exchange.stream.abacus.state.manager.processingCctpWithdraw
import exchange.stream.abacus.state.manager.utils.Address
import exchange.stream.abacus.state.manager.utils.EvmAddress
import exchange.stream.abacus.state.manager.utils.StreamAddress
import exchange.stream.abacus.state.model.AdjustIsolatedMarginInputField
import exchange.stream.abacus.state.model.ClosePositionInputField
import exchange.stream.abacus.state.model.TradeInputField
import exchange.stream.abacus.state.model.TradingStateMachine
import exchange.stream.abacus.state.model.TriggerOrdersInputField
import exchange.stream.abacus.state.model.account
import exchange.stream.abacus.state.model.onChainAccountBalances
import exchange.stream.abacus.state.model.onChainUserFeeTier
import exchange.stream.abacus.state.model.onChainUserStats
import exchange.stream.abacus.utils.AnalyticsUtils
import exchange.stream.abacus.utils.CoroutineTimer
import exchange.stream.abacus.utils.IMap
import exchange.stream.abacus.utils.Logger
import exchange.stream.abacus.utils.MIN_USDC_AMOUNT_FOR_AUTO_SWEEP
import exchange.stream.abacus.utils.SLIPPAGE_PERCENT
import exchange.stream.abacus.utils.iMapOf
import exchange.stream.abacus.utils.toJsonPrettyPrint
import exchange.stream.abacus.utils.toNobleAddress
import kollections.iListOf
import kollections.iSetOf
import kollections.toIMap
import kotlinx.datetime.Instant
import kotlin.collections.mutableMapOf
import kotlin.time.Duration.Companion.days

internal open class AccountSupervisor(
    stateMachine: TradingStateMachine,
    helper: NetworkHelper,
    analyticsUtils: AnalyticsUtils,
    internal val configs: AccountConfigs,
    internal val accountAddress: String,
) : DynamicNetworkSupervisor(stateMachine, helper, analyticsUtils) {
    val subaccounts = mutableMapOf<Int, SubaccountSupervisor>()

    private val accountBalancePollingDuration = 10.0
    private var accountBalancesTimer: LocalTimerProtocol? = null
        set(value) {
            if (field !== value) {
                field?.cancel()
                field = value
            }
        }

    private val nobleBalancePollingDuration = 10.0
    private var nobleBalancesTimer: LocalTimerProtocol? = null
        set(value) {
            if (field !== value) {
                field?.cancel()
                field = value
            }
        }

    private val subaccountsPollingDelay = 15.0
    private var subaccountsTimer: LocalTimerProtocol? = null
        set(value) {
            if (field !== value) {
                field?.cancel()
                field = value
            }
        }

    var sourceAddress: String? = null
        internal set(value) {
            if (field != value) {
                val oldValue = field
                field = value
                didSetSourceAddress(sourceAddress, oldValue)
            }
        }

    var cosmosWalletConnected: Boolean? = false
        internal set(value) {
            field = value
            if (value == true) {
                nobleBalancesTimer?.cancel()
                nobleBalancesTimer = null
            }
        }
    private var sourceAddressRestriction: Restriction? = null
        set(value) {
            if (field != value) {
                field = value
                didSetSourceAddressRestriction(value)
            }
        }

    internal var addressRestriction: UsageRestriction? = null
        set(value) {
            if (field != value) {
                field = value
                didSetAddressRestriction(value)
            }
        }

    internal open var restriction: UsageRestriction = UsageRestriction.noRestriction
        set(value) {
            if (field != value) {
                field = value
                didSetRestriction(value)
            }
        }

    private var compliance: Compliance = Compliance(null, ComplianceStatus.COMPLIANT, null, null)
        set(value) {
            if (field != value) {
                field = value
                didSetComplianceStatus(value)
            }
        }

    private var screenAccountAddressTimer: LocalTimerProtocol? = null
        set(value) {
            if (field !== value) {
                field?.cancel()
                field = value
            }
        }

    private var accountAddressRestriction: Restriction? = null
        set(value) {
            if (field != value) {
                field = value
                didSetAccountAddressRestriction(value)
            }
        }

    private val addressRetryDuration = 10.0
    private val addressContinuousMonitoringDuration = 60.0 * 60.0

    private var screenSourceAddressTimer: LocalTimerProtocol? = null
        set(value) {
            if (field !== value) {
                field?.cancel()
                field = value
            }
        }

    internal var subaccountNumber: Int? // Desired subaccountNumber
        get() {
            return subaccount?.subaccountNumber
        }
        set(value) {
            subaccounts.keys.filter { it != value }.forEach {
                subaccounts[it]?.forceRelease()
                subaccounts.remove(it)
            }
            if (subaccounts.contains(value).not() && value != null) {
                subscribeToSubaccount(value)
            }
        }

    internal val connectedSubaccountNumber: Int?
        get() {
            return if (subaccount?.realized == true) subaccount?.subaccountNumber else null
        }

    private val userStatsPollingDuration = 60.0
    private var userStatsTimer: LocalTimerProtocol? = null
        set(value) {
            if (field !== value) {
                field?.cancel()
                field = value
            }
        }

    init {
        screenAccountAddress()
        complianceScreen(StreamAddress(accountAddress), ComplianceAction.CONNECT)
    }

    internal fun subscribeToSubaccount(subaccountNumber: Int) {
        val isSubaccountRealized = stateMachine.state?.subaccount(subaccountNumber) != null
        val subaccountSupervisor = subaccounts[subaccountNumber]
        subaccountSupervisor?.retain()
            ?: run {
                val newSubaccountSupervisor =
                    SubaccountSupervisor(
                        stateMachine,
                        helper,
                        analyticsUtils,
                        configs.subaccountConfigs,
                        accountAddress,
                        subaccountNumber,
                    )
                newSubaccountSupervisor.readyToConnect = readyToConnect
                newSubaccountSupervisor.indexerConnected = indexerConnected
                newSubaccountSupervisor.socketConnected = socketConnected
                newSubaccountSupervisor.validatorConnected = validatorConnected
                subaccounts[subaccountNumber] = newSubaccountSupervisor
            }

        // if this is the first realized subaccount, retrieve user fee tier and user stats
        if (validatorConnected && isSubaccountRealized && !subaccounts.values.any { it.realized }) {
            if (configs.retrieveUserFeeTier) {
                retrieveUserFeeTier()
            }
            if (configs.retrieveUserStats) {
                retrieveUserStats()
            }
        }

        subaccounts[subaccountNumber]?.realized = isSubaccountRealized
    }

    internal fun unsubscribeFromSubaccount(subaccountNumber: Int) {
        val accountSupervisor = subaccounts[subaccountNumber] ?: return
        accountSupervisor.release()
        if (accountSupervisor.retainCount == 0) {
            subaccounts.remove(subaccountNumber)
        }
    }

    override fun didSetReadyToConnect(readyToConnect: Boolean) {
        super.didSetReadyToConnect(readyToConnect)
        for ((_, subaccountSupervisor) in subaccounts) {
            subaccountSupervisor.readyToConnect = readyToConnect
        }
    }

    override fun didSetIndexerConnected(indexerConnected: Boolean) {
        super.didSetIndexerConnected(indexerConnected)
        for ((_, subaccountSupervisor) in subaccounts) {
            subaccountSupervisor.indexerConnected = indexerConnected
        }

        if (indexerConnected) {
            if (configs.retrieveSubaccounts) {
                retrieveSubaccounts()
            }
        } else {
            subaccountsTimer = null
            screenAccountAddressTimer = null
            screenSourceAddressTimer = null
        }
    }

    override fun didSetValidatorConnected(validatorConnected: Boolean) {
        super.didSetValidatorConnected(validatorConnected)
        for ((_, subaccountSupervisor) in subaccounts) {
            subaccountSupervisor.validatorConnected = validatorConnected
        }

        if (validatorConnected) {
            if (subaccounts.values.any { it.realized }) {
                if (configs.retrieveUserFeeTier) {
                    retrieveUserFeeTier()
                }
                if (configs.retrieveUserStats) {
                    retrieveUserStats()
                }
            }
            if (configs.retrieveBalances) {
                retrieveBalances()
            }
            if (configs.transferNobleBalances) {
                retrieveNobleBalance()
            }
        } else {
            userStatsTimer = null
            accountBalancesTimer = null
            nobleBalancesTimer = null
        }
    }

    override fun didSetSocketConnected(socketConnected: Boolean) {
        super.didSetSocketConnected(socketConnected)
        for ((_, subaccountSupervisor) in subaccounts) {
            subaccountSupervisor.socketConnected = socketConnected
        }
    }

    private fun retrieveSubaccounts() {
        val url = accountUrl()
        if (url != null) {
            helper.get(
                url,
                null,
                null,
                callback = { _, response, httpCode, _ ->
                    val isValidResponse = helper.success(httpCode) && response != null
                    if (isValidResponse) {
                        response?.let { retrievedSubaccounts(it) }
                    }
                    if (!isValidResponse && httpCode != 403) {
                        subaccountNumber = 0
                        subaccountsTimer =
                            helper.ioImplementations.timer?.schedule(
                                subaccountsPollingDelay,
                                null,
                            ) {
                                retrieveSubaccounts()
                                false
                            }
                    }
                },
            )
        }
    }

    open fun retrievedSubaccounts(response: String) {
        val oldState = stateMachine.state
        update(stateMachine.account(response), oldState)

        if (subaccounts.isNotEmpty()) {
            for ((subaccountNumber, subaccount) in subaccounts) {
                subaccount.realized = canConnectTo(subaccountNumber)
            }
        } else {
            subaccountNumber = 0
        }
    }

    private fun accountUrl(): String? {
        val url = helper.configs.privateApiUrl("account")
        return if (url != null) {
            "$url/$accountAddress"
        } else {
            null
        }
    }

    private fun retrieveUserFeeTier() {
        val params = iMapOf("address" to accountAddress)
        val paramsInJson = helper.jsonEncoder.encode(params)
        helper.getOnChain(QueryType.UserFeeTier, paramsInJson) { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainUserFeeTier(response), oldState)
        }
    }

    private fun retrieveUserStats() {
        val timer = helper.ioImplementations.timer ?: CoroutineTimer.instance
        userStatsTimer =
            timer.schedule(0.0, userStatsPollingDuration) {
                if (validatorConnected && subaccount != null) {
                    getUserStats()
                    true
                } else {
                    false
                }
            }
    }

    private fun getUserStats() {
        val params = iMapOf("address" to accountAddress)
        val paramsInJson = helper.jsonEncoder.encode(params)
        helper.getOnChain(QueryType.UserStats, paramsInJson) { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainUserStats(response), oldState)
        }
    }

    private fun retrieveBalances() {
        val timer = helper.ioImplementations.timer ?: CoroutineTimer.instance
        accountBalancesTimer =
            timer.schedule(0.0, accountBalancePollingDuration) {
                if (validatorConnected) {
                    getAccountBalances()
                    true
                } else {
                    false
                }
            }
    }

    private fun getAccountBalances() {
        helper.getOnChain(QueryType.GetAccountBalances, "") { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainAccountBalances(response), oldState)
        }
    }

    private fun retrieveNobleBalance() {
        if (cosmosWalletConnected == true) {
            nobleBalancesTimer = null
            return
        }
        val timer = helper.ioImplementations.timer ?: CoroutineTimer.instance
        nobleBalancesTimer =
            timer.schedule(0.0, nobleBalancePollingDuration) {
                if (validatorConnected) {
                    getNobleBalance()
                    true
                } else {
                    false
                }
            }
    }

    private fun getNobleBalance() {
        helper.getOnChain(QueryType.GetNobleBalance, "") { response ->
            val balance = helper.parser.decodeJsonObject(response)
            if (balance != null) {
                val amount = helper.parser.asDecimal(balance["amount"])
//                minimum usdc required for successful tx (gas fee)
                if (amount != null && amount > MIN_USDC_AMOUNT_FOR_AUTO_SWEEP) {
                    if (processingCctpWithdraw) {
                        return@getOnChain
                    }
//                    if pending withdrawal, perform CCTP Withdrawal
                    pendingCctpWithdraw?.let { walletState ->
                        processingCctpWithdraw = true
                        val callback = walletState.callback
//                        if walletState has a singleMessagePayload, it's a single message tx
//                        otherwise walletState represents a multi message tx (like smart relay) and we should use the relevant transaction
                        val transactionType = if (walletState.multiMessagePayload == null) TransactionType.CctpWithdraw else TransactionType.CctpMultiMsgWithdraw
                        val payload = if (walletState.multiMessagePayload == null) walletState.singleMessagePayload else walletState.multiMessagePayload
                        helper.transaction(
                            transactionType,
                            payload,
                        ) { hash ->
                            val error = helper.parseTransactionResponse(hash)
                            if (error != null) {
                                Logger.e { "TransactionType.CctpWithdraw error: $error" }
                                callback?.let { it -> helper.send(error, it, hash) }
                            } else {
                                callback?.let { it -> helper.send(null, it, hash) }
                            }
                            pendingCctpWithdraw = null
                            processingCctpWithdraw = false
                        }
                    }
//                        else, transfer noble balance back to dydx
                        ?: run { sweepNobleBalanceToDydx(amount) }
                } else if (balance["error"] != null) {
                    Logger.e { "Error checking noble balance: $response" }
                }
            }
        }
    }

    private fun canConnectTo(subaccountNumber: Int): Boolean {
        return stateMachine.state?.subaccount(subaccountNumber) != null
    }

    internal fun retrieveHistoricalPnls() {
        for ((_, subaccount) in subaccounts) {
            subaccount.retrieveHistoricalPnls()
        }
    }

    private fun sweepNobleBalanceToDydx(amount: BigDecimal) {
        if (StatsigConfig.useSkip) {
            sweepNobleBalanceToDydxSkip(amount = amount)
        } else {
            sweepNobleBalanceToDydxSquid(amount = amount)
        }
    }

    private fun sweepNobleBalanceToDydxSquid(amount: BigDecimal) {
        val url = helper.configs.squidRoute()
        val fromChain = helper.configs.nobleChainId()
        val fromToken = helper.configs.nobleDenom
        val nobleAddress = accountAddress.toNobleAddress()
        val chainId = helper.environment.streamChainId
        val squidIntegratorId = helper.environment.squidIntegratorId
        val dydxTokenDemon = helper.environment.tokens["usdc"]?.denom
        if (url != null &&
            fromChain != null &&
            fromToken != null &&
            nobleAddress != null &&
            chainId != null &&
            dydxTokenDemon != null &&
            squidIntegratorId != null
        ) {
            val params: Map<String, String> =
                mapOf(
                    "fromChain" to fromChain,
                    "fromToken" to fromToken,
                    "fromAddress" to nobleAddress,
                    "fromAmount" to amount.toPlainString(),
                    "toChain" to chainId,
                    "toToken" to dydxTokenDemon,
                    "toAddress" to accountAddress.toString(),
                    "slippage" to "1",
                    "enableForecall" to "false",
                )
            val header =
                iMapOf(
                    "x-integrator-id" to squidIntegratorId,
                )
            helper.get(url, params, header) { _, response, code, _ ->
                if (response != null) {
                    val json = helper.parser.decodeJsonObject(response)
                    val ibcPayload =
                        helper.parser.asString(
                            helper.parser.value(
                                json,
                                "route.transactionRequest.data",
                            ),
                        )
                    if (ibcPayload != null) {
                        helper.transaction(TransactionType.SendNobleIBC, ibcPayload) {
                            val error = helper.parseTransactionResponse(it)
                            if (error != null) {
                                Logger.e { "sweepNobleBalanceToDydxSquid error: $error" }
                            }
                        }
                    }
                } else {
                    Logger.e { "sweepNobleBalanceToDydxSquid error, code: $code" }
                }
            }
        }
    }

    private fun sweepNobleBalanceToDydxSkip(amount: BigDecimal) {
        val url = helper.configs.skipV2MsgsDirect()
        val fromChain = helper.configs.nobleChainId()
        val fromToken = helper.configs.nobleDenom
        val nobleAddress = accountAddress.toNobleAddress() ?: return
        val chainId = helper.environment.streamChainId ?: return
        val dydxTokenDemon = helper.environment.tokens["usdc"]?.denom ?: return
        val body: Map<String, Any> = mapOf(
            "amount_in" to amount.toPlainString(),
//              from noble denom and chain
            "source_asset_denom" to fromToken,
            "source_asset_chain_id" to fromChain,
//              to dydx denom and chain
            "dest_asset_denom" to dydxTokenDemon,
            "dest_asset_chain_id" to chainId,
            "chain_ids_to_addresses" to mapOf(
                fromChain to nobleAddress,
                chainId to accountAddress,
            ),
            "slippage_tolerance_percent" to SLIPPAGE_PERCENT,
        )
        val header =
            iMapOf(
                "Content-Type" to "application/json",
            )
        helper.post(url, header, body.toJsonPrettyPrint()) { _, response, code, _ ->
            if (response != null) {
                val json = helper.parser.decodeJsonObject(response)
                if (json != null) {
                    val skipRoutePayloadProcessor = SkipRoutePayloadProcessor(parser = helper.parser)
                    val processedPayload = skipRoutePayloadProcessor.received(existing = mapOf(), payload = json)
                    val ibcPayload =
                        helper.parser.asString(
                            processedPayload.get("data"),
                        )
                    if (ibcPayload != null) {
                        helper.transaction(TransactionType.SendNobleIBC, ibcPayload) {
                            val error = helper.parseTransactionResponse(it)
                            if (error != null) {
                                Logger.e { "sweepNobleBalanceToDydxSkip error: $error" }
                            }
                        }
                    }
                }
            } else {
                Logger.e { "sweepNobleBalanceToDydxSkip error, code: $code" }
            }
        }
    }

    private fun handleComplianceResponse(response: String?, httpCode: Int, address: Address?): ComplianceStatus {
        var complianceStatus = ComplianceStatus.UNKNOWN
        var updatedAt: String? = null
        var expiresAt: String? = null
        if (helper.success(httpCode) && response != null) {
            val res = helper.parser.decodeJsonObject(response)?.toIMap()
            complianceStatus =
                helper.parser.asString(res?.get("status"))?.let { ComplianceStatus.valueOf(it) }
                    ?: ComplianceStatus.UNKNOWN
            updatedAt = helper.parser.asString(res?.get("updatedAt"))
            if (updatedAt != null) {
                expiresAt =
                    try {
                        Instant.parse(updatedAt).plus(7.days).toString()
                    } catch (e: IllegalArgumentException) {
                        Logger.e { "Error parsing compliance updatedAt: $updatedAt" }
                        null
                    }
            }
        }
        // If we are screening an EVM address we only update when the compliance status is blocked
        if (address is StreamAddress || complianceStatus == ComplianceStatus.BLOCKED) {
            compliance =
                compliance.copy(
                    status = complianceStatus,
                    updatedAt = updatedAt,
                    expiresAt = expiresAt,
                )
        }
        return complianceStatus
    }

    private fun updateCompliance(
        address: StreamAddress,
        status: ComplianceStatus,
        complianceAction: ComplianceAction
    ) {
        val message = "Compliance verification message"
        val payload =
            helper.jsonEncoder.encode(
                mapOf(
                    "message" to message,
                    "action" to complianceAction.toString(),
                    "status" to status.toString(),
                ),
            )
        helper.transaction(
            TransactionType.SignCompliancePayload,
            payload,
        ) { additionalPayload ->
            val error = parseTransactionResponse(additionalPayload)
            val result = helper.parser.decodeJsonObject(additionalPayload)

            if (error == null && result != null) {
                val url = complianceGeoblockUrl()
                val signedMessage = helper.parser.asString(result["signedMessage"])
                val publicKey = helper.parser.asString(result["publicKey"])
                val timestamp = helper.parser.asString(result["timestamp"])

                val isUrlAndKeysPresent =
                    url != null &&
                        signedMessage != null &&
                        publicKey != null &&
                        timestamp != null
                val isStatusValid = status != ComplianceStatus.UNKNOWN

                if (isUrlAndKeysPresent && isStatusValid) {
                    val body: IMap<String, String> =
                        iMapOf(
                            "address" to address.rawAddress,
                            "message" to message,
                            "currentStatus" to status.toString(),
                            "action" to complianceAction.toString(),
                            "signedMessage" to signedMessage!!,
                            "pubkey" to publicKey!!,
                            "timestamp" to timestamp!!,
                        )
                    val header =
                        iMapOf(
                            "Content-Type" to "application/json",
                        )
                    helper.post(
                        url!!,
                        header,
                        body.toJsonPrettyPrint(),
                        callback = { _, response, httpCode, _ ->
                            handleComplianceResponse(response, httpCode, address)
                            // retrieve the subaccounts if it does not exist yet. It is possible that the initial
                            // subaccount retrieval failed due to 403 before updating the compliance status.
                            if (helper.success(httpCode) && response != null && subaccounts.isEmpty()) {
                                retrieveSubaccounts()
                            }
                        },
                    )
                } else {
                    compliance = compliance.copy(status = ComplianceStatus.UNKNOWN)
                }
            } else {
                compliance = compliance.copy(status = ComplianceStatus.UNKNOWN)
            }
        }
    }

    private fun complianceScreen(address: Address, action: ComplianceAction? = null) {
        val url = complianceScreenUrl(address.rawAddress)
        if (url != null) {
            helper.get(
                url,
                null,
                null,
                callback = { _, response, httpCode, _ ->
                    val complianceStatus = handleComplianceResponse(response, httpCode, address)
                    if (address is StreamAddress && action != null) {
                        updateCompliance(address, complianceStatus, action)
                    }
                },
            )
        }
    }

    internal open fun triggerCompliance(action: ComplianceAction, callback: TransactionCallback?) {
        if (compliance.status != ComplianceStatus.UNKNOWN) {
            updateCompliance(StreamAddress(accountAddress), compliance.status, action)
            if (callback != null) {
                callback(true, null, null)
            }
        } else if (callback != null) {
            callback(false, V4TransactionErrors.error(null, "No account address"), null)
        }
    }

    private fun complianceScreenUrl(address: String): String? {
        val url = helper.configs.publicApiUrl("complianceScreen") ?: return null
        return "$url/$address"
    }

    private fun complianceGeoblockUrl(): String? {
        return helper.configs.publicApiUrl("complianceGeoblock")
    }

    open fun screenSourceAddress() {
        val address = sourceAddress
        if (address != null) {
            screen(address) { restriction ->
                when (restriction) {
                    Restriction.USER_RESTRICTED,
                    Restriction.NO_RESTRICTION,
                    Restriction.USER_RESTRICTION_UNKNOWN -> {
                        sourceAddressRestriction = restriction
                    }
                    else -> {
                        throw Exception("Unexpected restriction value")
                    }
                }
                rerunAddressScreeningDelay(sourceAddressRestriction)?.let {
                    val timer = helper.ioImplementations.timer ?: CoroutineTimer.instance
                    screenSourceAddressTimer =
                        timer.schedule(it, it) {
                            screenSourceAddress()
                            true
                        }
                }
            }
        } else {
            sourceAddressRestriction = Restriction.NO_RESTRICTION
        }
    }

    private fun didSetAccountAddressRestriction(accountAddressRestriction: Restriction?) {
        updateAddressRestriction()
    }

    open fun screenAccountAddress() {
        val address = accountAddress
        screen(address) { restriction ->
            when (restriction) {
                Restriction.USER_RESTRICTED,
                Restriction.NO_RESTRICTION,
                Restriction.USER_RESTRICTION_UNKNOWN -> {
                    accountAddressRestriction = restriction
                }
                else -> {
                    throw Exception("Unexpected restriction value")
                }
            }
            rerunAddressScreeningDelay(accountAddressRestriction)?.let {
                val timer = helper.ioImplementations.timer ?: CoroutineTimer.instance
                screenAccountAddressTimer =
                    timer.schedule(it, it) {
                        screenAccountAddress()
                        true
                    }
            }
        }
    }

    private fun rerunAddressScreeningDelay(restriction: Restriction?): Double? {
        return when (restriction) {
            Restriction.NO_RESTRICTION -> addressContinuousMonitoringDuration
            Restriction.USER_RESTRICTION_UNKNOWN -> addressRetryDuration
            else -> null
        }
    }

    open fun screen(address: String, callback: ((Restriction) -> Unit)) {
        val url = screenUrl()
        if (url != null) {
            helper.get(
                url,
                mapOf("address" to address),
                null,
                callback = { _, response, httpCode, _ ->
                    if (helper.success(httpCode) && response != null) {
                        val payload = helper.parser.decodeJsonObject(response)?.toIMap()
                        if (payload != null) {
                            val restricted =
                                helper.parser.asBool(payload["restricted"]) ?: false
                            callback(
                                if (restricted) {
                                    Restriction.USER_RESTRICTED
                                } else {
                                    Restriction.NO_RESTRICTION
                                },
                            )
                        } else {
                            callback(Restriction.USER_RESTRICTION_UNKNOWN)
                        }
                    } else {
                        if (httpCode == 403) {
                            // It could be 403 due to GEOBLOCKED
                            val usageRestriction = restrictionReason(response)
                            callback(usageRestriction.restriction)
                        } else {
                            callback(Restriction.USER_RESTRICTION_UNKNOWN)
                        }
                    }
                },
            )
        }
    }

    private fun screenUrl(): String? {
        return helper.configs.publicApiUrl("screen")
    }

    internal fun restrictionReason(response: String?): UsageRestriction {
        return if (response != null) {
            val json = helper.parser.decodeJsonObject(response)
            val errors = helper.parser.asList(helper.parser.value(json, "errors"))
            val geoRestriciton =
                errors?.firstOrNull { error ->
                    val code = helper.parser.asString(helper.parser.value(error, "code"))
                    code?.contains("GEOBLOCKED") == true
                }

            if (geoRestriciton !== null) {
                UsageRestriction.http403Restriction
            } else {
                UsageRestriction.userRestriction
            }
        } else {
            UsageRestriction.http403Restriction
        }
    }

    private fun didSetSourceAddress(sourceAddress: String?, oldValue: String?) {
        screenSourceAddressTimer = null
        sourceAddressRestriction = null
        if (sourceAddress != null) {
            screenSourceAddress()
            complianceScreen(EvmAddress(sourceAddress))
        }
    }

    private fun didSetSourceAddressRestriction(sourceAddressRestriction: Restriction?) {
        updateAddressRestriction()
    }

    private fun updateAddressRestriction() {
        val restrictions: Set<Restriction?> =
            iSetOf(accountAddressRestriction, sourceAddressRestriction)
        addressRestriction =
            if (restrictions.contains(Restriction.USER_RESTRICTED)) {
                UsageRestriction.userRestriction
            } else if (restrictions.contains(Restriction.USER_RESTRICTION_UNKNOWN)) {
                UsageRestriction.userRestrictionUnknown
            } else {
                if (sourceAddressRestriction == null && accountAddressRestriction == null) {
                    null
                } else {
                    UsageRestriction.noRestriction
                }
            }
    }

    private fun didSetAddressRestriction(addressRestriction: UsageRestriction?) {
        updateRestriction()
    }

    internal open fun updateRestriction() {
        restriction = addressRestriction ?: UsageRestriction.noRestriction
    }

    private fun didSetRestriction(restriction: UsageRestriction?) {
        val state = stateMachine.state
        stateMachine.state =
            PerpetualState(
                state?.assets,
                state?.marketsSummary,
                state?.orderbooks,
                state?.candles,
                state?.trades,
                state?.historicalFundings,
                state?.wallet,
                state?.account,
                state?.historicalPnl,
                state?.fills,
                state?.transfers,
                state?.fundingPayments,
                state?.configs,
                state?.input,
                state?.availableSubaccountNumbers ?: iListOf(),
                state?.transferStatuses,
                state?.trackStatuses,
                restriction,
                state?.compliance,
            )
        helper.ioImplementations.threading?.async(ThreadingType.main) {
            helper.stateNotification?.stateChanged(
                stateMachine.state,
                StateChanges(
                    iListOf(Changes.restriction),
                ),
            )
        }
    }

    private fun didSetComplianceStatus(compliance: Compliance) {
        val state = stateMachine.state
        stateMachine.state =
            PerpetualState(
                state?.assets,
                state?.marketsSummary,
                state?.orderbooks,
                state?.candles,
                state?.trades,
                state?.historicalFundings,
                state?.wallet,
                state?.account,
                state?.historicalPnl,
                state?.fills,
                state?.transfers,
                state?.fundingPayments,
                state?.configs,
                state?.input,
                state?.availableSubaccountNumbers ?: iListOf(),
                state?.transferStatuses,
                state?.trackStatuses,
                state?.restriction,
                Compliance(
                    state?.compliance?.geo,
                    compliance.status,
                    compliance.updatedAt,
                    compliance.expiresAt,
                ),
            )
        helper.ioImplementations.threading?.async(ThreadingType.main) {
            helper.stateNotification?.stateChanged(
                stateMachine.state,
                StateChanges(
                    iListOf(Changes.compliance),
                ),
            )
        }
    }

    internal fun receiveSubaccountChannelSocketData(
        info: SocketInfo,
        subaccountNumber: Int,
        payload: IMap<String, Any>,
        height: BlockAndTime?,
    ) {
        val subaccount = subaccounts[subaccountNumber] ?: return
        subaccount.receiveSubaccountChannelSocketData(info, payload, height)
    }
}

// Extension properties to help with current singular subaccount

private val AccountSupervisor.subaccount: SubaccountSupervisor?
    get() {
        return if (subaccounts.count() == 1) subaccounts.values.firstOrNull() else null
    }

internal val AccountSupervisor.notifications: IMap<String, Notification>
    get() {
        return subaccount?.notifications ?: iMapOf()
    }

internal fun AccountSupervisor.trade(data: String?, type: TradeInputField?) {
    subaccount?.trade(data, type)
}

internal fun AccountSupervisor.closePosition(data: String?, type: ClosePositionInputField) {
    subaccount?.closePosition(data, type)
}

internal fun AccountSupervisor.triggerOrders(data: String?, type: TriggerOrdersInputField?) {
    subaccount?.triggerOrders(data, type)
}

internal fun AccountSupervisor.adjustIsolatedMargin(
    data: String?,
    type: AdjustIsolatedMarginInputField?,
) {
    subaccount?.adjustIsolatedMargin(data, type)
}

internal fun AccountSupervisor.placeOrderPayload(
    currentHeight: Int?
): HumanReadablePlaceOrderPayload? {
    return subaccount?.placeOrderPayload(currentHeight)
}

internal fun AccountSupervisor.closePositionPayload(
    currentHeight: Int?
): HumanReadablePlaceOrderPayload? {
    return subaccount?.closePositionPayload(currentHeight)
}

internal fun AccountSupervisor.triggerOrdersPayload(
    currentHeight: Int?
): HumanReadableTriggerOrdersPayload? {
    return subaccount?.triggerOrdersPayload(currentHeight)
}

internal fun AccountSupervisor.adjustIsolatedMarginPayload(): HumanReadableSubaccountTransferPayload? {
    return subaccount?.adjustIsolatedMarginPayload()
}

internal fun AccountSupervisor.cancelOrderPayload(
    orderId: String
): HumanReadableCancelOrderPayload? {
    return subaccount?.cancelOrderPayload(orderId)
}

internal fun AccountSupervisor.depositPayload(): HumanReadableDepositPayload? {
    return subaccount?.depositPayload()
}

internal fun AccountSupervisor.withdrawPayload(): HumanReadableWithdrawPayload? {
    return subaccount?.withdrawPayload()
}

internal fun AccountSupervisor.subaccountTransferPayload(): HumanReadableSubaccountTransferPayload? {
    return subaccount?.subaccountTransferPayload()
}

internal fun AccountSupervisor.commitPlaceOrder(
    currentHeight: Int?,
    callback: TransactionCallback
): HumanReadablePlaceOrderPayload? {
    return subaccount?.commitPlaceOrder(currentHeight, callback)
}

internal fun AccountSupervisor.commitTriggerOrders(
    currentHeight: Int?,
    callback: TransactionCallback
): HumanReadableTriggerOrdersPayload? {
    return subaccount?.commitTriggerOrders(currentHeight, callback)
}

internal fun AccountSupervisor.commitAdjustIsolatedMargin(
    callback: TransactionCallback
): HumanReadableSubaccountTransferPayload? {
    return subaccount?.commitAdjustIsolatedMargin(callback)
}

internal fun AccountSupervisor.commitClosePosition(
    currentHeight: Int?,
    callback: TransactionCallback
): HumanReadablePlaceOrderPayload? {
    return subaccount?.commitClosePosition(currentHeight, callback)
}

internal fun AccountSupervisor.stopWatchingLastOrder() {
    subaccount?.stopWatchingLastOrder()
}

internal fun AccountSupervisor.faucet(amount: Double, callback: TransactionCallback) {
    subaccount?.faucet(amount, callback)
}

internal fun AccountSupervisor.cancelOrder(orderId: String, callback: TransactionCallback) {
    subaccount?.cancelOrder(orderId = orderId, callback = callback)
}

internal fun AccountSupervisor.orderCanceled(orderId: String) {
    subaccount?.orderCanceled(orderId)
}

internal fun AccountSupervisor.refresh(data: ApiData) {
    when (data) {
        ApiData.HISTORICAL_PNLS -> {
            subaccount?.refresh(data)
        }
    }
}
