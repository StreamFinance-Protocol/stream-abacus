package exchange.stream.abacus.calculator

import exchange.stream.abacus.output.input.IsolatedMarginAdjustmentType
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.Numeric
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.safeSet

@Suppress("UNCHECKED_CAST")
internal class SwapInputCalculator(val parser: ParserProtocol) {
    internal fun calculate(
        state: Map<String, Any>,
    ): Map<String, Any> {
        val swapInput = parser.asNativeMap(state["swap"])
        val markets = parser.asNativeMap(state["markets"])
        return if (swapInput != null && markets != null) {
            // TODO: calculate the swap inputs (e.g. if the fromToken input is changed, we need to recalculate the toToken input, and vice versa)
            // val modifiedSwapInput = calculateSwapInput(swapInput, markets)
            val finalized = finalize(swapInput)
            finalized
            // TODO: See if we need to apply the swap to the wallet
            // val modifiedWallet = subaccountTransformer.applyTransferToWallet(
            //     wallet,
            //     subaccountNumber,
            //     modifiedTransfer,
            //     parser,
            //     "postOrder",
            // )
            // modified["wallet"] = modifiedWallet

        } else {
            state
        }
    }

    private fun finalize(
        swapInput: Map<String, Any>
    ): Map<String, Any> {
        val modified = swapInput.mutable()
        val fromToken = parser.asNativeMap(swapInput["fromToken"]) ?: return modified
        val toToken = parser.asNativeMap(swapInput["toToken"]) ?: return modified
        if (fromToken["amount"] != null && toToken["amount"] != null && fromToken["ticker"] != null && toToken["ticker"] != null) {
            modified.safeSet("summary", summary(fromToken, toToken))
        }
        return modified
    }


    private fun summary(
        fromToken: Map<String, Any>,
        toToken: Map<String, Any>,
    ): Map<String, Any> {
        val summary = mutableMapOf<String, Any>()
        
        // TODO: calculate fee
        summary.safeSet("fee", 2)
        // TODO: calculate slippage
        summary.safeSet("slippage", 2)
        // TODO: calculate minTotal
        summary.safeSet("minTotal", 1000)
        return summary
    }

    private fun calculateSwapInput(
        swapInput: Map<String, Any>,
        markets: Map<String, Any>?
    ): Map<String, Any> {
        val modified = swapInput.mutable()

        val fromToken = parser.asNativeMap(swapInput["fromToken"]) ?: return modified
        val toToken = parser.asNativeMap(swapInput["toToken"]) ?: return modified

        val fromTokenTicker = parser.asString(fromToken["ticker"])
        val fromTokenAmount = parser.asString(fromToken["amount"])
        val toTokenTicker = parser.asString(toToken["ticker"])
        val toTokenAmount = parser.asString(toToken["amount"])

        if (fromTokenTicker != null && fromTokenAmount != null && toTokenTicker != null && toTokenAmount == null) {
            val calculatedToAmount = calculateToTokenAmount(fromToken, toToken, markets)
            modified.safeSet("toToken.amount", calculatedToAmount?.toString())
        } else if (toTokenTicker != null && toTokenAmount != null && fromTokenTicker != null && fromTokenAmount == null) {
            val calculatedFromAmount = calculateFromTokenAmount(fromToken, toToken, markets)
            modified.safeSet("fromToken.amount", calculatedFromAmount?.toString())
        }

        return modified
    }

    private fun calculateToTokenAmount(
        fromToken: Map<String, Any>,
        toToken: Map<String, Any>,
        markets: Map<String, Any>?
    ): Double? {

        val fromTokenSymbol = parser.asString(fromToken["symbol"]) ?: return null
        val toTokenSymbol = parser.asString(toToken["symbol"]) ?: return null
        val fromAmount = parser.asDouble(fromToken["amount"]) ?: return null

        val fromTokenMarket = parser.asNativeMap(markets?.get(fromTokenSymbol))
        val toTokenMarket = parser.asNativeMap(markets?.get(toTokenSymbol))

        val fromTokenPrice = parser.asDouble(fromTokenMarket?.get("oraclePrice")) ?: return null
        val toTokenPrice = parser.asDouble(toTokenMarket?.get("oraclePrice")) ?: return null

        if (toTokenPrice == 0.0) return null

        return (fromAmount * fromTokenPrice) / toTokenPrice
    }

    private fun calculateFromTokenAmount(
        fromToken: Map<String, Any>,
        toToken: Map<String, Any>,
        markets: Map<String, Any>?
    ): Double? {

        val fromTokenSymbol = parser.asString(fromToken["symbol"]) ?: return null
        val toTokenSymbol = parser.asString(toToken["symbol"]) ?: return null
        val toAmount = parser.asDouble(toToken["amount"]) ?: return null

        val fromTokenMarket = parser.asNativeMap(markets?.get(fromTokenSymbol))
        val toTokenMarket = parser.asNativeMap(markets?.get(toTokenSymbol))

        val fromTokenPrice = parser.asDouble(fromTokenMarket?.get("oraclePrice")) ?: return null
        val toTokenPrice = parser.asDouble(toTokenMarket?.get("oraclePrice")) ?: return null

        if (fromTokenPrice == 0.0) return null

        return (toAmount * toTokenPrice) / fromTokenPrice
    }

    internal fun updateToTokenAmount(
        state: Map<String, Any>,
        swapInput: Map<String, Any>
    ): Map<String, Any> {
        val markets = parser.asNativeMap(state["markets"])
        val fromToken = parser.asNativeMap(swapInput["fromToken"]) ?: return swapInput
        val toToken = parser.asNativeMap(swapInput["toToken"]) ?: return swapInput

        val calculatedAmount = calculateToTokenAmount(fromToken, toToken, markets)

        val modifiedToToken = toToken.mutable()
        modifiedToToken.safeSet("amount", calculatedAmount)

        val modified = swapInput.mutable()
        modified.safeSet("toToken", modifiedToToken)

        return modified
    }

    internal fun updateFromTokenAmount(
        state: Map<String, Any>,
        swapInput: Map<String, Any>
    ): Map<String, Any> {
        val markets = parser.asNativeMap(state["markets"])
        val fromToken = parser.asNativeMap(swapInput["fromToken"]) ?: return swapInput
        val toToken = parser.asNativeMap(swapInput["toToken"]) ?: return swapInput

        val calculatedAmount = calculateFromTokenAmount(fromToken, toToken, markets)

        val modifiedFromToken = fromToken.mutable()
        modifiedFromToken.safeSet("amount", calculatedAmount)

        val modified = swapInput.mutable()
        modified.safeSet("fromToken", modifiedFromToken)

        return modified
    }
}
