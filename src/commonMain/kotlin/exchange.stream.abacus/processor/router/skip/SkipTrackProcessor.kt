package exchange.stream.abacus.processor.router.skip

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.mutable

internal class SkipTrackProcessor(
    parser: ParserProtocol
) : BaseProcessor(parser) {
    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val modified = existing?.mutable() ?: mutableMapOf()
        val txHash = parser.asString(payload.get("tx_hash")) ?: return modified
        modified[txHash] = true
        return modified
    }
}
