package exchange.stream.abacus.state.model

import exchange.stream.abacus.processor.router.skip.SkipProcessor
import exchange.stream.abacus.processor.router.squid.SquidProcessor
import exchange.stream.abacus.state.manager.StatsigConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class TradingStateMachineTests {
    @Test
    fun testConstructRouterProcessorSkip() {
        val tradingStateMachine = TradingStateMachine(
            environment = null,
            localizer = null,
            formatter = null,
            maxSubaccountNumber = 1,
            useParentSubaccount = false,
        )
        StatsigConfig.useSkip = true
        assertTrue(tradingStateMachine.routerProcessor is SkipProcessor)
    }

    @Test
    fun testConstructRouterProcessorSquid() {
        val tradingStateMachine = TradingStateMachine(
            environment = null,
            localizer = null,
            formatter = null,
            maxSubaccountNumber = 1,
            useParentSubaccount = false,
        )
        StatsigConfig.useSkip = false
        assertTrue(tradingStateMachine.routerProcessor is SquidProcessor)
    }
}
