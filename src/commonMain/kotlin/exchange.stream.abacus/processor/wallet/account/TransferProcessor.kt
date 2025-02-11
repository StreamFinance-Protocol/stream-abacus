package exchange.stream.abacus.processor.wallet.account

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.Numeric
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.safeSet

internal class TransferProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val transferKeyMap = mapOf(
        "string" to mapOf(
            "id" to "id",
            "clientId" to "clientId",
            "type" to "type",
            "symbol" to "asset",
            "fromAddress" to "fromAddress",
            "toAddress" to "toAddress",
            "senderWallet" to "fromAddress",
            "recipientWallet" to "toAddress",
            "status" to "status",
            "transactionHash" to "transactionHash",
        ),
        "datetime" to mapOf(
            "createdAt" to "createdAt",
            "confirmedAt" to "confirmedAt",
        ),
        "double" to mapOf(
            "size" to "amount",
        ),
        "int" to mapOf(
            "subaccountNumber" to "subaccountNumber",
            "createdAtHeight" to "updatedAtBlock",
        ),
    )

    private val typeMap = mapOf(
        "DEPOSIT" to "APP.GENERAL.DEPOSIT",
        "WITHDRAWAL" to "APP.GENERAL.WITHDRAW",
        "FAST_WITHDRAWAL" to "APP.GENERAL.FAST_WITHDRAW",
        "TRANSFER_OUT" to "APP.GENERAL.TRANSFER_OUT",
        "TRANSFER_IN" to "APP.GENERAL.TRANSFER_IN",
    )

    private val typeIconMap = mapOf(
        "DEPOSIT" to "Incoming",
        "WITHDRAWAL" to "Outgoing",
        "FAST_WITHDRAWAL" to "Outgoing",
        "TRANSFER_OUT" to "Outgoing",
        "TRANSFER_IN" to "Incoming",
    )

    private val statusMap = mapOf(
        "PENDING" to "pending",
        "CONFIRMED" to "confirmed",
    )

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any> {
        val modified = transform(existing, payload, transferKeyMap).mutable()

        val sender = parser.asNativeMap(payload["sender"])
        val recipient = parser.asNativeMap(payload["recipient"])

        sender?.let {
            modified.safeSet("fromAddress", parser.asString(it["address"]))
            modified.safeSet("fromSubaccountNumber", parser.asString(it["subaccountNumber"]))
        }
        recipient?.let {
            modified.safeSet("toAddress", parser.asString(it["address"]))
            modified.safeSet("toSubaccountNumber", parser.asString(it["subaccountNumber"]))
        }

        // for v3
        val debitAmount = parser.asDouble(payload["debitAmount"])
        val creditAmount = parser.asDouble(payload["creditAmount"])
        if (debitAmount != null && debitAmount != Numeric.double.ZERO) {
            modified["amount"] = debitAmount
            modified.safeSet("asset", parser.asString(payload["debitAsset"]))
        } else if (creditAmount != null && debitAmount != Numeric.double.ZERO) {
            modified["amount"] = creditAmount
            modified.safeSet("asset", parser.asString(payload["creditAsset"]))
        }

        if (modified["status"] == null) {
            modified["status"] = "CONFIRMED"
        }
        if (modified["id"] == null) {
            modified.safeSet("id", parser.asString(payload["transactionHash"]))
        }
        updateResource(modified)
        return modified
    }

    private fun updateResource(transfer: MutableMap<String, Any>) {
        val resources = mutableMapOf<String, Any>()
        parser.asString(transfer["type"])?.let { type ->
            val modifiedType = when (type) {
                // For DEPOSIT, we always show as "Deposit"
                "WITHDRAWAL" -> {
                    val toAddress = parser.asString(transfer["toAddress"])
                    if (toAddress == accountAddress || toAddress == null) {
                        type
                    } else {
                        "TRANSFER_OUT"
                    }
                }
                else -> type
            }
            typeMap[modifiedType]?.let {
                resources["typeStringKey"] = it
            }
            typeIconMap[modifiedType]?.let {
                resources["iconLocal"] = it
            }
        }
        transfer["status"]?.let {
            statusMap[it]?.let {
                resources["indicator"] = it
            }
        }
        parser.asString(transfer["transactionHash"])?.let {
            val mintscan = environment?.links?.mintscan?.replace("{tx_hash}", it)
            resources.safeSet("blockExplorerUrl", mintscan)
        }

        transfer["resources"] = resources
    }
}
