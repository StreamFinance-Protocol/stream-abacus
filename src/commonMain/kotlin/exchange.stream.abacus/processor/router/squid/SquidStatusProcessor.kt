package exchange.stream.abacus.processor.router.squid

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.mutable

internal class SquidStatusProcessor(
    parser: ParserProtocol,
    private val transactionId: String?,
) : BaseProcessor(parser) {

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>,
    ): Map<String, Any> {
        val modified = existing?.mutable() ?: mutableMapOf()
        val hash = transactionId ?: parser.asString(parser.value(payload, "fromChain.transactionId"))
        if (hash != null) {
            modified[hash] = payload
        }
        return modified
    }
}
