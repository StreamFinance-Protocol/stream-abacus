package exchange.stream.abacus.processor.markets

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.processor.base.mergeWithIds
import exchange.stream.abacus.protocols.ParserProtocol

@Suppress("UNCHECKED_CAST")
internal class TradesProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    @Suppress("PropertyName")
    private val LIMIT = 500

    private val tradeProcessor = TradeProcessor(parser = parser)
    internal fun subscribed(
        content: Map<String, Any>,
    ): List<Any>? {
        val payload =
            parser.asNativeList(content["trades"])
        return if (payload != null) received(payload) else null
    }

    @Suppress("FunctionName")
    internal fun channel_data(
        existing: List<Any>?,
        content: Map<String, Any>,
    ): List<Any>? {
        val payload =
            parser.asNativeList(content["trades"]) as? List<Map<String, Any>>
        return if (payload != null) receivedChanges(existing, payload) else existing
    }

    private fun received(payload: List<Any>): List<Any> {
        return payload.mapNotNull { item ->
            parser.asNativeMap(item)?.let {
                tradeProcessor.received(null, it)
            }
        }.toList()
    }

    private fun receivedChanges(
        existing: List<Any>?,
        payload: List<Any>?,
    ): List<Any>? {
        if (payload != null) {
            val new = payload.mapNotNull { eachPayload ->
                parser.asNativeMap(eachPayload)?.let { eachPayloadData -> tradeProcessor.received(null, eachPayloadData) }
            }
            val merged = existing?.let {
                mergeWithIds(new, existing) { data -> parser.asNativeMap(data)?.let { parser.asString(it["id"]) } }
            } ?: new

            return if (merged.size > LIMIT) {
                merged.subList(0, LIMIT)
            } else {
                merged
            }
        } else {
            return existing
        }
    }
}
