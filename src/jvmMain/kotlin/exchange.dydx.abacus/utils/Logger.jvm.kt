package exchange.stream.abacus.utils

actual fun platformDebugLog(message: String) {
    println(message)
}

actual fun platformErrorLog(message: String) {
    System.err.println(message)
}
