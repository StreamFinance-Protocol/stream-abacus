package exchange.stream.abacus.state.model

import exchange.stream.abacus.calculator.MarginCalculator
import exchange.stream.abacus.protocols.asTypedList
import exchange.stream.abacus.protocols.asTypedObject
import exchange.stream.abacus.responses.SocketInfo
import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.utils.Logger
import indexer.codegen.IndexerFillResponse
import indexer.models.chain.OnChainAccountBalanceObject
import indexer.models.chain.OnChainUserFeeTierResponse
import indexer.models.chain.OnChainUserStatsResponse
import kollections.iListOf
import kollections.iMutableListOf
import kollections.toIList
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

internal fun TradingStateMachine.receivedSubaccountSubscribed(
    payload: Map<String, Any>,
    height: BlockAndTime?,
): StateChanges {
    this.wallet = walletProcessor.subscribedDeprecated(wallet, payload, height)
    if (staticTyping) {
        walletProcessor.processSubscribed(internalState.wallet, payload, height)
    }

    val changes = iMutableListOf<Changes>()
    if (payload["account"] != null || payload["subaccount"] != null) {
        changes.add(Changes.subaccount)
        changes.add(Changes.input)
    }
    changes.add(Changes.fills)
    changes.add(Changes.transfers)
    changes.add(Changes.fundingPayments)
    changes.add(Changes.historicalPnl)
    val subaccountNumber = parser.asInt(payload["subaccountNumber"]) ?: 0
    val subaccountNumbers = MarginCalculator.getChangedSubaccountNumbers(
        parser,
        account,
        subaccountNumber ?: 0,
        parser.asMap(input?.get("trade")),
    )

    return StateChanges(
        changes,
        null,
        subaccountNumbers,
    )
}

internal fun TradingStateMachine.receivedSubaccountsChanges(
    payload: Map<String, Any>,
    info: SocketInfo,
    height: BlockAndTime?,
): StateChanges {
    this.wallet = walletProcessor.channel_dataDeprecated(wallet, payload, info, height)
    if (staticTyping) {
        walletProcessor.processChannelData(internalState.wallet, payload, info, height)
    }

    val changes = iMutableListOf<Changes>()
    val idElements = info.id?.split("/")
    val subaccountNumber =
        if (idElements?.size == 2) parser.asInt(idElements.lastOrNull()) ?: 0 else 0
    val childSubaccountNumber = info.childSubaccountNumber
    val subaccountNumbers = iMutableListOf(subaccountNumber)

    if (childSubaccountNumber != null && !subaccountNumbers.contains(childSubaccountNumber)) {
        subaccountNumbers.add(childSubaccountNumber)
    }

    if (payload["accounts"] != null ||
        payload["subaccounts"] != null ||
        payload["positions"] != null ||
        payload["perpetualPositions"] != null ||
        payload["assetPositions"] != null ||
        payload["orders"] != null
    ) {
        changes.add(Changes.subaccount)
        changes.add(Changes.input)
        changes.add(Changes.historicalPnl)
    }
    if (payload["fills"] != null) {
        changes.add(Changes.fills)
    }
    if (payload["transfers"] != null) {
        changes.add(Changes.transfers)
    }
    if (payload["fundingPayments"] != null) {
        changes.add(Changes.fundingPayments)
    }
    return StateChanges(
        changes,
        null,
        subaccountNumbers,
    )
}

internal fun TradingStateMachine.receivedBatchSubaccountsChanges(
    payload: List<Any>,
    info: SocketInfo,
    height: BlockAndTime?
): StateChanges {
    var changes = iListOf<Changes>()
    var subaccountNumbers = iListOf<Int>()
    for (item in payload) {
        parser.asMap(item)?.let {
            val itemChanges = receivedSubaccountsChanges(it, info, height)
            val changedSubaccountNumbers = itemChanges.subaccountNumbers?.toList()
            if (changedSubaccountNumbers != null) {
                subaccountNumbers = subaccountNumbers.union(changedSubaccountNumbers).toIList()
            }
            changes = changes.union(itemChanges.changes).toIList()
        }
    }
    return StateChanges(changes, null, subaccountNumbers)
}

internal fun TradingStateMachine.onChainUserFeeTier(payload: String): StateChanges {
    val json = parser.decodeJsonObject(payload)
    if (staticTyping) {
        val payload = parser.asTypedObject<OnChainUserFeeTierResponse>(json)
        val oldValue = internalState.wallet.user?.copy()
        walletProcessor.processOnChainUserFeeTier(internalState.wallet, payload)
        return if (oldValue != internalState.wallet.user) {
            StateChanges(iListOf(Changes.wallet), null)
        } else {
            StateChanges(iListOf())
        }
    } else {
        return if (json != null) {
            receivedOnChainUserFeeTier(json)
        } else {
            StateChanges.noChange
        }
    }
}

private fun TradingStateMachine.receivedOnChainUserFeeTier(payload: Map<String, Any>): StateChanges {
    this.wallet = walletProcessor.receivedOnChainUserFeeTierDeprecated(wallet, payload)
    return StateChanges(iListOf(Changes.wallet), null)
}

internal fun TradingStateMachine.onChainUserStats(payload: String): StateChanges {
    val json = parser.decodeJsonObject(payload)
    if (staticTyping) {
        val payload = parser.asTypedObject<OnChainUserStatsResponse>(json)
        val oldValue = internalState.wallet.user?.copy()
        walletProcessor.processOnChainUserStats(internalState.wallet, payload)
        return if (oldValue != internalState.wallet.user) {
            StateChanges(iListOf(Changes.wallet), null)
        } else {
            StateChanges(iListOf())
        }
    } else {
        return if (json != null) {
            this.wallet = walletProcessor.receivedOnChainUserStatsDeprecated(wallet, json)
            StateChanges(iListOf(Changes.wallet), null)
        } else {
            StateChanges.noChange
        }
    }
}

internal fun TradingStateMachine.fills(payload: String, subaccountNumber: Int): StateChanges {
    val json = parser.decodeJsonObject(payload)
    return if (json != null) {
        receivedFills(json, subaccountNumber)
    } else {
        StateChanges.noChange
    }
}

internal fun TradingStateMachine.receivedFills(
    payload: Map<String, Any>,
    subaccountNumber: Int,
): StateChanges {
    val fills = parser.asList(payload["fills"])
    val size = fills?.size ?: 0
    return if (size > 0) {
        wallet = walletProcessor.receivedFillsDeprecated(wallet, payload, subaccountNumber)
        if (staticTyping) {
            val payload = parser.asTypedObject<IndexerFillResponse>(payload)
            walletProcessor.processFills(internalState.wallet, payload?.fills?.toList(), subaccountNumber)
        }
        StateChanges(iListOf(Changes.fills), null, iListOf(subaccountNumber))
    } else {
        StateChanges(iListOf<Changes>())
    }
}

internal fun TradingStateMachine.transfers(payload: String, subaccountNumber: Int): StateChanges {
    val json = parser.decodeJsonObject(payload)
    return if (json != null) {
        receivedTransfers(json, subaccountNumber)
    } else {
        StateChanges.noChange
    }
}

internal fun TradingStateMachine.receivedTransfers(
    payload: Map<String, Any>,
    subaccountNumber: Int,
): StateChanges {
    val size = parser.asList(payload["transfers"])?.size ?: 0
    return if (size > 0) {
        wallet = walletProcessor.receivedTransfers(wallet, payload, subaccountNumber)
        StateChanges(iListOf(Changes.transfers), null, iListOf(subaccountNumber))
    } else {
        StateChanges(iListOf<Changes>())
    }
}

internal fun TradingStateMachine.orderCanceled(
    orderId: String,
    subaccountNumber: Int
): StateChanges {
    val wallet = wallet
    if (wallet != null) {
        val (modifiedWallet, updated) = walletProcessor.orderCanceled(
            wallet,
            orderId,
            subaccountNumber,
        )
        if (updated) {
            this.wallet = modifiedWallet
            return StateChanges(iListOf(Changes.subaccount), null, iListOf(subaccountNumber))
        }
    }
    return StateChanges(iListOf<Changes>())
}

internal fun TradingStateMachine.onChainAccountBalances(payload: String): StateChanges {
    return try {
        val json = Json.parseToJsonElement(payload)
        val account = json.jsonArray.toList()
        if (staticTyping) {
            val response = parser.asTypedList<OnChainAccountBalanceObject>(account)
            val oldValue = internalState.wallet.account.balances
            walletProcessor.processAccountBalances(internalState.wallet, response)
            if (oldValue != internalState.wallet.account.balances) {
                return StateChanges(iListOf(Changes.accountBalances), null)
            } else {
                return StateChanges(iListOf())
            }
        } else {
            this.wallet = walletProcessor.receivedAccountBalances(wallet, account)
            return StateChanges(iListOf(Changes.accountBalances), null)
        }
    } catch (exception: SerializationException) { // JSON Deserialization exception
        Logger.e {
            "Failed to deserialize onChainAccountBalances: $payload \n" +
                "Exception: $exception"
        }
        StateChanges(iListOf())
    } catch (exception: IllegalArgumentException) { // .jsonArray exception
        Logger.e {
            "Failed to deserialize onChainAccountBalances: $payload \n" +
                "Exception: $exception"
        }
        StateChanges(iListOf())
    }
}
