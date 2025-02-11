package exchange.stream.abacus.processor.assets

import exchange.stream.abacus.output.Asset
import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.processor.utils.MarketId
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.mutable
import indexer.models.configs.AssetJson

internal class AssetsProcessor(
    parser: ParserProtocol,
    localizer: LocalizerProtocol?
) : BaseProcessor(parser) {
    private val assetProcessor = AssetProcessor(parser = parser, localizer = localizer)

    override fun environmentChanged() {
        assetProcessor.environment = environment
    }

    internal fun processConfigurations(
        existing: MutableMap<String, Asset>,
        payload: Map<String, AssetJson>,
        deploymentUri: String
    ): MutableMap<String, Asset> {
        for ((key, data) in payload) {
            val assetId = MarketId.assetid(key)
            if (assetId != null) {
                val asset = assetProcessor.process(
                    assetId = assetId,
                    payload = data,
                    deploymentUri = deploymentUri,
                )

                existing[assetId] = asset
            }
        }

        return existing
    }

    internal fun receivedConfigurations(
        existing: Map<String, Any>?,
        payload: Map<String, Any>,
        deploymentUri: String,
    ): Map<String, Any> {
        val assets = existing?.mutable() ?: mutableMapOf<String, Any>()
        for ((key, data) in payload) {
            val assetId = MarketId.assetid(key)
            if (assetId != null) {
                val marketPayload = parser.asNativeMap(data)
                if (marketPayload != null) {
                    val receivedAsset = assetProcessor.receivedConfigurations(
                        assetId,
                        parser.asNativeMap(existing?.get(assetId)),
                        marketPayload,
                        deploymentUri,
                    )
                    assets[assetId] = receivedAsset
                }
            }
        }
        return assets
    }
}
