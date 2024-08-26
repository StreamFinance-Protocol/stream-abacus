package exchange.stream.abacus.state.manager

import exchange.stream.abacus.output.ComplianceAction
import exchange.stream.abacus.output.Restriction
import exchange.stream.abacus.output.input.SelectionOption
import exchange.stream.abacus.protocols.TransactionCallback
import exchange.stream.abacus.state.model.AdjustIsolatedMarginInputField
import exchange.stream.abacus.state.model.ClosePositionInputField
import exchange.stream.abacus.state.model.TradeInputField
import exchange.stream.abacus.state.model.TransferInputField
import exchange.stream.abacus.state.model.TriggerOrdersInputField
import exchange.stream.abacus.utils.IList
import kotlin.js.JsExport

@JsExport
interface AsyncAbacusStateManagerProtocol {
    // Connection environments
    val availableEnvironments: IList<SelectionOption>
    var environmentId: String?
    val environment: V4Environment?

    // app should set it to true when foregrounded and with network connection
    var readyToConnect: Boolean

    // account/subaccount data options
    var historicalPnlPeriod: HistoricalPnlPeriod

    // market data options
    var orderbookGrouping: OrderbookGrouping
    var candlesResolution: String

    val appSettings: AppSettings?

    // client requested gas token
    var gasToken: GasToken?

    // input fields
    fun trade(data: String?, type: TradeInputField?)
    fun closePosition(data: String?, type: ClosePositionInputField)
    fun transfer(data: String?, type: TransferInputField?)
    fun triggerOrders(data: String?, type: TriggerOrdersInputField?)
    fun adjustIsolatedMargin(data: String?, type: AdjustIsolatedMarginInputField?)

    // helper functions
    fun isMarketValid(marketId: String?): Boolean
    fun transferStatus(
        hash: String,
        fromChainId: String?,
        toChainId: String?,
        isCctp: Boolean,
        requestId: String?
    )

    // Refresh some part of the state
    fun refresh(data: ApiData)

    // If FE wants to send onchain transactions without using commit... functions,
    // these functions provide payload
    fun placeOrderPayload(): HumanReadablePlaceOrderPayload?
    fun closePositionPayload(): HumanReadablePlaceOrderPayload?
    fun triggerOrdersPayload(): HumanReadableTriggerOrdersPayload?
    fun cancelOrderPayload(orderId: String): HumanReadableCancelOrderPayload?
    fun depositPayload(): HumanReadableDepositPayload?
    fun withdrawPayload(): HumanReadableWithdrawPayload?
    fun subaccountTransferPayload(): HumanReadableSubaccountTransferPayload?
    fun adjustIsolatedMarginPayload(): HumanReadableSubaccountTransferPayload?

    // Commit changes with input objects
    fun commitPlaceOrder(callback: TransactionCallback): HumanReadablePlaceOrderPayload?
    fun commitClosePosition(callback: TransactionCallback): HumanReadablePlaceOrderPayload?
    fun commitTriggerOrders(callback: TransactionCallback): HumanReadableTriggerOrdersPayload?
    fun commitAdjustIsolatedMargin(callback: TransactionCallback): HumanReadableSubaccountTransferPayload?
    fun stopWatchingLastOrder()
    fun commitTransfer(callback: TransactionCallback)
    fun commitCCTPWithdraw(callback: TransactionCallback)

    // Commit changes with params
    fun faucet(amount: Double, callback: TransactionCallback)
    fun cancelOrder(orderId: String, callback: TransactionCallback)

    // Bridge functions.
    // If client is not using cancelOrder function, it should call orderCanceled function with
    // payload from v4-client to process state
    fun orderCanceled(orderId: String)

    // Screen for restrictions
    fun screen(address: String, callback: (restriction: Restriction) -> Unit)

    // Trigger update for compliance
    fun triggerCompliance(action: ComplianceAction, callback: TransactionCallback)

    // Get chain data from id. Necessary to know chain name based on chain id
    fun getChainById(chainId: String): TransferChainInfo?
}

@JsExport
interface AsyncAbacusStateManagerSingletonProtocol {
    var accountAddress: String?
    var sourceAddress: String?
    var subaccountNumber: Int
    var market: String?
    var cosmosWalletConnected: Boolean?
}

@JsExport
interface SingletonAsyncAbacusStateManagerProtocol :
    AsyncAbacusStateManagerProtocol,
    AsyncAbacusStateManagerSingletonProtocol

@JsExport
fun AsyncAbacusStateManagerSingletonProtocol.setAddresses(
    source: String?,
    account: String?
) {
    accountAddress = account
    sourceAddress = source
}
