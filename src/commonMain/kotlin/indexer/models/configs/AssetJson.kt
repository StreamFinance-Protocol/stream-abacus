package indexer.models.configs

import exchange.stream.abacus.utils.IList
import kotlinx.serialization.Serializable

/**
 * AssetJson from ${V4_WEB_URL}/configs/markets.json
 */
@Serializable
data class AssetJson(
    val name: String,
    val websiteLink: String? = null,
    val whitepaperLink: String? = null,
    val coinMarketCapsLink: String? = null,
    val tags: IList<String>? = null,
)
