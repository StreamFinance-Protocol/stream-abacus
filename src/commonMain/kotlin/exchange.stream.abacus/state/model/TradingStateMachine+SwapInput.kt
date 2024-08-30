package exchange.stream.abacus.state.model

import exchange.stream.abacus.calculator.SwapInputCalculator
import exchange.stream.abacus.responses.ParsingError
import exchange.stream.abacus.responses.StateResponse
import exchange.stream.abacus.state.changes.Changes
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.mutableMapOf
import exchange.stream.abacus.utils.safeSet
import kollections.JsExport
import kollections.iListOf
import kotlinx.serialization.Serializable

@JsExport
@Serializable
enum class SwapInputField(val rawValue: String) {
    fromTokenAmount("fromToken.amount"),
    fromTokenTicker("fromToken.ticker"),
    toTokenAmount("toToken.amount"),
    toTokenTicker("toToken.ticker");

    companion object {
        operator fun invoke(rawValue: String) =
        SwapInputField.values().firstOrNull { it.rawValue == rawValue }
    }
}

fun TradingStateMachine.swap(
    data: String?,
    type: SwapInputField?,
    subaccountNumber: Int,
): StateResponse {
    var changes: StateChanges? = null
    var error: ParsingError? = null
    val typeText = type?.rawValue
    val input = this.input?.mutable() ?: mutableMapOf()
    input["current"] = "swap"
    val swap =
        parser.asMap(input["swap"])?.mutable()
            ?: kotlin.run {
                val swap = mutableMapOf<String, Any>()
                val calculator = SwapInputCalculator(parser)
                val params = mutableMapOf<String, Any>()
                params.safeSet("swap", swap)
                params.safeSet("markets", parser.asMap(marketsSummary?.get("markets")))
                val modified = calculator.calculate(params)
                val value = parser.asMap(modified["swap"])?.mutable() ?: swap
                value
            }
    if (typeText != null) {
        if (validSwapInput(swap, typeText)) {
            when (typeText) {
                SwapInputField.fromTokenAmount.rawValue -> {
                    swap.safeSet("fromToken.amount", parser.asDouble(data))
                    changes = StateChanges(
                        iListOf(Changes.input),
                        null,
                        iListOf(subaccountNumber),
                    )
                }
                SwapInputField.toTokenAmount.rawValue -> {
                    swap.safeSet("toToken.amount", parser.asDouble(data))
                    changes = StateChanges(
                        iListOf(Changes.input),
                        null,
                        iListOf(subaccountNumber),
                    )
                }

                SwapInputField.fromTokenTicker.rawValue -> {
                    swap.safeSet("fromToken.ticker", parser.asString(data))
                    val markets = parser.asMap(marketsSummary?.get("markets"))
                    val market = markets?.entries?.find {
                        val marketData = it.value as? Map<String, Any>
                        marketData?.get("assetId") == parser.asString(data)
                    }
                    val symbol = market?.key
                    swap.safeSet("fromToken.symbol", symbol)

                    changes = StateChanges(
                        iListOf(Changes.input),
                        null,
                        iListOf(subaccountNumber),
                    )
                }
                SwapInputField.toTokenTicker.rawValue -> {
                    swap.safeSet("toToken.ticker", parser.asString(data))
                    val markets = parser.asMap(marketsSummary?.get("markets"))
                    val market = markets?.entries?.find {
                        val marketData = it.value as? Map<String, Any>
                        marketData?.get("assetId") == parser.asString(data)
                    }
                    val symbol = market?.key
                    swap.safeSet("toToken.symbol", symbol)

                    changes = StateChanges(
                        iListOf(Changes.input),
                        null,
                        iListOf(subaccountNumber),
                    )
                }
                else -> {}
            }
        } else {
            error = cannotModify(typeText)
        }
    } else {
        changes =
            StateChanges(
                iListOf(Changes.input),
                null,
                iListOf(subaccountNumber),
            )
    }
    input["swap"] = swap
    this.input = input
    changes?.let { update(it) }
    return StateResponse(state, changes, if (error != null) iListOf(error) else null)
}

fun TradingStateMachine.changeSwapDirection(subaccountNumber: Int): StateResponse {
    val changes: StateChanges? = null
    val input = this.input?.mutable() ?: mutableMapOf()
    val swap = parser.asMap(input["swap"])?.mutable() ?: mutableMapOf()
    val fromToken = parser.asNativeMap(swap["fromToken"])
    val toToken = parser.asNativeMap(swap["toToken"])

    if (fromToken != null && toToken != null) {
        swap["fromToken"] = toToken
        swap["toToken"] = fromToken
        input["swap"] = swap
        StateChanges(iListOf(Changes.input), null, iListOf(subaccountNumber))
    } else {
        StateChanges(iListOf(Changes.input), null, iListOf(subaccountNumber))
    }
    this.input = input
    if (changes != null) update(changes)
    return StateResponse(state, changes, null)
}

fun TradingStateMachine.validSwapInput(
    swap: Map<String, Any>,
    typeText: String?
): Boolean {
    // TODO: implement this function
    return true
}
