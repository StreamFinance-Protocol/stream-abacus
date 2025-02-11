package exchange.stream.abacus.utils

import exchange.stream.abacus.output.input.TradeInputGoodUntil
import exchange.stream.abacus.protocols.ParserProtocol
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class GoodTil {
    companion object {
        internal fun duration(goodTil: Map<String, Any>?, parser: ParserProtocol): Duration? {
            if (goodTil === null) return null
            val duration = parser.asInt(goodTil["duration"]) ?: return null
            val timeInterval = when (parser.asString(goodTil["unit"])) {
                "M" -> duration.minutes
                "H" -> duration.hours
                "D" -> duration.days
                "W" -> (duration * 7).days
                else -> return null
            }
            return timeInterval
        }

        internal fun duration(goodTil: TradeInputGoodUntil?): Duration? {
            if (goodTil === null) return null
            val duration = goodTil.duration ?: return null
            val timeInterval = when (goodTil.unit) {
                "M" -> duration.minutes
                "H" -> duration.hours
                "D" -> duration.days
                "W" -> (duration * 7).days
                else -> return null
            }
            return timeInterval
        }
    }
}
