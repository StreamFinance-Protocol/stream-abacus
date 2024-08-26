package exchange.stream.abacus.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTests {
    @Test
    fun testAddressConversion() {
        val streamAddress = "dydx14zzueazeh0hj67cghhf9jypslcf9sh2n5k6art"
        val nobleAddress = "noble14zzueazeh0hj67cghhf9jypslcf9sh2n4vp3mj"
        assertEquals(nobleAddress, streamAddress.toNobleAddress())
        assertEquals(streamAddress, nobleAddress?.toStreamAddress())
    }
}
