package exchange.stream.abacus.validator.transfer

import exchange.stream.abacus.output.input.ErrorType
import exchange.stream.abacus.output.input.TransferType
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.app.helper.Formatter
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.state.manager.V4Environment
import exchange.stream.abacus.validator.BaseInputValidator
import exchange.stream.abacus.validator.TransferValidatorProtocol

internal class WithdrawalGatingValidator(
    localizer: LocalizerProtocol?,
    formatter: Formatter?,
    parser: ParserProtocol,
) : BaseInputValidator(localizer, formatter, parser), TransferValidatorProtocol {
    override fun validateTransfer(
        wallet: Map<String, Any>?,
        subaccount: Map<String, Any>?,
        transfer: Map<String, Any>,
        configs: Map<String, Any>?,
        currentBlockAndHeight: BlockAndTime?,
        restricted: Boolean,
        environment: V4Environment?
    ): List<Any>? {
        val currentBlock = currentBlockAndHeight?.block ?: Int.MAX_VALUE // parser.asInt(parser.value(environment, "currentBlock"))
        val withdrawalGating = parser.asMap(parser.value(configs, "withdrawalGating"))
        val withdrawalsAndTransfersUnblockedAtBlock = parser.asInt(withdrawalGating?.get("withdrawalsAndTransfersUnblockedAtBlock")) ?: 0
        var blockDurationSeconds = if (environment?.isMainNet == true) 1.1 else 1.5
        val secondsUntilUnblock = ((withdrawalsAndTransfersUnblockedAtBlock - currentBlock) * blockDurationSeconds).toInt()

        val type = parser.asString(parser.value(transfer, "type"))

        if ((type == TransferType.withdrawal.rawValue || type == TransferType.transferOut.rawValue) &&
            secondsUntilUnblock > 0
        ) {
            return listOf(
                error(
                    ErrorType.error.rawValue,
                    "",
                    null,
                    "WARNINGS.ACCOUNT_FUND_MANAGEMENT.${if (type == TransferType.withdrawal.rawValue) "WITHDRAWAL_PAUSED_ACTION" else "TRANSFERS_PAUSED_ACTION"}",
                    "WARNINGS.ACCOUNT_FUND_MANAGEMENT.${if (type == TransferType.withdrawal.rawValue) "WITHDRAWAL_PAUSED_TITLE" else "TRANSFERS_PAUSED_TITLE"}",
                    "WARNINGS.ACCOUNT_FUND_MANAGEMENT.${if (type == TransferType.withdrawal.rawValue) "WITHDRAWAL_PAUSED_DESCRIPTION" else "TRANSFERS_PAUSED_DESCRIPTION"}",
                    mapOf(
                        "SECONDS" to mapOf(
                            "value" to secondsUntilUnblock,
                            "format" to "string",
                        ),
                    ),
                    null,
                    environment?.links?.withdrawalGateLearnMore,
                    "APP.GENERAL.LEARN_MORE_ARROW",
                ),
            )
        } else {
            return null
        }
    }
}
