package exchange.stream.abacus.validator.transfer

import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.app.helper.Formatter
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.state.manager.V4Environment
import exchange.stream.abacus.validator.BaseInputValidator
import exchange.stream.abacus.validator.TransferValidatorProtocol

internal class DepositValidator(
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
        environment: V4Environment?,
    ): List<Any>? {
        return null
//        val balance = parser.asDouble(parser.value(wallet, "balance"))
//        val size = parser.asDouble(parser.value(transfer, "size.size"))
//        return if (size != null) {
//            if (balance == null) {
//                listOf(
//                    error(
//                        parser,
//                        "WARNING",
//                        "UNKNOWN_WALLET_BALANCE",
//                        null,
//                        null,
//                        "DEPOSIT_MODAL_TITLE.UNKNOWN_WALLET_BALANCE",
//                        "APP.DEPOSIT_MODAL.UNKNOWN_WALLET_BALANCE"
//                    )
//                )
//            } else if (size > balance) {
//                listOf(
//                    error(
//                        parser,
//                        "ERROR",
//                        "AMOUNT_LARGER_THANK_WALLET_BALANCE",
//                        listOf("size.size"),
//                        "APP.TRADE.MODIFY_SIZE_FIELD",
//                        "DEPOSIT_MODAL_TITLE.AMOUNT_LARGER_THANK_WALLET_BALANCE",
//                        "APP.DEPOSIT_MODAL.AMOUNT_LARGER_THANK_WALLET_BALANCE"
//                    )
//                )
//            } else null
//        } else null
    }
}
