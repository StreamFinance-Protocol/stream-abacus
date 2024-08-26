package exchange.stream.abacus.state.internalstate

import exchange.stream.abacus.output.input.SelectionOption
import exchange.stream.abacus.output.input.TransferInputChainResource
import exchange.stream.abacus.output.input.TransferInputTokenResource

internal data class InternalTransferInputState(
    var chains: List<SelectionOption>? = null,
    var tokens: List<SelectionOption>? = null,
    var chainResources: Map<String, TransferInputChainResource>? = null,
    var tokenResources: Map<String, TransferInputTokenResource>? = null,
)
