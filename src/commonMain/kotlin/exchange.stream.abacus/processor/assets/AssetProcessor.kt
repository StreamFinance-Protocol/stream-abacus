package exchange.stream.abacus.processor.assets

import exchange.stream.abacus.output.Asset
import exchange.stream.abacus.output.AssetResources
import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.utils.mutable
import exchange.stream.abacus.utils.safeSet
import indexer.models.configs.AssetJson

internal interface AssetProcessorProtocol {
    fun process(
        assetId: String,
        payload: AssetJson,
        deploymentUri: String,
    ): Asset
}

@Suppress("UNCHECKED_CAST")
internal class AssetProcessor(
    parser: ParserProtocol,
    private val localizer: LocalizerProtocol?
) : BaseProcessor(parser), AssetProcessorProtocol {
    private val assetConfigurationsResourcesKeyMap = mapOf(
        "string" to mapOf(
            "websiteLink" to "websiteLink",
            "whitepaperLink" to "whitepaperLink",
            "coinMarketCapsLink" to "coinMarketCapsLink",
        ),
    )

    private val assetConfigurationsKeyMap = mapOf(
        "string" to mapOf(
            "name" to "name",
        ),
        "strings" to mapOf(
            "tags" to "tags",
        ),
    )

    override fun process(
        assetId: String,
        payload: AssetJson,
        deploymentUri: String,
    ): Asset {
        val imageUrl = "$deploymentUri/currencies/${assetId.lowercase()}.png"
        val primaryDescriptionKey = "$assetId.PRIMARY"
        val secondaryDescriptionKey = "$assetId.SECONDARY"
        val primaryDescription = localizer?.localize(primaryDescriptionKey)
        val secondaryDescription = localizer?.localize(secondaryDescriptionKey)

        return Asset(
            id = assetId,
            name = payload.name,
            tags = payload.tags,
            resources = AssetResources(
                websiteLink = payload.websiteLink,
                whitepaperLink = payload.whitepaperLink,
                coinMarketCapsLink = payload.coinMarketCapsLink,
                imageUrl = imageUrl,
                primaryDescriptionKey = primaryDescriptionKey,
                secondaryDescriptionKey = secondaryDescriptionKey,
                primaryDescription = primaryDescription,
                secondaryDescription = secondaryDescription,
            ),
        )
    }

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        return existing
    }

    internal fun receivedConfigurations(
        assetId: String,
        asset: Map<String, Any>?,
        payload: Map<String, Any>,
        deploymentUri: String,
    ): Map<String, Any> {
        val received = transform(asset, payload, assetConfigurationsKeyMap)
        val resources = transform(
            parser.asNativeMap(asset?.get("resources")),
            payload,
            assetConfigurationsResourcesKeyMap,
        ).mutable()
        val imageUrl = "$deploymentUri/currencies/${assetId.lowercase()}.png"
        val primaryDescriptionKey = "__ASSETS.$assetId.PRIMARY"
        val secondaryDescriptionKey = "__ASSETS.$assetId.SECONDARY"
        resources.safeSet("imageUrl", imageUrl)
        resources.safeSet("primaryDescriptionKey", primaryDescriptionKey)
        resources.safeSet("secondaryDescriptionKey", secondaryDescriptionKey)
        received["id"] = assetId
        received["resources"] = resources

        return received
    }
}
