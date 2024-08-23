package exchange.stream.abacus.state.manager

import exchange.stream.abacus.output.input.TransferInput

data class CctpChainTokenInfo(
    val chainId: String,
    val tokenAddress: String,
) {
    fun isCctpEnabled(transferInput: TransferInput?) =
        transferInput?.chain == chainId && transferInput.token == tokenAddress
}

object CctpConfig {
    var cctpChainIds: List<CctpChainTokenInfo>? = null
}
