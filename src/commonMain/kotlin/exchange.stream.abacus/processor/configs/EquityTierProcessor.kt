package exchange.stream.abacus.processor.configs

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.QUANTUM_MULTIPLIER

@Suppress("UNCHECKED_CAST")
internal class EquityTierProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val equityTierKeyMap = mapOf(
        "int" to mapOf(
            "maxOrders" to "maxOrders",
        ),
        "double" to mapOf(
            "requiredTotalNetCollateralUSD" to "requiredTotalNetCollateralUSD",
        ),
    )

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val received = transform(existing, payload, equityTierKeyMap)

        val requiredTotalNetCollateralUSD = parser.asDecimal(payload["usdTncRequired"])
        if (requiredTotalNetCollateralUSD != null) {
            received["requiredTotalNetCollateralUSD"] = parser.asDouble(requiredTotalNetCollateralUSD / QUANTUM_MULTIPLIER)!!
        }

        val maxOrders = payload["limit"]
        if (maxOrders != null) {
            received["maxOrders"] = maxOrders
        }

        return received
    }
}
