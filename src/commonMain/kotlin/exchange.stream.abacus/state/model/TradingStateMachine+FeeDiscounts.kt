package exchange.stream.abacus.state.model

import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.utils.Logger
import kollections.iEmptyList
import kollections.iListOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

internal fun TradingStateMachine.feeDiscounts(payload: String): StateChanges {
    val json = try {
        Json.parseToJsonElement(payload).jsonArray.toList()
    } catch (exception: SerializationException) {
        Logger.e {
            "Failed to deserialize feeDiscounts: $payload \n" +
                "Exception: $exception"
        }
        return StateChanges(iEmptyList())
    } catch (exception: IllegalArgumentException) { // .jsonArray exception
        Logger.e {
            "Failed to deserialize feeDiscounts: $payload \n" +
                "Exception: $exception"
        }
        return StateChanges(iEmptyList())
    }
    return receivedFeeDiscounts(json)
}

internal fun TradingStateMachine.receivedFeeDiscounts(payload: List<Any>): StateChanges {
    configs = configsProcessor.receivedFeeDiscounts(configs, payload)
    return StateChanges(iListOf(Changes.configs))
}
