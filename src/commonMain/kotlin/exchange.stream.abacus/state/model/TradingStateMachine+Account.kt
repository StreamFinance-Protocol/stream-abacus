package exchange.stream.abacus.state.model

import exchange.stream.abacus.output.account.SubaccountOrder
import exchange.stream.abacus.output.input.OrderStatus
import exchange.stream.abacus.responses.StateResponse
import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.utils.Logger
import kollections.iListOf
import kollections.toIList
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal fun TradingStateMachine.account(payload: String): StateChanges {
    val json = try {
        parser.asMap(Json.parseToJsonElement(payload))
    } catch (exception: SerializationException) {
        Logger.e {
            "Failed to deserialize account: $payload \n" +
                "Exception: $exception"
        }
        null
    }
    return if (json != null) {
        receivedAccount(json)
    } else {
        StateChanges(iListOf<Changes>(), null, null)
    }
}

internal fun TradingStateMachine.receivedAccount(
    payload: Map<String, Any>
): StateChanges {
    if (staticTyping) {
        walletProcessor.processAccount(
            internalState = internalState.wallet,
            payload = payload,
        )
    }
    this.wallet = walletProcessor.receivedAccount(wallet, payload)
    return StateChanges(iListOf(Changes.subaccount))
}

internal fun TradingStateMachine.updateHeight(
    height: BlockAndTime,
): StateResponse {
    this.currentBlockAndHeight = height
    val (modifiedWallet, updated, subaccountIds) = walletProcessor.updateHeight(wallet, height)
    return if (updated) {
        this.wallet = modifiedWallet
        val changes = StateChanges(iListOf(Changes.subaccount), null, subaccountIds?.toIList())
        val realChanges = update(changes)
        StateResponse(state, realChanges, null, null)
    } else {
        return StateResponse(state, null, null, null)
    }
}

internal fun TradingStateMachine.findOrder(
    orderId: String,
    subaccountNumber: Int,
): SubaccountOrder? {
    val subaccount = state?.subaccount(subaccountNumber) ?: return null
    val orders = subaccount.orders ?: return null
    val order = orders.firstOrNull {
        it.id == orderId
    } ?: return null
    return when (order.status) {
        OrderStatus.Open, OrderStatus.Pending, OrderStatus.Untriggered -> order
        else -> null
    }
}

internal fun TradingStateMachine.findOrder(
    clientId: Int,
    subaccountNumber: Int,
): SubaccountOrder? {
    val subaccount = state?.subaccount(subaccountNumber) ?: return null
    val orders = subaccount.orders ?: return null
    val order = orders.firstOrNull {
        it.clientId == clientId
    } ?: return null
    return order
}

internal fun TradingStateMachine.findOrderInData(
    orderId: String,
    subaccountNumber: Int,
): SubaccountOrder? {
    val subaccount = state?.subaccount(subaccountNumber) ?: return null
    val orders = subaccount.orders ?: return null
    val order = orders.firstOrNull {
        it.id == orderId
    } ?: return null
    return when (order.status) {
        OrderStatus.Open, OrderStatus.Pending, OrderStatus.Untriggered -> order
        else -> null
    }
}
