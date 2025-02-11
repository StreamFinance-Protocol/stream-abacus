package exchange.stream.abacus.processor.configs

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol

internal class WithdrawalGatingProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val withdrawalGatingKeyMap = mapOf(
        "int" to mapOf(
            "chainOutageSeenAtBlock" to "chainOutageSeenAtBlock",
            "negativeTncSubaccountSeenAtBlock" to "negativeTncSubaccountSeenAtBlock",
            "withdrawalsAndTransfersUnblockedAtBlock" to "withdrawalsAndTransfersUnblockedAtBlock",
        ),
    )

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        return transform(existing, payload, withdrawalGatingKeyMap)
    }
}
