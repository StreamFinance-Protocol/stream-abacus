package exchange.stream.abacus.protocols

import exchange.stream.abacus.output.input.SelectionOption
import exchange.stream.abacus.responses.ParsingError
import exchange.stream.abacus.utils.toJsonPrettyPrint
import kollections.JsExport

@JsExport
interface LocalizerProtocol {
    fun localize(path: String, paramsAsJson: String? = null): String
}

fun LocalizerProtocol.localizeWithParams(path: String, params: Map<String, String>): String? =
    localize(path = path, paramsAsJson = params.toJsonPrettyPrint())

interface AbacusLocalizerProtocol : LocalizerProtocol {
    val languages: List<SelectionOption>

    var language: String?

    fun setLanguage(language: String, callback: (successful: Boolean, error: ParsingError?) -> Unit)
}
