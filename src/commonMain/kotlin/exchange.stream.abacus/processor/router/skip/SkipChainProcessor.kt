package exchange.stream.abacus.processor.router.skip

import exchange.stream.abacus.output.input.SelectionOption
import exchange.stream.abacus.protocols.ParserProtocol

internal class SkipChainProcessor(private val parser: ParserProtocol) {
    fun received(
        payload: Map<String, Any>
    ): SelectionOption {
        return SelectionOption(
            stringKey = parser.asString(payload["network_identifier"]) ?: parser.asString(payload["chain_name"]),
            string = parser.asString(payload["network_identifier"]) ?: parser.asString(payload["chain_name"]),
            type = parser.asString(payload["chain_id"]) ?: "",
            iconUrl = parser.asString(payload["logo_uri"]),
        )
    }
}
