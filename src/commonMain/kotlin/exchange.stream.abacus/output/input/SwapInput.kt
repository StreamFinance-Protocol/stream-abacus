package exchange.stream.abacus.output.input

import exchange.stream.abacus.output.Asset
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.IList
import exchange.stream.abacus.utils.Logger
import kollections.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class SwapInputTokenOptions(   
    val assets: Map<String, Asset>? = null
) {
    companion object {
        internal fun create(
            existing: SwapInputTokenOptions?,
            assets: Map<String, Asset>?
        ): SwapInputTokenOptions? {
            Logger.d { "creating Swap Input Token Options\n" }

            return if (existing?.assets != assets) {
                SwapInputTokenOptions(assets)
            } else {
                existing
            }
        }
    }
}


@JsExport
@Serializable
data class SwapInputSummary(
    val fee: Double?,
    val slippage: Double?,
    val minTotal: Double?,
) {
    companion object {
        internal fun create(
            existing: SwapInputSummary?,
            parser: ParserProtocol,
            data: Map<*, *>?
        ): SwapInputSummary? {
            Logger.d { "creating Swap Input Summary\n" }

            data?.let {
                val fee = parser.asDouble(parser.value(data, "fee"))
                val slippage = parser.asDouble(parser.value(data, "slippage"))
                val minTotal = parser.asDouble(parser.value(data, "minTotal"))

                return if (
                    existing?.fee != fee ||
                    existing?.slippage != slippage ||
                    existing?.minTotal != minTotal
                ) {
                    SwapInputSummary(
                        fee,
                        slippage,
                        minTotal
                    )
                } else {
                    existing
                }
            }
            Logger.d { "Swap Input Summary not valid" }
            return null
        }
    }
}


@JsExport
@Serializable
data class SwapInputToken(
    val ticker: String?,
    val amount: String?,
    val symbol: String?
) {
    companion object {
        internal fun create(
            existing: SwapInputToken?,
            parser: ParserProtocol,
            data: Map<*, *>?,
        ): SwapInputToken? {
            Logger.d { "creating Swap Input Token\n" }

            data?.let {
                val ticker = parser.asString(data["ticker"])
                val amount = parser.asString(data["amount"])
                val symbol = parser.asString(data["symbol"])
                return if (existing?.ticker != ticker ||
                    existing?.amount != amount ||
                    existing?.symbol != symbol
                ) {
                    SwapInputToken(ticker, amount, symbol)
                } else {
                    existing
                }
            }
            Logger.d { "Swap Input Token not valid" }
            return null
        }
    }
}


@JsExport
@Serializable
data class SwapInput(
    val fromToken: SwapInputToken?,
    val toToken: SwapInputToken?,
    val summary: SwapInputSummary?
) {
    companion object {
        internal fun create(
            existing: SwapInput?,
            parser: ParserProtocol,
            data: Map<*, *>?
        ): SwapInput? {
            Logger.d { "creating Swap Input\n" }

            data?.let {
                val fromToken = SwapInputToken.create(
                    existing?.fromToken,
                    parser,
                    parser.asMap(data["fromToken"]),
                )
                val toToken = SwapInputToken.create(
                    existing?.toToken,
                    parser,
                    parser.asMap(data["toToken"]),
                )
                val summary = SwapInputSummary.create(
                    existing?.summary,
                    parser,
                    parser.asMap(data["summary"]),
                )

                return if (
                    existing?.fromToken != fromToken ||
                    existing?.toToken != toToken ||
                    existing?.summary != summary
                ) {
                    SwapInput(
                        fromToken,
                        toToken,
                        summary
                    )
                } else {
                    existing
                }
            }
            Logger.d { "Swap Input not valid" }
            return null
        }
    }
}
