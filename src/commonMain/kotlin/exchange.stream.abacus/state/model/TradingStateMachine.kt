package exchange.stream.abacus.state.model

import exchange.stream.abacus.calculator.AccountCalculator
import exchange.stream.abacus.calculator.AdjustIsolatedMarginInputCalculator
import exchange.stream.abacus.calculator.CalculationPeriod
import exchange.stream.abacus.calculator.MarketCalculator
import exchange.stream.abacus.calculator.TradeCalculation
import exchange.stream.abacus.calculator.TradeInputCalculator
import exchange.stream.abacus.calculator.TransferInputCalculator
import exchange.stream.abacus.calculator.TriggerOrdersInputCalculator
import exchange.stream.abacus.calculator.v2.AccountCalculatorV2
import exchange.stream.abacus.output.Asset
import exchange.stream.abacus.output.Configs
import exchange.stream.abacus.output.MarketCandles
import exchange.stream.abacus.output.MarketHistoricalFunding
import exchange.stream.abacus.output.MarketOrderbook
import exchange.stream.abacus.output.MarketTrade
import exchange.stream.abacus.output.PerpetualMarketSummary
import exchange.stream.abacus.output.PerpetualState
import exchange.stream.abacus.output.TransferStatus
import exchange.stream.abacus.output.Wallet
import exchange.stream.abacus.output.account.Account
import exchange.stream.abacus.output.account.Subaccount
import exchange.stream.abacus.output.account.SubaccountFill
import exchange.stream.abacus.output.account.SubaccountFundingPayment
import exchange.stream.abacus.output.account.SubaccountHistoricalPNL
import exchange.stream.abacus.output.account.SubaccountTransfer
import exchange.stream.abacus.output.input.Input
import exchange.stream.abacus.output.input.ReceiptLine
import exchange.stream.abacus.processor.assets.AssetsProcessor
import exchange.stream.abacus.processor.configs.ConfigsProcessor
import exchange.stream.abacus.processor.markets.MarketsSummaryProcessor
import exchange.stream.abacus.processor.router.IRouterProcessor
import exchange.stream.abacus.processor.router.skip.SkipProcessor
import exchange.stream.abacus.processor.router.squid.SquidProcessor
import exchange.stream.abacus.processor.wallet.WalletProcessor
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.protocols.asTypedStringMap
import exchange.stream.abacus.responses.ParsingError
import exchange.stream.abacus.responses.ParsingErrorType
import exchange.stream.abacus.responses.ParsingException
import exchange.stream.abacus.responses.SocketInfo
import exchange.stream.abacus.responses.StateResponse
import exchange.stream.abacus.state.app.adaptors.AbUrl
import exchange.stream.abacus.state.app.helper.Formatter
import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.state.internalstate.InternalAccountState
import exchange.stream.abacus.state.internalstate.InternalState
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.state.manager.EnvironmentFeatureFlags
import exchange.stream.abacus.state.manager.StatsigConfig
import exchange.stream.abacus.state.manager.TokenInfo
import exchange.stream.abacus.state.manager.V4Environment
import exchange.stream.abacus.utils.IList
import exchange.stream.abacus.utils.Logger
import exchange.stream.abacus.utils.NUM_PARENT_SUBACCOUNTS
import exchange.stream.abacus.utils.Parser
import exchange.stream.abacus.utils.ServerTime
import exchange.stream.abacus.utils.iMapOf
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.mutableMapOf
import exchange.stream.abacus.utils.safeSet
import exchange.stream.abacus.utils.typedSafeSet
import exchange.stream.abacus.validator.InputValidator
import indexer.models.configs.AssetJson
import kollections.JsExport
import kollections.iListOf
import kollections.iMutableListOf
import kollections.iMutableMapOf
import kollections.toIList
import kollections.toIMutableMap
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.days

@Suppress("UNCHECKED_CAST")
@JsExport
open class TradingStateMachine(
    private val environment: V4Environment?,
    private val localizer: LocalizerProtocol?,
    private val formatter: Formatter?,
    private val maxSubaccountNumber: Int,
    private val useParentSubaccount: Boolean,
    val staticTyping: Boolean = false,
) {
    internal var internalState: InternalState = InternalState()

    internal val parser: ParserProtocol = Parser()
    internal val marketsProcessor = MarketsSummaryProcessor(parser)
    internal val assetsProcessor = run {
        val processor = AssetsProcessor(
            parser = parser,
            localizer = localizer,
        )
        processor.environment = environment
        processor
    }
    internal val walletProcessor = WalletProcessor(parser, localizer)
    internal val configsProcessor = ConfigsProcessor(parser)
    private val skipProcessor = SkipProcessor(parser = parser, internalState = internalState.transfer)
    private val squidProcessor = SquidProcessor(parser = parser, internalState = internalState.transfer)
    internal val routerProcessor: IRouterProcessor
        get() {
            if (StatsigConfig.useSkip) return skipProcessor
            return squidProcessor
        }

    internal val marketsCalculator = MarketCalculator(parser)
    internal val accountCalculator = AccountCalculator(parser, useParentSubaccount)
    internal val accountCalculatorV2 = AccountCalculatorV2(parser, useParentSubaccount)

    internal val inputValidator = InputValidator(localizer, formatter, parser)

    internal var data: Map<String, Any>? = null

    private var dummySubaccountPNLs = mutableMapOf<String, SubaccountHistoricalPNL>();

    internal val tokensInfo: Map<String, TokenInfo>
        get() = environment?.tokens!!

    internal val featureFlags: EnvironmentFeatureFlags
        get() = environment?.featureFlags!!

    internal var currentBlockAndHeight: BlockAndTime? = null

    internal var groupingMultiplier: Int
        get() = marketsProcessor.groupingMultiplier
        set(value) {
            marketsProcessor.groupingMultiplier = value
        }

    internal var marketsSummary: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("markets"))
        }
        set(value) {
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("markets", value)
            this.data = if (modified.size != 0) modified else null
        }

    internal var historicalPnlDays: Int = 1

    internal var assets: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("assets"))
        }
        set(value) {
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("assets", value)
            this.data = if (modified.size != 0) modified else null
        }

    internal var wallet: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("wallet"))
        }
        set(value) {
            val oldAddress = parser.asString(parser.value(wallet, "walletAddress"))
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("wallet", value)
            this.data = if (modified.isEmpty()) null else modified
            val address = parser.asString(parser.value(wallet, "walletAddress"))
            if (address != oldAddress) {
                dummySubaccountPNLs = mutableMapOf()
            }
        }

    internal var account: Map<String, Any>?
        get() {
            return parser.asNativeMap(wallet?.get("account"))
        }
        set(value) {
            val modified = wallet?.mutable() ?: mutableMapOf()
            modified.safeSet("account", value)
            this.wallet = if (modified.size != 0) modified else null
        }

    internal var user: Map<String, Any>?
        get() {
            return parser.asNativeMap(wallet?.get("user"))
        }
        set(value) {
            val modified = wallet?.mutable() ?: mutableMapOf()
            modified.safeSet("user", value)
            this.wallet = if (modified.size != 0) modified else null
        }

    internal var configs: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("configs"))
        }
        set(value) {
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("configs", value)
            this.data = if (modified.size != 0) modified else null
        }

    internal var input: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("input"))
        }
        set(value) {
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("input", value)
            this.data = if (modified.size != 0) modified else null
        }

    internal var transferStatuses: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("transferStatuses"))
        }
        set(value) {
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("transferStatuses", value)
            this.data = if (modified.size != 0) modified else null
        }

    internal var trackStatuses: Map<String, Any>?
        get() {
            return parser.asNativeMap(data?.get("trackStatuses"))
        }
        set(value) {
            val modified = data?.mutable() ?: mutableMapOf()
            modified.safeSet("trackStatuses", value)
            this.data = if (modified.size != 0) modified else null
        }

    var state: PerpetualState? = null

    private fun noChange(): StateResponse {
        return StateResponse(state, null)
    }

    fun socket(
        url: AbUrl,
        jsonString: String,
        subaccountNumber: Int,
        height: BlockAndTime?,
    ): StateResponse {
        val errors = iMutableListOf<ParsingError>()
        val json =
            try {
                Json.parseToJsonElement(jsonString).jsonObject.toMap()
            } catch (e: SerializationException) {
                errors.add(
                    ParsingError(
                        ParsingErrorType.ParsingError,
                        "$jsonString is not a valid JSON object",
                        e.stackTraceToString(),
                    ),
                )
                null
            }
        if (json == null || errors.isNotEmpty()) {
            return StateResponse(state, null, errors)
        }
        return socket(url, json, subaccountNumber, height)
    }

    @Throws(Exception::class)
    private fun socket(
        url: AbUrl,
        payload: Map<String, Any>,
        subaccountNumber: Int,
        height: BlockAndTime?,
    ): StateResponse {
        var changes: StateChanges? = null
        val type = parser.asString(payload["type"])
        val channel = parser.asString(payload["channel"])
        val id = parser.asString(payload["id"])
        val childSubaccountNumber = parser.asInt(payload["subaccountNumber"])
        val info = SocketInfo(type, channel, id, childSubaccountNumber)
        try {
            when (type) {
                "subscribed" -> {
                    val content = parser.asNativeMap(payload["contents"])
                        ?: throw ParsingException(
                            ParsingErrorType.MissingContent,
                            payload.toString(),
                        )
                    when (channel) {
                        "v3_markets", "v4_markets" -> {
                            changes = receivedMarkets(content, subaccountNumber)
                        }

                        "v4_subaccounts", "v4_parent_subaccounts" -> {
                            changes = receivedSubaccountSubscribed(content, height)
                        }

                        "v3_orderbook", "v4_orderbook" -> {
                            val market = parser.asString(payload["id"])
                            changes = receivedOrderbook(market, content, subaccountNumber)
                        }

                        "v3_trades", "v4_trades" -> {
                            val market = parser.asString(payload["id"])
                            changes = receivedTrades(market, content)
                        }

                        "v4_candles" -> {
                            val channel = parser.asString(payload["id"])
                            val (market, resolution) = splitCandlesChannel(channel)
                            changes = receivedCandles(market, resolution, content)
                        }

                        else -> {
                            throw ParsingException(
                                ParsingErrorType.UnknownChannel,
                                "$channel subscribed is not known",
                            )
                        }
                    }
                }

                "unsubscribed" -> {}

                "channel_data" -> {
                    val content = parser.asNativeMap(payload["contents"])
                        ?: throw ParsingException(
                            ParsingErrorType.MissingContent,
                            payload.toString(),
                        )
                    when (channel) {
                        "v3_markets", "v4_markets" -> {
                            changes = receivedMarketsChanges(content, subaccountNumber)
                        }

                        "v4_subaccounts", "v4_parent_subaccounts" -> {
                            changes = receivedSubaccountsChanges(content, info, height)
                        }

                        "v3_orderbook", "v4_orderbook" -> {
                            throw ParsingException(
                                ParsingErrorType.UnhandledEndpoint,
                                "channel_data for $channel is not implemented",
                            )
                            //                                    change = receivedOrderbookChanges(market, it)
                        }

                        "v3_trades", "v4_trades" -> {
                            val market = parser.asString(payload["id"])
                            changes = receivedTradesChanges(market, content)
                        }

                        "v4_candles" -> {
                            val channel = parser.asString(payload["id"])
                            val (market, resolution) = splitCandlesChannel(channel)
                            changes = receivedCandlesChanges(market, resolution, content)
                        }

                        else -> {
                            throw ParsingException(
                                ParsingErrorType.UnknownChannel,
                                "$channel channel data is not known",
                            )
                        }
                    }
                }

                "channel_batch_data" -> {
                    val content = parser.asList(payload["contents"])
                        ?: throw ParsingException(
                            ParsingErrorType.MissingContent,
                            payload.toString(),
                        )
                    when (channel) {
                        "v3_markets", "v4_markets" -> {
                            changes = receivedBatchedMarketsChanges(content, subaccountNumber)
                        }

                        "v3_trades", "v4_trades" -> {
                            val market = parser.asString(payload["id"])
                            changes = receivedBatchedTradesChanges(market, content)
                        }

                        "v4_candles" -> {
                            val channel = parser.asString(payload["id"])
                            val (market, resolution) = splitCandlesChannel(channel)
                            changes = receivedBatchedCandlesChanges(market, resolution, content)
                        }

                        "v3_orderbook", "v4_orderbook" -> {
                            val market = parser.asString(payload["id"])
                            changes = receivedBatchOrderbookChanges(
                                market,
                                content,
                                subaccountNumber,
                            )
                        }

                        "v4_subaccounts", "v4_parent_subaccounts" -> {
                            changes = receivedBatchSubaccountsChanges(content, info, height)
                        }

                        else -> {
                            throw ParsingException(
                                ParsingErrorType.UnknownChannel,
                                "$channel channel batch data is not known",
                            )
                        }
                    }
                }

                "connected" -> {}

                "error" -> {
                    throw ParsingException(ParsingErrorType.BackendError, payload.toString())
                }

                else -> {
                    throw ParsingException(
                        ParsingErrorType.Unhandled,
                        "Type [ $type # $channel ] is not handled",
                    )
                }
            }
            var realChanges = changes
            changes?.let {
                realChanges = update(it)
            }
            return StateResponse(state, realChanges, null, info)
        } catch (e: ParsingException) {
            return StateResponse(state, null, iListOf(e.toParsingError()), info)
        }
    }

    private fun splitCandlesChannel(channel: String?): Pair<String, String> {
        if (channel == null) {
            throw ParsingException(
                ParsingErrorType.UnknownChannel,
                "$channel is not known",
            )
        }
        val marketAndResolution = channel.split("/")
        if (marketAndResolution.size != 2) {
            throw ParsingException(
                ParsingErrorType.UnknownChannel,
                "$channel is not known",
            )
        }
        val market = marketAndResolution[0]
        val resolution = marketAndResolution[1]
        return Pair(market, resolution)
    }

    /**
     * function specifically for testing spoofed rest response processing
     */
    fun rest(
        url: AbUrl,
        payload: String,
        subaccountNumber: Int,
        height: Int?,
        deploymentUri: String? = null,
        period: String? = null,
    ): StateResponse {
        /*
        For backward compatibility only
         */
        var changes: StateChanges? = null
        var error: ParsingError? = null
        when (url.path) {
            "/v3/historical-pnl", "/v4/historical-pnl" -> {
                val subaccountNumber =
                    parser.asInt(url.params?.firstOrNull { param -> param.key == "subaccountNumber" }?.value)
                        ?: 0
                changes = historicalPnl(payload, subaccountNumber)
            }

            "/v3/candles" -> {
                changes = candles(payload)
            }

            "/v4/sparklines" -> {
                changes = sparklines(payload)
            }

            "/v4/fills" -> {
                val subaccountNumber =
                    parser.asInt(url.params?.firstOrNull { param -> param.key == "subaccountNumber" }?.value)
                        ?: 0
                changes = fills(payload, subaccountNumber)
            }

            "/v4/transfers" -> {
                val subaccountNumber =
                    parser.asInt(url.params?.firstOrNull { param -> param.key == "subaccountNumber" }?.value)
                        ?: 0
                changes = transfers(payload, subaccountNumber)
            }

            "/configs/markets.json" -> {
                if (deploymentUri != null) {
                    changes = configurations(
                        payload = payload,
                        subaccountNumber = subaccountNumber,
                        deploymentUri = deploymentUri,
                    )
                }
            }

            else -> {
                if (url.path.contains("/v3/historical-funding/") || url.path.contains("/v4/historicalFunding/")) {
                    changes = historicalFundings(payload)
                } else if (url.path.contains("/v3/candles/") || url.path.contains("/v4/candles/")) {
                    changes = candles(payload)
                } else if (url.path.contains("/v4/addresses/")) {
                    changes = account(payload)
                } else {
                    error = ParsingError(
                        ParsingErrorType.UnhandledEndpoint,
                        "${url.path} parsing has not be implemented, or is an invalid endpoint",
                    )
                }
            }
        }
        if (changes != null) {
            update(changes)
        }

        val errors = if (error != null) iListOf(error) else null
        return StateResponse(state, changes, errors)
    }

    internal fun resetWallet(accountAddress: String?): StateResponse {
        val wallet = if (accountAddress != null) iMapOf("walletAddress" to accountAddress) else null
        this.wallet = wallet
        if (accountAddress != internalState.wallet.walletAddress) {
            internalState.wallet.walletAddress = accountAddress
            internalState.wallet.account = InternalAccountState()
        }
        if (accountAddress == null) {
            this.account = null
        }
        val changes = StateChanges(
            iListOf(
                Changes.wallet,
                Changes.subaccount,
                Changes.historicalPnl,
                Changes.fills,
                Changes.transfers,
                Changes.fundingPayments,
            ),
        )
        update(changes)
        walletProcessor.accountAddress = accountAddress
        return StateResponse(state, changes, null)
    }

    internal fun configurations(
        payload: String,
        subaccountNumber: Int?,
        deploymentUri: String
    ): StateChanges {
        val json = parser.decodeJsonObject(payload)
        if (staticTyping) {
            val parsedAssetPayload = parser.asTypedStringMap<AssetJson>(json)
            if (parsedAssetPayload == null) {
                Logger.e { "Error parsing asset payload" }
                return StateChanges.noChange
            }

            return processMarketsConfigurations(
                payload = parsedAssetPayload,
                subaccountNumber = subaccountNumber,
                deploymentUri = deploymentUri,
            )
        } else {
            return if (json != null) {
                receivedMarketsConfigurationsDeprecated(json, subaccountNumber, deploymentUri)
            } else {
                StateChanges.noChange
            }
        }
    }

    internal fun update(changes: StateChanges): StateChanges {
        if (changes.changes.contains(Changes.input)) {
            val subaccountNumber = changes.subaccountNumbers?.firstOrNull()

            val subaccount = if (subaccountNumber != null) {
                parser.asNativeMap(
                    parser.value(
                        this.account,
                        "subaccounts.$subaccountNumber",
                    ),
                )
            } else {
                null
            }

            this.input = inputValidator.validate(
                subaccountNumber,
                this.wallet,
                this.user,
                subaccount,
                parser.asNativeMap(this.marketsSummary?.get("markets")),
                this.input,
                this.configs,
                this.currentBlockAndHeight,
                this.environment,
            )

            if (subaccountNumber != null) {
                when (this.input?.get("current")) {
                    "trade" -> {
                        calculateTrade(subaccountNumber)
                    }

                    "closePosition" -> {
                        calculateClosePosition(subaccountNumber)
                    }

                    "transfer" -> {
                        calculateTransfer(subaccountNumber)
                    }

                    "triggerOrders" -> {
                        calculateTriggerOrders(subaccountNumber)
                    }

                    "adjustIsolatedMargin" -> {
                        calculateAdjustIsolatedMargin(subaccountNumber)
                    }

                    else -> {}
                }
            }
        }
        recalculateStates(changes)

        val wallet = state?.wallet
        val input = state?.input

        state = update(state, changes, tokensInfo, localizer)

        val realChanges = iMutableListOf<Changes>()
        for (change in changes.changes) {
            val didChange = when (change) {
                Changes.assets,
                Changes.markets,
                Changes.candles,
                Changes.sparklines,
                Changes.historicalFundings,
                Changes.accountBalances,
                Changes.subaccount,
                Changes.historicalPnl,
                Changes.fills,
                Changes.transfers,
                Changes.fundingPayments,
                Changes.trades,
                Changes.configs,
                Changes.transferStatuses,
                Changes.trackStatuses,
                Changes.orderbook,
                -> true

                Changes.wallet -> state?.wallet != wallet
                Changes.input -> state?.input != input

                // Restriction is handled separately and shouldn't have gone through here
                Changes.restriction -> {
                    Logger.d { "Restriction is handled separately and shouldn't have gone through here" }
                    false
                }
                Changes.compliance -> {
                    Logger.d { "Compliance is handled separately and shouldn't have gone through here" }
                    false
                }
            }
            if (didChange) {
                realChanges.add(change)
            }
        }
        return StateChanges(realChanges, changes.markets, changes.subaccountNumbers)
    }

    private fun calculateTrade(subaccountNumber: Int) {
        calculateTrade("trade", TradeCalculation.trade, subaccountNumber)
    }

    private fun calculateTrade(tag: String, calculation: TradeCalculation, subaccountNumber: Int) {
        val input = this.input?.mutable()
        val trade = parser.asNativeMap(input?.get(tag))
        val inputType = parser.asString(parser.value(trade, "size.input"))
        val calculator = TradeInputCalculator(parser, calculation)
        val params = mutableMapOf<String, Any>()
        params.safeSet("markets", parser.asNativeMap(marketsSummary?.get("markets")))
        params.safeSet("account", account)
        params.safeSet("user", user)
        params.safeSet("trade", trade)
        params.safeSet("configs", configs)

        val modified = calculator.calculate(params, subaccountNumber, inputType)
        this.setMarkets(parser.asNativeMap(modified["markets"]))
        this.account = parser.asNativeMap(modified["account"])
        input?.safeSet(tag, parser.asNativeMap(modified["trade"]))

        this.input = input
    }

    private fun calculateClosePosition(subaccountNumber: Int) {
        calculateTrade("closePosition", TradeCalculation.closePosition, subaccountNumber)
    }

    private fun calculateTransfer(subaccountNumber: Int?) {
        val input = this.input?.mutable()
        val transfer = parser.asNativeMap(input?.get("transfer"))
        val calculator = TransferInputCalculator(parser)
        val params = mutableMapOf<String, Any>()
        params.safeSet("markets", parser.asNativeMap(marketsSummary?.get("markets")))
        params.safeSet("user", user)
        params.safeSet("transfer", transfer)
        params.safeSet("wallet", wallet)

        val modified = calculator.calculate(params, subaccountNumber)
        this.setMarkets(parser.asNativeMap(modified["markets"]))
        this.wallet = parser.asNativeMap(modified["wallet"])
        input?.safeSet("transfer", parser.asNativeMap(modified["transfer"]))

        this.input = input
    }

    private fun calculateTriggerOrders(subaccountNumber: Int?) {
        val input = this.input?.mutable()
        val triggerOrders = parser.asNativeMap(input?.get("triggerOrders"))
        val calculator = TriggerOrdersInputCalculator(parser)
        val params = mutableMapOf<String, Any>()
        params.safeSet("account", account)
        params.safeSet("user", user)
        params.safeSet("markets", parser.asNativeMap(marketsSummary?.get("markets")))
        params.safeSet("triggerOrders", triggerOrders)

        val modified = calculator.calculate(params, subaccountNumber)
        input?.safeSet("triggerOrders", parser.asNativeMap(modified["triggerOrders"]))

        this.input = input
    }

    private fun calculateAdjustIsolatedMargin(subaccountNumber: Int?) {
        val input = this.input?.mutable()
        val adjustIsolatedMargin = parser.asNativeMap(input?.get("adjustIsolatedMargin"))
        val calculator = AdjustIsolatedMarginInputCalculator(parser)
        val params = mutableMapOf<String, Any>()
        params.safeSet("wallet", wallet)
        params.safeSet("account", account)
        params.safeSet("user", user)
        params.safeSet("markets", parser.asNativeMap(marketsSummary?.get("markets")))
        params.safeSet("adjustIsolatedMargin", adjustIsolatedMargin)

        val modified = calculator.calculate(params, subaccountNumber)
        this.setMarkets(parser.asNativeMap(modified["markets"]))
        this.wallet = parser.asNativeMap(modified["wallet"])
        input?.safeSet("adjustIsolatedMargin", parser.asNativeMap(modified["adjustIsolatedMargin"]))

        this.input = input
    }

    private fun subaccount(subaccountNumber: Int): Map<String, Any>? {
        return parser.asNativeMap(parser.value(account, "subaccounts.$subaccountNumber"))
    }

    private fun subaccountList(subaccountNumber: Int, name: String): IList<Any>? {
        return parser.asList(subaccount(subaccountNumber)?.get(name))
    }

    private fun groupedSubaccount(subaccountNumber: Int): Map<String, Any>? {
        return parser.asNativeMap(parser.value(account, "groupedSubaccounts.$subaccountNumber"))
    }

    private fun groupedSubaccountList(subaccountNumber: Int, name: String): IList<Any>? {
        return parser.asList(groupedSubaccount(subaccountNumber)?.get(name))
    }

    private fun subaccountHistoricalPnl(subaccountNumber: Int): IList<Any>? {
        return subaccountList(subaccountNumber, "historicalPnl")
    }

    private fun subaccountFills(subaccountNumber: Int): IList<Any>? {
        return subaccountList(subaccountNumber, "fills")
    }

    private fun subaccountTransfers(subaccountNumber: Int): IList<Any>? {
        return subaccountList(subaccountNumber, "transfers")
    }

    private fun subaccountFundingPayments(subaccountNumber: Int): IList<Any>? {
        return subaccountList(subaccountNumber, "fundingPayments")
    }

    private fun groupedSubaccountHistoricalPnl(subaccountNumber: Int): IList<Any>? {
        return groupedSubaccountList(subaccountNumber, "historicalPnl")
    }

    private fun groupedSubaccountFills(subaccountNumber: Int): IList<Any>? {
        return groupedSubaccountList(subaccountNumber, "fills")
    }

    private fun groupedSubaccountTransfers(subaccountNumber: Int): IList<Any>? {
        return groupedSubaccountList(subaccountNumber, "transfers")
    }

    private fun groupedSubaccountFundingPayments(subaccountNumber: Int): IList<Any>? {
        return groupedSubaccountList(subaccountNumber, "fundingPayments")
    }

    private fun allSubaccountNumbers(): IList<Int> {
        val subaccountsData = parser.asNativeMap(account?.get("subaccounts"))
        return if (subaccountsData != null) {
            parser.asNativeMap(subaccountsData)?.keys?.mapNotNull { key ->
                parser.asInt(key)
            }?.toIList() ?: iListOf<Int>()
        } else {
            iListOf<Int>()
        }
    }

    private fun maxSubaccountNumber(): Int? {
        var maxSubaccountNumber: Int? = null
        val subaccountsData = parser.asNativeMap(account?.get("subaccounts"))
        if (subaccountsData != null) {
            for ((key, value) in subaccountsData) {
                val subaccountNumber = parser.asInt(key)
                if (subaccountNumber != null) {
                    if (maxSubaccountNumber != null) {
                        maxSubaccountNumber = max(maxSubaccountNumber, subaccountNumber)
                    } else {
                        maxSubaccountNumber = subaccountNumber
                    }
                }
            }
        }
        return maxSubaccountNumber
    }

    private fun subaccountNumbersWithPlaceholders(maxSubaccountNumber: Int?): IList<Int> {
        return if (maxSubaccountNumber != null) {
            val subaccountNumbers = iMutableListOf<Int>()
            for (i in 0 until min(maxSubaccountNumber, this.maxSubaccountNumber) + 1) {
                subaccountNumbers.add(i)
            }
            subaccountNumbers
        } else {
            iListOf(0)
        }
    }

    private fun recalculateStates(changes: StateChanges) {
        val subaccountNumbers = changes.subaccountNumbers ?: allSubaccountNumbers()
        if (changes.changes.contains(Changes.subaccount)) {
            val periods = if (this.input != null) {
                setOf(
                    CalculationPeriod.current,
                    CalculationPeriod.post,
                    CalculationPeriod.settled,
                )
            } else {
                setOf(CalculationPeriod.current)
            }

            this.marketsSummary?.let { marketsSummary ->
                parser.asNativeMap(marketsSummary["markets"])?.let { markets ->
                    val modifiedAccount = accountCalculator.calculate(
                        account = account,
                        subaccountNumbers = subaccountNumbers,
                        configs = null,
                        markets = markets,
                        price = priceOverwrite(markets),
                        periods = periods,
                    )
                    this.account = modifiedAccount
                }
                if (staticTyping) {
                    internalState.wallet.account = accountCalculatorV2.calculate(
                        account = internalState.wallet.account,
                        subaccountNumbers = subaccountNumbers,
                    )
                }
            }
        }
        if (parser.value(account, "groupedSubaccounts") != null) {
            if (changes.changes.contains(Changes.fills)) {
                this.account = mergeFills(this.account, subaccountNumbers)
            }
            if (changes.changes.contains(Changes.transfers)) {
                this.account = mergeTransfers(this.account, subaccountNumbers)
            }
        }
        if (changes.changes.contains(Changes.input)) {
            val modified = this.input?.mutable() ?: return
            when (parser.asString(modified["current"])) {
                "trade" -> {
                    when (parser.asString(parser.value(modified, "trade.size.input"))) {
                        "size.size", "size.usdcSize" -> {
                            val subaccountNumber = changes.subaccountNumbers?.firstOrNull()
                            val marketId = parser.asString(parser.value(modified, "trade.marketId"))
                            if (subaccountNumber != null && marketId != null) {
                                val leverage =
                                    parser.asDouble(
                                        parser.value(
                                            this.account,
                                            "subaccounts.$subaccountNumber.openPositions.$marketId.leverage.postOrder",
                                        ),
                                    )
                                modified.safeSet("trade.size.leverage", leverage)
                            } else {
                                modified.safeSet("trade.size.leverage", null)
                            }
                        }

                        else -> {
                        }
                    }
                }

                "triggerOrders" -> {
                    // TODO: update price diffs based on price.input
                }

                "closePosition", "transfer" -> {
                }
            }
            modified.safeSet("receiptLines", calculateReceipt(modified))
            this.input = modified
        }
    }

    private fun calculateReceipt(input: Map<String, Any>): List<String>? {
        return when (parser.asString(input["current"])) {
            "trade" -> {
                val trade = parser.asNativeMap(input["trade"]) ?: return null
                val type = parser.asString(trade["type"]) ?: return null
                return when (type) {
                    "MARKET", "STOP_MARKET", "TAKE_PROFIT_MARKET", "TRAILING_STOP" -> {
                        listOf(
                            ReceiptLine.ExpectedPrice.rawValue,
                            ReceiptLine.LiquidationPrice.rawValue,
                            ReceiptLine.PositionMargin.rawValue,
                            ReceiptLine.PositionLeverage.rawValue,
                            ReceiptLine.Fee.rawValue,
                        )
                    }

                    else -> {
                        listOf(
                            ReceiptLine.LiquidationPrice.rawValue,
                            ReceiptLine.PositionMargin.rawValue,
                            ReceiptLine.PositionLeverage.rawValue,
                            ReceiptLine.Fee.rawValue,
                        )
                    }
                }
            }

            "closePosition" -> {
                listOf(
                    ReceiptLine.BuyingPower.rawValue,
                    ReceiptLine.MarginUsage.rawValue,
                    ReceiptLine.ExpectedPrice.rawValue,
                    ReceiptLine.Fee.rawValue,
                )
            }

            "transfer" -> {
                val transfer = parser.asNativeMap(input["transfer"]) ?: return null
                val type = parser.asString(transfer["type"]) ?: return null
                return when (type) {
                    "DEPOSIT", "WITHDRAWAL" -> {
                        if (StatsigConfig.useSkip) {
                            listOf(
                                ReceiptLine.Equity.rawValue,
                                ReceiptLine.BuyingPower.rawValue,
                                ReceiptLine.BridgeFee.rawValue,
                                // add these back when supported by Skip
//                            ReceiptLine.ExchangeRate.rawValue,
//                            ReceiptLine.ExchangeReceived.rawValue,
//                            ReceiptLine.Fee.rawValue,
                                ReceiptLine.Slippage.rawValue,
                                ReceiptLine.TransferRouteEstimatedDuration.rawValue,
                            )
                        } else {
                            listOf(
                                ReceiptLine.Equity.rawValue,
                                ReceiptLine.BuyingPower.rawValue,
                                ReceiptLine.ExchangeRate.rawValue,
                                ReceiptLine.ExchangeReceived.rawValue,
                                ReceiptLine.Fee.rawValue,
//                                ReceiptLine.BridgeFee.rawValue,
                                ReceiptLine.Slippage.rawValue,
                                ReceiptLine.TransferRouteEstimatedDuration.rawValue,
                            )
                        }
                    }

                    "TRANSFER_OUT" -> {
                        listOf(
                            ReceiptLine.Equity.rawValue,
                            ReceiptLine.MarginUsage.rawValue,
                            ReceiptLine.Fee.rawValue,
                        )
                    }

                    else -> {
                        listOf()
                    }
                }
            }

            "adjustIsolatedMargin" -> {
                listOf(
                    ReceiptLine.CrossFreeCollateral.rawValue,
                    ReceiptLine.CrossMarginUsage.rawValue,
                    ReceiptLine.PositionLeverage.rawValue,
                    ReceiptLine.PositionMargin.rawValue,
                    ReceiptLine.LiquidationPrice.rawValue,
                )
            }

            else -> null
        }
    }

    private fun update(
        state: PerpetualState?,
        changes: StateChanges,
        tokensInfo: Map<String, TokenInfo>,
        localizer: LocalizerProtocol?,
    ): PerpetualState {
        var marketsSummary = state?.marketsSummary
        var orderbooks = state?.orderbooks
        var trades = state?.trades
        var candles = state?.candles
        var historicalFundings = state?.historicalFundings
        var assets = state?.assets?.toIMutableMap()
        var wallet = state?.wallet
        var account = state?.account
        var historicalPnl = state?.historicalPnl
        var fills = state?.fills
        var transfers = state?.transfers
        var fundingPayments = state?.fundingPayments
        var configs = state?.configs
        var input = state?.input
        var transferStatuses = state?.transferStatuses?.toIMutableMap()
        var trackStatuses = state?.trackStatuses?.toIMutableMap()
        val restriction = state?.restriction
        val geo = state?.compliance

        if (changes.changes.contains(Changes.markets)) {
            parser.asNativeMap(data?.get("markets"))?.let {
                marketsSummary =
                    PerpetualMarketSummary.apply(marketsSummary, parser, it, this.assets, changes)
            } ?: run {
                marketsSummary = null
            }
        }
        if (changes.changes.contains(Changes.orderbook)) {
            val markets = changes.markets
            orderbooks = if (markets != null) {
                val modified = orderbooks?.toIMutableMap() ?: iMutableMapOf()
                for (marketId in markets) {
                    val data =
                        parser.asNativeMap(
                            parser.value(
                                data,
                                "markets.markets.$marketId.orderbook",
                            ),
                        )
                    val existing = orderbooks?.get(marketId)
                    val orderbook = MarketOrderbook.create(existing, parser, data)
                    modified.typedSafeSet(marketId, orderbook)
                }
                modified
            } else {
                null
            }
        }
        if (changes.changes.contains(Changes.trades)) {
            val markets = changes.markets
            if (markets != null) {
                val modified = trades?.toIMutableMap() ?: mutableMapOf()
                for (marketId in markets) {
                    val data = parser.asList(
                        parser.value(
                            data,
                            "markets.markets.$marketId.trades",
                        ),
                    ) as? IList<Map<String, Any>>
                    val existing = trades?.get(marketId)
                    val trades = MarketTrade.create(existing, parser, data, localizer)
                    modified.typedSafeSet(marketId, trades)
                }
                trades = modified
            } else {
                trades = null
            }
        }
        if (changes.changes.contains(Changes.historicalFundings)) {
            val markets = changes.markets
            if (markets != null) {
                val modified = historicalFundings?.toIMutableMap() ?: mutableMapOf()
                for (marketId in markets) {
                    val data = parser.asList(
                        parser.value(
                            data,
                            "markets.markets.$marketId.historicalFunding",
                        ),
                    ) as? IList<Map<String, Any>>
                    val existing = historicalFundings?.get(marketId)
                    val historicalFunding = MarketHistoricalFunding.create(existing, parser, data)
                    modified.typedSafeSet(marketId, historicalFunding)
                }
                historicalFundings = modified
            } else {
                historicalFundings = null
            }
        }
        if (changes.changes.contains(Changes.candles)) {
            val markets = changes.markets
            if (markets != null) {
                val modified = candles?.toIMutableMap() ?: mutableMapOf()
                for (marketId in markets) {
                    val data =
                        parser.asNativeMap(parser.value(data, "markets.markets.$marketId.candles"))
                    val existing = candles?.get(marketId)
                    val candles = MarketCandles.create(existing, parser, data)
                    modified.typedSafeSet(marketId, candles)
                }
                candles = modified
            } else {
                candles = null
            }
        }
        if (changes.changes.contains(Changes.assets)) {
            if (staticTyping) {
                assets = internalState.assets.toIMutableMap()
                if (assets.isEmpty()) {
                    assets = null
                }
            } else {
                this.assets?.let {
                    assets = assets ?: mutableMapOf<String, Asset>()
                    for ((key, data) in it) {
                        parser.asNativeMap(data)?.let {
                            Asset.create(assets?.get(key), parser, it, localizer)?.let {
                                assets!![key] = it
                            }
                        }
                    }
                } ?: run {
                    assets = null
                }
            }
        }
        if (changes.changes.contains(Changes.configs)) {
            this.configs?.let {
                configs = Configs.create(configs, parser, it, localizer)
            } ?: run {
                configs = null
            }
        }
        if (changes.changes.contains(Changes.wallet)) {
            if (staticTyping) {
                wallet = Wallet.create(internalState.wallet)
            } else {
                this.wallet?.let {
                    wallet = Wallet.createDeprecated(
                        existing = wallet,
                        parser = parser,
                        data = it,
                    )
                } ?: run {
                    wallet = null
                }
            }
        }
        val subaccountNumbers = changes.subaccountNumbers ?: allSubaccountNumbers()
        val accountData = this.account
        if (accountData != null) {
            if (changes.changes.contains(Changes.subaccount)) {
                account = if (account == null) {
                    Account.create(
                        existing = null,
                        parser = parser,
                        data = accountData,
                        tokensInfo = tokensInfo,
                        localizer = localizer,
                        staticTyping = staticTyping,
                        internalState = internalState.wallet.account,
                    )
                } else {
                    val subaccounts = account.subaccounts?.toIMutableMap() ?: mutableMapOf()
                    for (subaccountNumber in subaccountNumbers) {
                        val subaccount = Subaccount.create(
                            existing = account.subaccounts?.get("$subaccountNumber"),
                            parser = parser,
                            data = subaccount(subaccountNumber),
                            localizer = localizer,
                            staticTyping = staticTyping,
                            internalState = internalState.wallet.account.subaccounts[subaccountNumber],
                        )
                        subaccounts.typedSafeSet("$subaccountNumber", subaccount)
                    }
                    val groupedSubaccounts = account.groupedSubaccounts?.toIMutableMap() ?: mutableMapOf()
                    for (subaccountNumber in subaccountNumbers) {
                        if (subaccountNumber < NUM_PARENT_SUBACCOUNTS) {
                            val subaccount = Subaccount.create(
                                existing = account.groupedSubaccounts?.get("$subaccountNumber"),
                                parser = parser,
                                data = groupedSubaccount(subaccountNumber),
                                localizer = localizer,
                                staticTyping = staticTyping,
                                internalState = internalState.wallet.account.groupedSubaccounts[subaccountNumber],
                            )
                            groupedSubaccounts.typedSafeSet("$subaccountNumber", subaccount)
                        }
                    }
                    Account(
                        account.balances,
                        subaccounts,
                        groupedSubaccounts,
                    )
                }
            }
            if (changes.changes.contains(Changes.accountBalances)) {
                account = Account.create(
                    existing = account,
                    parser = parser,
                    data = accountData,
                    tokensInfo = tokensInfo,
                    localizer = localizer,
                    staticTyping = staticTyping,
                    internalState = internalState.wallet.account,
                )
            }
        } else {
            account = null
            fills = null
            historicalPnl = null
            transfers = null
            fundingPayments = null
        }
        for (subaccountNumber in subaccountNumbers) {
            val subaccountText = "$subaccountNumber"
            val subaccount =
                parser.asNativeMap(parser.value(this.account, "groupedSubaccounts.$subaccountNumber")) ?: parser.asNativeMap(parser.value(this.account, "subaccounts.$subaccountNumber"))

            if (changes.changes.contains(Changes.historicalPnl)) {
                val now = ServerTime.now()
                val start = now - historicalPnlDays.days
                val modifiedHistoricalPnl = historicalPnl?.toIMutableMap() ?: mutableMapOf()
                var subaccountHistoricalPnl = historicalPnl?.get(subaccountText)
                if (subaccountHistoricalPnl?.size == 1) {
                    // Check if the PNL was generated from equity
                    val first = subaccountHistoricalPnl.firstOrNull()
                    if (first === dummySubaccountPNLs[subaccountText]) {
                        subaccountHistoricalPnl = null
                    }
                }

                if (staticTyping) {
                    subaccountHistoricalPnl =
                        internalState.wallet.account.subaccounts[subaccountNumber]?.historicalPNLs?.toIList()?.filter {
                            it.createdAtMilliseconds >= start.toEpochMilliseconds()
                        }
                } else {
                    val subaccountHistoricalPnlData =
                        (subaccountHistoricalPnl(subaccountNumber) as? IList<Map<String, Any>>)?.mutable()
                            ?: mutableListOf()

                    subaccountHistoricalPnl = SubaccountHistoricalPNL.create(
                        existing = subaccountHistoricalPnl,
                        parser = parser,
                        data = subaccountHistoricalPnlData,
                        startTime = start,
                    )
                }
                modifiedHistoricalPnl.typedSafeSet(subaccountText, subaccountHistoricalPnl)
                historicalPnl = modifiedHistoricalPnl
            }
            if (changes.changes.contains(Changes.fills)) {
                val modifiedFills = fills?.toIMutableMap() ?: mutableMapOf()
                var subaccountFills = fills?.get(subaccountText)
                if (staticTyping) {
                    val newFills = internalState.wallet.account.subaccounts[subaccountNumber]?.fills?.toIList()
                    subaccountFills = SubaccountFill.merge(
                        existing = subaccountFills,
                        new = newFills,
                    )
                } else {
                    subaccountFills = SubaccountFill.create(
                        subaccountFills,
                        parser,
                        subaccountFills(subaccountNumber) as? IList<Map<String, Any>>,
                        localizer,
                    )
                }
                modifiedFills.typedSafeSet(subaccountText, subaccountFills)
                fills = modifiedFills
            }
            if (changes.changes.contains(Changes.transfers)) {
                val modifiedTransfers = transfers?.toIMutableMap() ?: mutableMapOf()
                var subaccountTransfers = transfers?.get(subaccountText)
                subaccountTransfers = SubaccountTransfer.create(
                    subaccountTransfers,
                    parser,
                    subaccountTransfers(subaccountNumber) as? IList<Map<String, Any>>,
                )
                modifiedTransfers.typedSafeSet(subaccountText, subaccountTransfers)
                transfers = modifiedTransfers
            }
            if (changes.changes.contains(Changes.fundingPayments)) {
                val modifiedFundingPayments = fundingPayments?.toIMutableMap() ?: mutableMapOf()
                var subaccountFundingPayments = fundingPayments?.get(subaccountText)
                subaccountFundingPayments = SubaccountFundingPayment.create(
                    subaccountFundingPayments,
                    parser,
                    subaccountFundingPayments(subaccountNumber) as? IList<Map<String, Any>>,
                )
                modifiedFundingPayments.typedSafeSet(subaccountText, subaccountFundingPayments)
                fundingPayments = modifiedFundingPayments
            }

            if (changes.changes.contains(Changes.input)) {
                this.input = inputValidator.validate(
                    subaccountNumber,
                    this.wallet,
                    this.user,
                    subaccount,
                    parser.asNativeMap(this.marketsSummary?.get("markets")),
                    this.input,
                    this.configs,
                    this.currentBlockAndHeight,
                    this.environment,
                )
                this.input?.let {
                    input = Input.create(input, parser, it, environment, internalState)
                }
            }
        }
        if (changes.changes.contains(Changes.transferStatuses)) {
            this.transferStatuses?.let {
                transferStatuses = transferStatuses ?: mutableMapOf<String, TransferStatus>()
                for ((key, data) in it) {
                    parser.asNativeMap(data)?.let {
                        val status = TransferStatus.create(transferStatuses?.get(key), parser, it)
                        if (status != null) {
                            transferStatuses!![key] = status
                        } else {
                            transferStatuses!!.remove(key)
                        }
                    }
                }
            }
        }
        if (changes.changes.contains(Changes.trackStatuses)) {
            this.trackStatuses?.let {
                trackStatuses = trackStatuses ?: mutableMapOf<String, Boolean>()
                for ((key, data) in it) {
                    val isTracked = parser.asBool(data)
                    if (isTracked != null) {
                        trackStatuses!![key] = isTracked
                    } else {
                        trackStatuses!!.remove(key)
                    }
                }
            }
        }
        return PerpetualState(
            assets,
            marketsSummary,
            orderbooks,
            candles,
            trades,
            historicalFundings,
            wallet,
            account,
            historicalPnl,
            fills,
            transfers,
            fundingPayments,
            configs,
            input,
            subaccountNumbersWithPlaceholders(maxSubaccountNumber()),
            transferStatuses,
            trackStatuses,
            restriction,
            geo,
        )
    }

    private fun priceOverwrite(markets: Map<String, Any>): Map<String, Any>? {
        // TODO(@aforaleka): Uncomment when protocol can match collateralization check at limit price
        // if (parser.asString(input?.get("current")) == "trade") {
        //     val trade = parser.asNativeMap(input?.get("trade"))
        //     when (parser.asString(trade?.get("type"))) {
        //         "LIMIT", "STOP_LIMIT", "TAKE_PROFIT", "TRAILING_STOP", "STOP_MARKET", "TAKE_PROFIT_MARKET" -> {
        //             val price = parser.asDouble(parser.value(trade, "summary.price"))
        //             val marketId = parser.asString(trade?.get("marketId"))
        //             if (marketId != null && price != null) {
        //                 val market = parser.asNativeMap(markets[marketId])
        //                 val oraclePrice =
        //                     parser.asDouble(market?.get("oraclePrice"))
        //                 if (oraclePrice != null) {
        //                     val side = parser.asString(trade?.get("side"))
        //                     if ((side == "BUY" && price < oraclePrice) || (side == "SELL" && price > oraclePrice)) {
        //                         return iMapOf(marketId to price)
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }
        return null
    }

    private fun setMarkets(markets: Map<String, Any>?) {
    }

    fun setHistoricalPnlDays(days: Int, subaccountNumber: Int): StateResponse {
        return if (historicalPnlDays != days) {
            historicalPnlDays = days
            val now = ServerTime.now()
            val startTime = now - days.days
            val historicalPnls = state?.historicalPnl?.get("$subaccountNumber") ?: return noChange()
            val first = historicalPnls.firstOrNull() ?: return noChange()
            val changes = StateChanges(iListOf(Changes.historicalPnl))
            state = update(state, changes, tokensInfo, localizer)
            StateResponse(state, changes)
        } else {
            noChange()
        }
    }

    fun clearInput(subaccountNumber: Int): StateResponse {
        val input = input
        return if (input != null) {
            val current = parser.asString(input["current"])
            val modified = when (current) {
                "trade", "closePosition" -> clearTradeInput(input)
                "transfer" -> clearTransferInput(input)
                else -> null
            }
            if (modified != null) {
                this.input = modified

                val changes = StateChanges(
                    iListOf(Changes.input, Changes.subaccount),
                    null,
                    iListOf(subaccountNumber),
                )
                state = update(state, changes, tokensInfo, localizer)
                StateResponse(state, changes)
            } else {
                noChange()
            }
        } else {
            noChange()
        }
    }

    private fun clearTradeInput(input: Map<String, Any>): Map<String, Any> {
        val trade = parser.asNativeMap(input["trade"])?.toMutableMap()
        trade?.safeSet("size", null)
        trade?.safeSet("price", null)
        val modifiedInput = input.toMutableMap()
        modifiedInput.safeSet("trade", trade)
        return modifiedInput
    }

    private fun clearTransferInput(input: Map<String, Any>): Map<String, Any> {
        val trade = parser.asNativeMap(input["trade"])?.toMutableMap()
        trade?.safeSet("size", null)
        trade?.safeSet("price", null)
        val modifiedInput = input.toMutableMap()
        modifiedInput.safeSet("trade", trade)
        return modifiedInput
    }

    fun received(subaccountNumber: Int, height: BlockAndTime?): StateResponse {
        val wallet = wallet
        if (wallet != null) {
            val (modifiedWallet, updated) = walletProcessor.received(
                wallet,
                subaccountNumber,
                height,
            )
            if (updated) {
                this.wallet = wallet

                val changes = StateChanges(iListOf(Changes.subaccount))
                state = update(state, changes, tokensInfo, localizer)
                return StateResponse(state, changes)
            }
        }
        return noChange()
    }

    fun parseOnChainEquityTiers(payload: String): StateResponse {
        var changes: StateChanges? = null
        var error: ParsingError? = null
        try {
            changes = onChainEquityTiers(payload)
        } catch (e: ParsingException) {
            error = e.toParsingError()
        }
        if (changes != null) {
            update(changes)
        }

        val errors = if (error != null) iListOf(error) else null
        return StateResponse(state, changes, errors)
    }

    fun parseOnChainFeeTiers(payload: String): StateResponse {
        var changes: StateChanges? = null
        var error: ParsingError? = null
        try {
            changes = onChainFeeTiers(payload)
        } catch (e: ParsingException) {
            error = e.toParsingError()
        }
        if (changes != null) {
            update(changes)
        }

        val errors = if (error != null) iListOf(error) else null
        return StateResponse(state, changes, errors)
    }

    fun parseOnChainUserFeeTier(payload: String): StateResponse {
        var changes: StateChanges? = null
        var error: ParsingError? = null
        try {
            changes = onChainUserFeeTier(payload)
        } catch (e: ParsingException) {
            error = e.toParsingError()
        }
        if (changes != null) {
            update(changes)
        }

        val errors = if (error != null) iListOf(error) else null
        return StateResponse(state, changes, errors)
    }

    fun parseOnChainUserStats(payload: String): StateResponse {
        var changes: StateChanges? = null
        var error: ParsingError? = null
        try {
            changes = onChainUserStats(payload)
        } catch (e: ParsingException) {
            error = e.toParsingError()
        }
        if (changes != null) {
            update(changes)
        }

        val errors = if (error != null) iListOf(error) else null
        return StateResponse(state, changes, errors)
    }

    fun updateResponse(changes: StateChanges?): StateResponse {
        if (changes != null) {
            update(changes)
        }

        return StateResponse(state, changes, null)
    }
}
