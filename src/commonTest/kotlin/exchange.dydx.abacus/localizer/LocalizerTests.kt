package exchange.stream.abacus.localizer

import exchange.stream.abacus.payload.BaseTests
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizerTests {
    @Test
    fun test() {
        val ioImplementations = BaseTests.testIOImplementations()

        val localizer = BaseTests.testLocalizer(ioImplementations)

        val localized = localizer.localize("APP")
        assertEquals("app-doc", localized)
    }
}
