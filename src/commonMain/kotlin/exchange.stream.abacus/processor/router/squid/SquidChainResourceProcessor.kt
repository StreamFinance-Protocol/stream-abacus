package exchange.stream.abacus.processor.router.squid

import exchange.stream.abacus.output.input.TransferInputChainResource
import exchange.stream.abacus.protocols.ParserProtocol

internal class SquidChainResourceProcessor(
    private val parser: ParserProtocol
) {
    fun received(
        payload: Map<String, Any>
    ): TransferInputChainResource {
        return TransferInputChainResource(
            chainName = parser.asString(payload["chainName"]),
            rpc = parser.asString(payload["rpc"]),
            networkName = parser.asString(payload["networkName"]),
            chainId = parser.asInt(payload["chainId"]),
            iconUrl = parser.asString(payload["chainIconURI"]),
        )
    }
}
