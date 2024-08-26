package exchange.stream.abacus.payload.v4

import exchange.stream.abacus.payload.BaseTests
import exchange.stream.abacus.responses.StateResponse
import exchange.stream.abacus.state.app.adaptors.AbUrl
import exchange.stream.abacus.state.model.PerpTradingStateMachine
import exchange.stream.abacus.tests.extensions.loadMarkets
import exchange.stream.abacus.tests.extensions.loadMarketsConfigurations
import exchange.stream.abacus.tests.extensions.loadOrderbook
import exchange.stream.abacus.tests.extensions.loadv4SubaccountsWithPositions

open class V4BaseTests(useParentSubaccount: Boolean = false) : BaseTests(127, useParentSubaccount) {
    internal val testWsUrl =
        AbUrl.fromString("wss://indexer.v4staging.dydx.exchange/v4/ws")
    internal val testRestUrl =
        "https://indexer.v4staging.dydx.exchange"
    override fun createState(useParentSubaccount: Boolean, staticTyping: Boolean): PerpTradingStateMachine {
        return PerpTradingStateMachine(
            environment = mock.v4Environment,
            localizer = null,
            formatter = null,
            maxSubaccountNumber = 127,
            useParentSubaccount = useParentSubaccount,
            staticTyping = staticTyping,
        )
    }

    internal open fun loadMarkets(): StateResponse {
        return test({
            perp.loadMarkets(mock)
        }, null)
    }

    internal open fun loadMarketsConfigurations(): StateResponse {
        return test({
            perp.loadMarketsConfigurations(mock, deploymentUri)
        }, null)
    }

    internal open fun loadSubaccounts(): StateResponse {
        return test({
            perp.loadv4SubaccountsWithPositions(mock, "$testRestUrl/v4/addresses/cosmo")
        }, null)
    }

    open fun loadOrderbook(): StateResponse {
        return test({
            perp.loadOrderbook(mock)
        }, null)
    }

    override fun setup() {
        loadMarketsConfigurations()
        loadMarkets()
        perp.parseOnChainEquityTiers(mock.v4OnChainMock.equity_tiers)
        loadSubaccounts()
    }
}
