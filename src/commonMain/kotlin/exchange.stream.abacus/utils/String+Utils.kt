package exchange.stream.abacus.utils

import exchange.stream.abacus.utils.beth32.Bech32
import kotlin.String

fun String.isAddressValid(): Boolean {
    try {
        val (humanReadablePart, data) = Bech32.decode(this)
        return humanReadablePart == "dydx"
    } catch (e: Exception) {
        return false
    }
}

fun String.toNobleAddress(): String? {
    try {
        val (humanReadablePart, data) = Bech32.decode(this)
        if (humanReadablePart != "dydx") {
            return null
        }
        return Bech32.encode("noble", data)
    } catch (e: Exception) {
        return null
    }
}

fun String.toOsmosisAddress(): String? {
    try {
        val (humanReadablePart, data) = Bech32.decode(this)
        if (humanReadablePart != "dydx") {
            return null
        }
        return Bech32.encode("osmo", data)
    } catch (e: Exception) {
        return null
    }
}
fun String.toNeutronAddress(): String? {
    try {
        val (humanReadablePart, data) = Bech32.decode(this)
        if (humanReadablePart != "dydx") {
            return null
        }
        return Bech32.encode("neutron", data)
    } catch (e: Exception) {
        return null
    }
}

fun String.toStreamAddress(): String? {
    try {
        val (humanReadablePart, data) = Bech32.decode(this)
        if (humanReadablePart != "noble") {
            return null
        }
        // TODO: replace this with our bech32 prefix
        return Bech32.encode("dydx", data)
    } catch (e: Exception) {
        return null
    }
}

fun String.toCamelCase(): String {
    return this.split("_", "-")
        .mapIndexed { index, s -> if (index == 0) s.lowercase() else s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
        .joinToString("")
}
