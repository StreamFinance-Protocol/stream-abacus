package exchange.stream.abacus.processor.router.squid

import exchange.stream.abacus.output.input.TransferInputTokenResource
import exchange.stream.abacus.protocols.ParserProtocol

internal class SquidTokenResourceProcessor(
    private val parser: ParserProtocol
) {
    fun received(
        payload: Map<String, Any>
    ): TransferInputTokenResource {
        return TransferInputTokenResource(
            name = parser.asString(payload["name"]),
            address = parser.asString(payload["address"]),
            symbol = parser.asString(payload["symbol"]),
            decimals = parser.asInt(payload["decimals"]),
            iconUrl = parser.asString(payload["logoURI"]),
        )
    }
}
