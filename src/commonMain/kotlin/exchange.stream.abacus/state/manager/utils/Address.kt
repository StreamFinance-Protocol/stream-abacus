package exchange.stream.abacus.state.manager.utils

import kotlin.jvm.JvmInline

sealed interface Address {
    val rawAddress: String
}

@JvmInline
value class EvmAddress(override val rawAddress: String) : Address

@JvmInline
value class StreamAddress(override val rawAddress: String) : Address
