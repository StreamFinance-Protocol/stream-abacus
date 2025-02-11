package exchange.stream.abacus.processor.configs

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import exchange.stream.abacus.processor.base.BaseProcessor
import exchange.stream.abacus.protocols.ParserProtocol
import kotlin.math.pow

internal class WithdrawalCapacityProcessor(parser: ParserProtocol) : BaseProcessor(parser) {
    private val processor = LimiterCapacitiesProcessor(parser = parser)

    override fun received(
        existing: Map<String, Any>?,
        payload: Map<String, Any>
    ): Map<String, Any>? {
        val modified = mutableMapOf<String, Any>()
        parser.asNativeList(payload?.get("limiterCapacityList"))?.let { limiterCapacityList ->
            val processedLimiterCapacityList = processor.received(limiterCapacityList)
            modified["limiterCapacityList"] = processedLimiterCapacityList
            if (limiterCapacityList.size != 2) {
                return existing
            }
            val dailyLimit = parser.asDecimal(parser.asMap(limiterCapacityList[0])?.get("capacity"))
            val weeklyLimit = parser.asDecimal(parser.asMap(limiterCapacityList[1])?.get("capacity"))
            if (dailyLimit != null && weeklyLimit != null) {
                var maxWithdrawalCapacity: BigDecimal?
                if (dailyLimit < weeklyLimit) {
                    maxWithdrawalCapacity = dailyLimit
                } else {
                    maxWithdrawalCapacity = weeklyLimit
                }

                val usdcDecimals = environment?.tokens?.get("usdc")?.decimals ?: 6
                maxWithdrawalCapacity /= BigDecimal.fromDouble(10.0.pow(usdcDecimals))

                parser.asString(maxWithdrawalCapacity)?.let {
                    modified["maxWithdrawalCapacity"] = it
                }
            }
        }

        return modified
    }
}
