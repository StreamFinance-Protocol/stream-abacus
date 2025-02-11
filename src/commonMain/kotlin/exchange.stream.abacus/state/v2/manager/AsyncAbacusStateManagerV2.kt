package exchange.stream.abacus.state.v2.manager

import exchange.stream.abacus.di.AbacusScope
import exchange.stream.abacus.di.Deployment
import exchange.stream.abacus.di.DeploymentUri
import exchange.stream.abacus.output.ComplianceAction
import exchange.stream.abacus.output.Restriction
import exchange.stream.abacus.output.input.SelectionOption
import exchange.stream.abacus.protocols.DataNotificationProtocol
import exchange.stream.abacus.protocols.FileLocation
import exchange.stream.abacus.protocols.PresentationProtocol
import exchange.stream.abacus.protocols.StateNotificationProtocol
import exchange.stream.abacus.protocols.ThreadingType
import exchange.stream.abacus.protocols.TransactionCallback
import exchange.stream.abacus.protocols.readCachedTextFile
import exchange.stream.abacus.state.app.adaptors.V4TransactionErrors
import exchange.stream.abacus.state.app.helper.DynamicLocalizer
import exchange.stream.abacus.state.manager.ApiData
import exchange.stream.abacus.state.manager.AppSettings
import exchange.stream.abacus.state.manager.ConfigFile
import exchange.stream.abacus.state.manager.GasToken
import exchange.stream.abacus.state.manager.HistoricalPnlPeriod
import exchange.stream.abacus.state.manager.HumanReadableCancelOrderPayload
import exchange.stream.abacus.state.manager.HumanReadableDepositPayload
import exchange.stream.abacus.state.manager.HumanReadablePlaceOrderPayload
import exchange.stream.abacus.state.manager.HumanReadableSubaccountTransferPayload
import exchange.stream.abacus.state.manager.HumanReadableTriggerOrdersPayload
import exchange.stream.abacus.state.manager.HumanReadableWithdrawPayload
import exchange.stream.abacus.state.manager.OrderbookGrouping
import exchange.stream.abacus.state.manager.SingletonAsyncAbacusStateManagerProtocol
import exchange.stream.abacus.state.manager.TransferChainInfo
import exchange.stream.abacus.state.manager.V4Environment
import exchange.stream.abacus.state.manager.configs.V4StateManagerConfigs
import exchange.stream.abacus.state.model.AdjustIsolatedMarginInputField
import exchange.stream.abacus.state.model.ClosePositionInputField
import exchange.stream.abacus.state.model.TradeInputField
import exchange.stream.abacus.state.model.TransferInputField
import exchange.stream.abacus.state.model.TriggerOrdersInputField
import exchange.stream.abacus.state.v2.supervisor.AppConfigsV2
import exchange.stream.abacus.utils.CoroutineTimer
import exchange.stream.abacus.utils.DummyFormatter
import exchange.stream.abacus.utils.DummyLocalizer
import exchange.stream.abacus.utils.IList
import exchange.stream.abacus.utils.IOImplementations
import exchange.stream.abacus.utils.Logger
import exchange.stream.abacus.utils.Parser
import exchange.stream.abacus.utils.ProtocolNativeImpFactory
import exchange.stream.abacus.utils.Threading
import exchange.stream.abacus.utils.UIImplementations
import kollections.JsExport
import kollections.iListOf
import kollections.iMutableListOf
import me.tatarka.inject.annotations.Inject

@JsExport
@AbacusScope
@Inject
class AsyncAbacusStateManagerV2(
    val deploymentUri: DeploymentUri,
    val deployment: Deployment, // MAINNET, TESTNET, DEV
    val appConfigs: AppConfigsV2,
    val ioImplementations: IOImplementations,
    val uiImplementations: UIImplementations,
    val stateNotification: StateNotificationProtocol? = null,
    val dataNotification: DataNotificationProtocol? = null,
    private val presentationProtocol: PresentationProtocol? = null,
) : SingletonAsyncAbacusStateManagerProtocol {
    init {
        Logger.clientLogger = ioImplementations.logging
        if (appConfigs.enableLogger) {
            Logger.isDebugEnabled = true
        }
    }

    private val environmentsFile = ConfigFile.ENV

    private var _appSettings: AppSettings? = null

    override val appSettings: AppSettings?
        get() = _appSettings

    private var environments: IList<V4Environment> = iListOf()
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                _environment = findEnvironment(environmentId)
                ioImplementations.threading?.async(ThreadingType.main) {
                    stateNotification?.environmentsChanged()
                    dataNotification?.environmentsChanged()
                }
            }
        }

    override val availableEnvironments: IList<SelectionOption>
        get() = environments.map { environment ->
            SelectionOption(environment.id, environment.name, null, null)
        }

    override var environmentId: String? = null
        set(value) {
            if (field != value) {
                field = value
                ioImplementations.threading?.async(ThreadingType.abacus) {
                    _environment = findEnvironment(environmentId)
                }
            }
        }

    private var _environment: V4Environment? = null
        set(value) {
            if (field !== value) {
                field = value
                reconnect()
            }
        }

    override val environment: V4Environment?
        get() = _environment

    internal var adaptor: StateManagerAdaptorV2? = null
        private set(value) {
            if (field !== value) {
                field?.dispose()

                value?.market = market
                value?.accountAddress = accountAddress
                value?.sourceAddress = sourceAddress
                value?.subaccountNumber = subaccountNumber
                value?.orderbookGrouping = orderbookGrouping
                value?.historicalPnlPeriod = historicalPnlPeriod
                value?.candlesResolution = candlesResolution
                value?.readyToConnect = readyToConnect
                value?.cosmosWalletConnected = cosmosWalletConnected
                field = value
            }
        }

    override var readyToConnect: Boolean = false
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.readyToConnect = field
            }
        }

    override var market: String? = null
        set(value) {
            if (isMarketValid(value) && field != value) {
                field = value
                ioImplementations.threading?.async(ThreadingType.abacus) {
                    adaptor?.market = field
                }
            }
        }

    override var orderbookGrouping: OrderbookGrouping = OrderbookGrouping.none
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.orderbookGrouping = field
            }
        }

    override var candlesResolution: String = "1DAY"
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.candlesResolution = field
            }
        }

    override var accountAddress: String? = null
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.accountAddress = field
            }
        }

    override var cosmosWalletConnected: Boolean? = false
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.cosmosWalletConnected = field
            }
        }

    override var sourceAddress: String? = null
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.sourceAddress = field
            }
        }

    override var subaccountNumber: Int = 0
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.subaccountNumber = field
            }
        }

    override var historicalPnlPeriod: HistoricalPnlPeriod = HistoricalPnlPeriod.Period7d
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.historicalPnlPeriod = field
            }
        }

    override var gasToken: GasToken? = null
        set(value) {
            field = value
            ioImplementations.threading?.async(ThreadingType.abacus) {
                adaptor?.gasToken = field
            }
        }

    companion object {
        private fun createIOImplementions(_nativeImplementations: ProtocolNativeImpFactory): IOImplementations {
            return IOImplementations(
                rest = _nativeImplementations.rest,
                webSocket = _nativeImplementations.webSocket,
                chain = _nativeImplementations.chain,
                tracking = _nativeImplementations.tracking,
                threading = _nativeImplementations.threading ?: Threading(),
                timer = _nativeImplementations.timer ?: CoroutineTimer(),
                fileSystem = _nativeImplementations.fileSystem,
                logging = _nativeImplementations.logging,
            )
        }

        private fun createUIImplemention(_nativeImplementations: ProtocolNativeImpFactory): UIImplementations {
            return UIImplementations(
                localizer = _nativeImplementations.localizer ?: DummyLocalizer(),
                formatter = _nativeImplementations.formatter ?: DummyFormatter(),
            )
        }
    }

    init {
        if (ioImplementations.rest === null) {
            throw Error("IOImplementations.rest is not set")
        }
        if (ioImplementations.webSocket === null) {
            throw Error("IOImplementations.webSocket is not set")
        }
        if (ioImplementations.chain === null) {
            throw Error("IOImplementations.chain is not set")
        }
        if (ioImplementations.threading === null) {
            throw Error("IOImplementations.threading is not set")
        }
        if (ioImplementations.timer === null) {
            throw Error("IOImplementations.timer is not set")
        }
        if (uiImplementations.localizer === null) {
            throw Error("UIImplementations.localizer is not set")
        }
//        if (UIImplementations.formatter === null) {
//            throw Error("UIImplementations.formatter is not set")
//        }
        if (uiImplementations.localizer is DynamicLocalizer) {
            if (ioImplementations.fileSystem === null) {
                throw Error("IOImplementations.fileSystem is not set, used by Abacus localizer")
            }
        }
        if (stateNotification === null && dataNotification === null) {
            throw Error("Either stateNotification or dataNotification need to be set")
        }
        ConfigFile.values().forEach {
            load(it)
        }
    }

    private fun load(configFile: ConfigFile) {
        ioImplementations.threading?.async(ThreadingType.network) {
            val path = configFile.path
            if (appConfigs.loadRemote) {
                loadFromRemoteConfigFile(configFile)
                val configFileUrl = "$deploymentUri$path"
                ioImplementations.rest?.get(
                    configFileUrl,
                    null,
                    callback = { response, httpCode, headers ->
                        ioImplementations.threading?.async(ThreadingType.abacus) {
                            if (success(httpCode) && response != null) {
                                if (parse(response, configFile)) {
                                    writeToLocalFile(response, path)
                                }
                            }
                        }
                    },
                )
            } else {
                loadFromBundledLocalConfigFile(configFile)
            }
        }
    }

    private fun loadFromRemoteConfigFile(configFile: ConfigFile) {
        ioImplementations.fileSystem?.readCachedTextFile(
            configFile.path,
        )?.let {
            ioImplementations.threading?.async(ThreadingType.abacus) {
                parse(it, configFile)
            }
        }
    }

    private fun loadFromBundledLocalConfigFile(configFile: ConfigFile) {
        ioImplementations.fileSystem?.readTextFile(
            FileLocation.AppBundle,
            configFile.path,
        )?.let {
            ioImplementations.threading?.async(ThreadingType.abacus) {
                parse(it, configFile)
            }
        }
    }

    private fun parse(response: String, configFile: ConfigFile): Boolean {
        return when (configFile) {
            ConfigFile.ENV -> parseEnvironments(response)
        }
    }

    private fun success(httpCode: Int): Boolean {
        return httpCode in 200..299
    }

    private fun writeToLocalFile(response: String, file: String) {
        ioImplementations.threading?.async(ThreadingType.network) {
            ioImplementations.fileSystem?.writeTextFile(
                file,
                response,
            )
        }
    }

    private fun parseEnvironments(response: String): Boolean {
        val parser = Parser()
        val items = parser.decodeJsonObject(response)
        val deployments = parser.asMap(items?.get("deployments")) ?: return false
        val target = parser.asMap(deployments[deployment]) ?: return false
        val targetEnvironments = parser.asList(target["environments"]) ?: return false
        val targetDefault = parser.asString(target["default"])

        val tokensData = parser.asNativeMap(items?.get("tokens"))
        val linksData = parser.asNativeMap(items?.get("links"))
        val walletsData = parser.asNativeMap(items?.get("wallets"))
        val governanceData = parser.asNativeMap(items?.get("governance"))

        if (items != null) {
            val environmentsData = parser.asMap(items["environments"]) ?: return false
            val parsedEnvironments = mutableMapOf<String, V4Environment>()
            for ((key, value) in environmentsData) {
                val data = parser.asMap(value) ?: continue
                val streamChainId = parser.asString(data["streamChainId"]) ?: continue
                val environment = V4Environment.parse(
                    key,
                    data,
                    parser,
                    deploymentUri,
                    uiImplementations.localizer,
                    parser.asNativeMap(tokensData?.get(streamChainId)),
                    parser.asNativeMap(linksData?.get(streamChainId)),
                    parser.asNativeMap(walletsData?.get(streamChainId)),
                    parser.asNativeMap(governanceData?.get(streamChainId)),
                ) ?: continue
                parsedEnvironments[environment.id] = environment
            }
            if (parsedEnvironments.isEmpty()) {
                return false
            }
            val environments = iMutableListOf<V4Environment>()
            for (environmentId in targetEnvironments) {
                val environment = parsedEnvironments[parser.asString(environmentId)!!]
                if (environment != null) {
                    environments.add(environment)
                }
            }

            this.environments = environments
            if (targetDefault != null && this.environmentId == null) {
                this.environmentId = targetDefault
            }

            val apps = parser.asMap(items["apps"])
            if (apps != null) {
                this._appSettings = AppSettings.parse(apps, parser)
            }

            return true
        } else {
            return false
        }
    }

    private fun findEnvironment(environment: String?): V4Environment? {
        return environments.firstOrNull { it ->
            it.id == environment
        }
    }

    private fun reconnect() {
        val environment = environment
        if (environment != null) {
            adaptor = StateManagerAdaptorV2(
                deploymentUri,
                environment,
                ioImplementations,
                uiImplementations,
                V4StateManagerConfigs(deploymentUri, environment),
                appConfigs,
                stateNotification,
                dataNotification,
                presentationProtocol,
            )
        }
    }

    override fun trade(data: String?, type: TradeInputField?) {
        adaptor?.trade(data, type)
    }

    override fun closePosition(data: String?, type: ClosePositionInputField) {
        adaptor?.closePosition(data, type)
    }

    override fun transfer(data: String?, type: TransferInputField?) {
        adaptor?.transfer(data, type)
    }

    override fun triggerOrders(data: String?, type: TriggerOrdersInputField?) {
        adaptor?.triggerOrders(data, type)
    }

    override fun adjustIsolatedMargin(data: String?, type: AdjustIsolatedMarginInputField?) {
        adaptor?.adjustIsolatedMargin(data, type)
    }

    override fun isMarketValid(marketId: String?): Boolean {
        return if (marketId == null) {
            true
        } else {
            val market = adaptor?.stateMachine?.state?.market(marketId)
            (market?.status?.canTrade == true || market?.status?.canReduce == true)
        }
    }

    override fun transferStatus(
        hash: String,
        fromChainId: String?,
        toChainId: String?,
        isCctp: Boolean,
        requestId: String?
    ) {
        adaptor?.transferStatus(hash, fromChainId, toChainId, isCctp, requestId)
    }

    override fun refresh(data: ApiData) {
        adaptor?.refresh(data)
    }

    override fun placeOrderPayload(): HumanReadablePlaceOrderPayload? {
        return adaptor?.placeOrderPayload()
    }

    override fun closePositionPayload(): HumanReadablePlaceOrderPayload? {
        return adaptor?.closePositionPayload()
    }

    override fun cancelOrderPayload(orderId: String): HumanReadableCancelOrderPayload? {
        return adaptor?.cancelOrderPayload(orderId)
    }

    override fun triggerOrdersPayload(): HumanReadableTriggerOrdersPayload? {
        return adaptor?.triggerOrdersPayload()
    }

    override fun adjustIsolatedMarginPayload(): HumanReadableSubaccountTransferPayload? {
        return adaptor?.adjustIsolatedMarginPayload()
    }

    override fun depositPayload(): HumanReadableDepositPayload? {
        return adaptor?.depositPayload()
    }

    override fun withdrawPayload(): HumanReadableWithdrawPayload? {
        return adaptor?.withdrawPayload()
    }

    override fun subaccountTransferPayload(): HumanReadableSubaccountTransferPayload? {
        return adaptor?.subaccountTransferPayload()
    }

    override fun commitPlaceOrder(callback: TransactionCallback): HumanReadablePlaceOrderPayload? {
        return try {
            adaptor?.commitPlaceOrder(callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
            null
        }
    }

    override fun commitTriggerOrders(callback: TransactionCallback): HumanReadableTriggerOrdersPayload? {
        return try {
            adaptor?.commitTriggerOrders(callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
            null
        }
    }

    override fun commitAdjustIsolatedMargin(callback: TransactionCallback): HumanReadableSubaccountTransferPayload? {
        return try {
            adaptor?.commitAdjustIsolatedMargin(callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
            null
        }
    }

    override fun commitClosePosition(callback: TransactionCallback): HumanReadablePlaceOrderPayload? {
        return try {
            adaptor?.commitClosePosition(callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
            null
        }
    }

    override fun stopWatchingLastOrder() {
        adaptor?.stopWatchingLastOrder()
    }

    override fun commitTransfer(callback: TransactionCallback) {
        try {
            adaptor?.commitTransfer(callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
        }
    }

    override fun commitCCTPWithdraw(callback: TransactionCallback) {
        try {
            adaptor?.commitCCTPWithdraw(callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
        }
    }

    override fun faucet(amount: Double, callback: TransactionCallback) {
        try {
            adaptor?.faucet(amount, callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
        }
    }

    override fun cancelOrder(orderId: String, callback: TransactionCallback) {
        try {
            adaptor?.cancelOrder(orderId, callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
        }
    }

    override fun triggerCompliance(action: ComplianceAction, callback: TransactionCallback) {
        try {
            adaptor?.triggerCompliance(action, callback)
        } catch (e: Exception) {
            val error = V4TransactionErrors.error(null, e.toString())
            callback(false, error, null)
        }
    }

    // Bridge functions.
    // If client is not using cancelOrder function, it should call orderCanceled function with
    // payload from v4-client to process state
    override fun orderCanceled(orderId: String) {
        adaptor?.orderCanceled(orderId)
    }

    override fun screen(address: String, callback: (restriction: Restriction) -> Unit) {
        adaptor?.screen(address, callback)
    }

    override fun getChainById(chainId: String): TransferChainInfo? {
        val parser = Parser()
        val chainMap = adaptor?.stateMachine?.routerProcessor?.getChainById(chainId = chainId)
        val chainName = parser.asString(chainMap?.get("chain_name"))
        val logoUri = parser.asString(chainMap?.get("logo_uri"))
        val chainType = parser.asString(chainMap?.get("chain_type"))
        val isTestnet = parser.asBool(chainMap?.get("is_testnet"))
        if (chainName is String && logoUri is String && chainType is String && isTestnet is Boolean) {
            return TransferChainInfo(
                chainName = chainName,
                chainId = chainType,
                logoUri = logoUri,
                chainType = chainType,
                isTestnet = isTestnet,
            )
        }
        return null
    }
}
