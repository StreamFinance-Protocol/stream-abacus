package exchange.stream.abacus.payload.v3

import exchange.stream.abacus.payload.BaseTests
import exchange.stream.abacus.responses.StateResponse
import exchange.stream.abacus.tests.extensions.loadAccounts
import exchange.stream.abacus.tests.extensions.loadFeeDiscounts
import exchange.stream.abacus.tests.extensions.loadFeeTiers
import exchange.stream.abacus.tests.extensions.loadMarkets
import exchange.stream.abacus.tests.extensions.loadMarketsConfigurations
import exchange.stream.abacus.tests.extensions.loadOrderbook
import exchange.stream.abacus.tests.extensions.loadUser

open class V3BaseTests : BaseTests(0, false) {
    internal fun loadMarkets(): StateResponse {
        return test({
            perp.loadMarkets(mock)
        }, null)
    }

    internal fun loadMarketsConfigurations(): StateResponse {
        return perp.loadMarketsConfigurations(mock, deploymentUri)
    }

    internal fun loadAccounts(): StateResponse {
        return test({
            perp.loadAccounts(mock)
        }, null)
    }

    internal fun loadUser(): StateResponse {
        return test({
            perp.loadUser(mock)
        }, null)
    }

    override fun setup() {
        loadMarkets()
        loadMarketsConfigurations()
        perp.parseOnChainEquityTiers(mock.v4OnChainMock.equity_tiers)
        loadAccounts()
        loadUser()
    }

    internal fun loadOrderbook(): StateResponse {
        return test({
            perp.loadOrderbook(mock)
        }, null)
    }

    internal fun loadFeeTiers(): StateResponse {
        return test({
            perp.loadFeeTiers(mock)
        }, null)
    }

    internal fun loadFeeDiscounts(): StateResponse {
        return test({
            perp.loadFeeDiscounts(mock)
        }, null)
    }
}
