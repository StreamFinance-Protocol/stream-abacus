package exchange.stream.abacus.payload.v4

import exchange.stream.abacus.state.model.TradeInputField
import exchange.stream.abacus.state.model.trade
import exchange.stream.abacus.state.model.tradeInMarket
import exchange.stream.abacus.tests.extensions.loadOrderbook
import kotlin.test.Test

class V4NoAccountTradeInputTests : V4BaseTests() {
    @Test
    fun testNoAccount() {
        // Due to the JIT compiler nature for JVM (and Kotlin) and JS, Android/web would ran slow the first round. Second round give more accurate result
        setup()

        print("--------First round----------\n")

        testOnce()

        reset()

        print("--------Second round----------\n")

        testOnce()
    }

    override fun setup() {
        loadMarkets()
        loadMarketsConfigurations()
        loadOrderbook()
    }

    private fun testOnce() {
        test(
            {
                perp.tradeInMarket("ETH-USD", 0)
            },
            """
            {
                "input": {
                    "trade": {
                        "marketId": "ETH-USD"
                    }
                }
            }
            """.trimIndent(),
        )

        test({
            perp.trade("LIMIT", TradeInputField.type, 0)
        }, null)

        test({
            perp.trade("BUY", TradeInputField.side, 0)
        }, null)

        test({
            perp.trade("0.2", TradeInputField.size, 0)
        }, null)

        test({
            perp.trade("1500", TradeInputField.limitPrice, 0)
        }, null)

        test(
            {
                perp.trade("1", TradeInputField.limitPrice, 0)
            },
            """
            {
                "input": {
                    "errors": [
                        {
                            "type": "ERROR",
                            "code": "REQUIRED_WALLET",
                            "action": "/onboard",
                            "resources": {
                                "title": {
                                    "stringKey": "ERRORS.TRADE_BOX_TITLE.CONNECT_WALLET_TO_TRADE"
                                },
                                "text": {
                                    "stringKey": "ERRORS.TRADE_BOX.CONNECT_WALLET_TO_TRADE"
                                },
                                "action": {
                                    "stringKey": "ERRORS.TRADE_BOX_TITLE.CONNECT_WALLET_TO_TRADE"
                                }
                            }
                        }
                    ]
                }
            }
            """.trimIndent(),
        )
    }
}
