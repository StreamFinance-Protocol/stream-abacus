package exchange.stream.abacus.processor.configs

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol

internal class LimiterCapacitiesProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val itemProcessor = LimiterCapacityProcessor(parser = parser)

    internal fun received(
        payload: List<Any>
    ): List<Any> {
        val modified = mutableListOf<Map<String, Any>>()
        for (item in payload) {
            parser.asNativeMap(item)?.let { it ->
                itemProcessor.received(null, it)?.let { received ->
                    modified.add(received)
                }
            }
        }
        return modified
    }
}
