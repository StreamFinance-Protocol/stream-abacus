package exchange.stream.abacus.output

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@JsExport
@Serializable
data class FAQ(
    val questionLocalizationKey: String,
    val answerLocalizationKey: String
)
