package exchange.stream.abacus.processor.wallet.account

import exchange.stream.abacus.output.account.SubaccountOrder
import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.processor.base.mergeWithIds
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.manager.BlockAndTime
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.safeSet
import exchange.stream.abacus.utils.typedSafeSet
import indexer.models.IndexerCompositeOrderObject

internal class OrdersProcessor(
    parser: ParserProtocol,
    localizer: LocalizerProtocol?,
    private val orderProcessor: OrderProcessorProtocol = OrderProcessor(parser = parser, localizer = localizer),
) : BaseProcessor(parser) {

    private val itemProcessor: OrderProcessor? = orderProcessor as? OrderProcessor

    fun process(
        existing: List<SubaccountOrder>?,
        payload: List<IndexerCompositeOrderObject>,
        height: BlockAndTime?,
        subaccountNumber: Int
    ): List<SubaccountOrder> {
        val new = payload.mapNotNull { eachPayload ->
            val orderId = eachPayload.id ?: eachPayload.clientId
            orderProcessor.process(
                existing = existing?.find { it.id == orderId },
                payload = eachPayload,
                subaccountNumber = subaccountNumber,
                height = height,
            )
        }
        existing?.let {
            return mergeWithIds(new, existing) { item -> item.id }
        }
        return new
    }

    internal fun received(
        existing: Map<String, Any>?,
        payload: List<Any>?,
        height: BlockAndTime?,
        subaccountNumber: Int?,
    ): Map<String, Any>? {
        return if (payload != null) {
            val orders = existing?.mutable() ?: mutableMapOf<String, Any>()
            for (data in payload) {
                parser.asNativeMap(data)?.let { data ->
                    val orderId = parser.asString(data["id"] ?: data["clientId"])
                    val modified = data.toMutableMap()
                    val orderSubaccountNumber = parser.asInt(data["subaccountNumber"])

                    if (orderSubaccountNumber == null) {
                        modified.safeSet("subaccountNumber", subaccountNumber)
                    }

                    if (orderId != null) {
                        val existing = parser.asNativeMap(orders[orderId])
                        val order = itemProcessor?.received(existing, modified, height)
                        orders.typedSafeSet(orderId, order)
                    }
                }
            }

            orders
        } else {
            existing
        }
    }

    internal fun updateHeight(
        existing: Map<String, Any>,
        height: BlockAndTime?
    ): Pair<Map<String, Any>, Boolean> {
        var updated = false
        val modified = existing.mutable()
        for ((key, item) in existing) {
            val order = parser.asNativeMap(item)
            if (order != null && itemProcessor != null) {
                val (modifiedOrder, orderUpdated) = itemProcessor.updateHeightDeprecated(order, height)
                if (orderUpdated) {
                    modified[key] = modifiedOrder
                    updated = orderUpdated
                }
            }
        }

        return Pair(modified, updated)
    }

    internal fun canceled(
        existing: Map<String, Any>,
        orderId: String,
    ): Pair<Map<String, Any>, Boolean> {
        val order = parser.asNativeMap(existing.get(orderId))
        return if (order != null) {
            val modified = existing.mutable()
            itemProcessor?.canceled(order)
            modified.typedSafeSet(orderId, order)
            Pair(modified, true)
        } else {
            Pair(existing, false)
        }
    }
}
