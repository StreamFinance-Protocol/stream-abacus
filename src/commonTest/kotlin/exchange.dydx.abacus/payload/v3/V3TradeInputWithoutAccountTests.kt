package exchange.stream.abacus.payload.v3

import exchange.stream.abacus.state.model.TradeInputField
import exchange.stream.abacus.state.model.trade
import exchange.stream.abacus.state.model.tradeInMarket
import exchange.stream.abacus.tests.extensions.loadAccounts
import exchange.stream.abacus.tests.extensions.log
import exchange.stream.abacus.utils.ServerTime
import kotlin.test.Test

class V3TradeInputWithoutAccountTests : V3BaseTests() {
    @Test
    fun testDataFeed() {
        setup()

        var time = ServerTime.now()
        testMarketTradeInput()
        time = perp.log("Market Order", time)

        testLoadAccounts()
        time = perp.log("Loaded Account", time)
    }

    override fun setup() {
        loadMarkets()
        loadMarketsConfigurations()
        // do not load account
        loadOrderbook()
    }

    private fun testMarketTradeInput() {
        /*
        Initial setup
         */
        test({
            perp.tradeInMarket("ETH-USD", 0)
        }, null)

        test({
            perp.trade("BUY", TradeInputField.side, 0)
        }, null)

        test({
            perp.trade("MARKET", TradeInputField.type, 0)
        }, null)

        test(
            {
                perp.trade("1.", TradeInputField.size, 0)
            },
            """
                {
                    "input": {
                        "trade": {
                            "type": "MARKET",
                            "side": "BUY",
                            "marketId": "ETH-USD",
                            "timeInForce": "GTT",
                            "goodTil": {
                                "unit": "D",
                                "duration": 28
                            },
                            "fields": [
                            ],
                            "options": {
                            },
                            "summary": {
                            },
                            "size": {
                                "size": 1.0,
                                "input": "size.size"
                            }
                        },
                        "current": "trade"
                    }
                }
            """.trimIndent(),
        )
    }

    private fun testLoadAccounts() {
        test(
            {
                perp.loadAccounts(mock)
            },
            """
                {
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                }
                            }
                        }
                    },
                    "input": {
                        "trade": {
                            "type": "MARKET",
                            "side": "BUY",
                            "marketId": "ETH-USD",
                            "timeInForce": "GTT",
                            "goodTil": {
                                "unit": "D",
                                "duration": 28
                            },
                            "fields": [
                            ],
                            "options": {
                            },
                            "summary": {
                            },
                            "size": {
                                "size": 1.0,
                                "input": "size.size"
                            }
                        },
                        "current": "trade"
                    }
                }
            """.trimIndent(),
        )
    }
}
