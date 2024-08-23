package exchange.stream.abacus.state.model

import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import kollections.iListOf

fun TradingStateMachine.onChainWithdrawalGating(payload: String): StateChanges {
    val json = parser.decodeJsonObject(payload)
    return json?.let {
        configs = configsProcessor.receivedWithdrawalGating(configs, it)
        return StateChanges(iListOf(Changes.configs, Changes.input))
    } ?: StateChanges.noChange
}

fun TradingStateMachine.onChainWithdrawalCapacity(payload: String): StateChanges {
    val json = parser.decodeJsonObject(payload)
    return json?.let {
        configs = configsProcessor.receivedWithdrawalCapacity(configs, it)
        return StateChanges(iListOf(Changes.configs, Changes.input))
    } ?: StateChanges.noChange
}
