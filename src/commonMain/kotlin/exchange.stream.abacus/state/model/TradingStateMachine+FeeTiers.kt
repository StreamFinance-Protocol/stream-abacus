package exchange.stream.abacus.state.model

import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import kollections.iListOf

internal fun TradingStateMachine.onChainFeeTiers(payload: String): StateChanges {
    val json = parser.decodeJsonObject(payload)
    val tiers = parser.asList(parser.value(json, "params.tiers"))
    return if (tiers != null) {
        receivedOnChainFeeTiers(tiers)
    } else {
        StateChanges(iListOf())
    }
}

private fun TradingStateMachine.receivedOnChainFeeTiers(payload: List<Any>): StateChanges {
    configs = configsProcessor.receivedOnChainFeeTiers(configs, payload)
    return StateChanges(iListOf(Changes.configs))
}
