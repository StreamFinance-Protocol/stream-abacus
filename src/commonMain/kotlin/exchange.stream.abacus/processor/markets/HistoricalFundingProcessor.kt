package exchange.stream.abacus.processor.markets

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol

internal class HistoricalFundingProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val keyMap = mapOf(
        "double" to mapOf(
            "rate" to "rate",
            "price" to "price",
        ),
        "datetime" to mapOf(
            "effectiveAt" to "effectiveAt",
        ),
    )

    override fun received(existing: Map<String, Any>?, payload: Map<String, Any>): Map<String, Any> {
        return transform(existing, payload, keyMap)
    }
}
