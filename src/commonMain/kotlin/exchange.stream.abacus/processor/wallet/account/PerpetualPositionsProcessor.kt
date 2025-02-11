package exchange.stream.abacus.processor.wallet.account

import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.internalstate.InternalPerpetualPosition
import exchange.stream.abacus.utils.modify
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.safeSet
import indexer.codegen.IndexerPerpetualPositionResponseObject

internal class PerpetualPositionsProcessor(
    parser: ParserProtocol,
    localizer: LocalizerProtocol?,
    private val itemProcessor: PerpetualPositionProcessorProtocol =
        PerpetualPositionProcessor(parser = parser, localizer = localizer),
) : BaseProcessor(parser) {

    fun process(
        existing: Map<String, InternalPerpetualPosition>? = null,
        payload: Map<String, IndexerPerpetualPositionResponseObject>?,
    ): Map<String, InternalPerpetualPosition>? {
        return if (payload != null) {
            val result = mutableMapOf<String, InternalPerpetualPosition>()
            for ((key, value) in payload.entries) {
                val existingPosition = existing?.get(key)
                val newPosition = itemProcessor.process(existingPosition, value)
                if (newPosition != null) {
                    result[key] = newPosition
                } else {
                    result.remove(key)
                }
            }
            return if (result != existing) {
                result
            } else {
                existing
            }
        } else {
            existing
        }
    }

    fun processChanges(
        existing: Map<String, InternalPerpetualPosition>?,
        payload: List<IndexerPerpetualPositionResponseObject>?,
    ): Map<String, InternalPerpetualPosition>? {
        return if (payload != null) {
            val result = existing?.toMutableMap() ?: mutableMapOf()
            for (item in payload) {
                if (item.market != null) {
                    val newPosition = itemProcessor.processChanges(result[item.market], item)
                    if (newPosition != null) {
                        result[item.market] = newPosition
                    } else {
                        result.remove(item.market)
                    }
                }
            }
            return if (result != existing) {
                result
            } else {
                existing
            }
        } else {
            existing
        }
    }

    internal fun received(
        payload: Map<String, Any>?,
        subaccountNumber: Int?,
    ): Map<String, Any>? {
        if (payload != null) {
            val result = mutableMapOf<String, Any>()
            for ((key, value) in payload) {
                parser.asNativeMap(value)?.let { data ->

                    var modifiedData = data.toMutableMap()
                    subaccountNumber?.run {
                        modifiedData.modify("subaccountNumber", subaccountNumber)
                    }

                    val itemProcessor = itemProcessor as? PerpetualPositionProcessor
                    val item = itemProcessor?.received(null, modifiedData)
                    result.safeSet(key, item)
                }
            }
            return result
        }
        return null
    }

    internal fun receivedChangesDeprecated(
        existing: Map<String, Any>?,
        payload: List<Any>?,
    ): Map<String, Any>? {
        return if (payload != null) {
            val output = existing?.mutable() ?: mutableMapOf()
            for (item in payload) {
                parser.asNativeMap(item)?.let { item ->
                    parser.asString(item["market"])?.let {
                        val itemProcessor = itemProcessor as? PerpetualPositionProcessor
                        val modified =
                            itemProcessor?.receivedChangesDeprecated(parser.asNativeMap(existing?.get(it)), item)
                        output.safeSet(it, modified)
                    }
                }
            }
            output
        } else {
            existing
        }
    }
}
