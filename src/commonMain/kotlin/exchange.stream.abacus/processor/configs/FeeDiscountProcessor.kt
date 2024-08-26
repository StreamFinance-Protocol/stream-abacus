package exchange.stream.abacus.processor.configs

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol

@Suppress("UNCHECKED_CAST")
internal class FeeDiscountProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val feeDiscountKeyMap = mapOf(
        "string" to mapOf(
            "tier" to "tier",
            "symbol" to "symbol",
        ),
        "double" to mapOf(
            "discount" to "discount",
        ),
        "int" to mapOf(
            "balance" to "balance",
        ),
    )

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val received = transform(existing, payload, feeDiscountKeyMap)
        val tier = received["tier"]
        if (tier != null) {
            received["id"] = tier
            received["resources"] = mapOf("stringKey" to "FEE_DISCOUNT.$tier")
        }
        return received
    }
}
