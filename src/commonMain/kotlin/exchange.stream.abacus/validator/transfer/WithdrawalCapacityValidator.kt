package exchange.stream.abacus.validator.transfer

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import exchange.stream.abacus.output.input.ErrorType
import exchange.stream.abacus.output.input.TransferType
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.app.helper.Formatter
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.state.manager.V4Environment
import exchange.stream.abacus.validator.BaseInputValidator
import exchange.stream.abacus.validator.TransferValidatorProtocol

internal class WithdrawalCapacityValidator(
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
        val withdrawalCapacity = parser.asMap(parser.value(configs, "withdrawalCapacity"))
        val maxWithdrawalCapacity = parser.asDecimal(parser.value(withdrawalCapacity, "maxWithdrawalCapacity")) ?: BigDecimal.fromLong(Long.MAX_VALUE)
        val type = parser.asString(parser.value(transfer, "type"))
        val size = parser.asMap(parser.value(transfer, "size"))
        val usdcSize = parser.asDecimal(size?.get("usdcSize")) ?: BigDecimal.ZERO
        val usdcSizeInputIsGreaterThanCapacity = usdcSize > maxWithdrawalCapacity

        if (type == TransferType.withdrawal.rawValue && usdcSizeInputIsGreaterThanCapacity) {
            return listOf(
                error(
                    ErrorType.error.rawValue,
                    "",
                    null,
                    "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_LIMIT_OVER_ACTION",
                    "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_LIMIT_OVER_TITLE",
                    "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_LIMIT_OVER_DESCRIPTION",
                    mapOf(
                        "USDC_LIMIT" to mapOf(
                            "value" to maxWithdrawalCapacity.doubleValue(false),
                            "format" to "price",
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
