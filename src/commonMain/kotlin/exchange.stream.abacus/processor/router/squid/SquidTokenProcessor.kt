package exchange.stream.abacus.processor.router.squid

import exchange.stream.abacus.output.input.SelectionOption
import exchange.stream.abacus.protocols.ParserProtocol

internal class SquidTokenProcessor(
    private val parser: ParserProtocol,
) {
    fun received(
        payload: Map<String, Any>
    ): SelectionOption {
        return SelectionOption(
            stringKey = parser.asString(payload["name"]),
            string = parser.asString(payload["name"]),
            type = parser.asString(payload["address"]) ?: "",
            iconUrl = parser.asString(payload["logoURI"]),
        )
    }
}
