package exchange.stream.abacus.payload.v4

import exchange.stream.abacus.tests.extensions.loadv4SubaccountsWithPositions
import exchange.stream.abacus.tests.extensions.log
import exchange.stream.abacus.utils.SHORT_TERM_ORDER_DURATION
import exchange.stream.abacus.utils.ServerTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class V4AccountOrdersSortingTest : V4BaseTests() {
    @Test
    fun testDataFeed() {
        // Due to the JIT compiler nature for JVM (and Kotlin) and JS, Android/web would ran slow the first round. Second round give more accurate result
        setup()

        print("--------First round----------\n")

        testAccountsOnce()
    }

    private fun testAccountsOnce() {
        var time = ServerTime.now()
        testSubaccountsReceived()
        time = perp.log("Accounts Received", time)

        testSubaccountSubscribed()
        time = perp.log("Accounts Subscribed", time)
    }

    private fun testSubaccountsReceived() {
        test(
            {
                perp.loadv4SubaccountsWithPositions(mock, "$testRestUrl/v4/addresses/cosmo")
            },
            """
            {
                "wallet": {
                    "account": {
                        "subaccounts": {
                            "0": {
                                "equity": {
                                    "current": 108116.7318528828
                                },
                                "freeCollateral": {
                                    "current": 106640.3767269893
                                },
                                "quoteBalance": {
                                    "current": 99872.368956
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
        )
    }

    private fun testSubaccountSubscribed() {
        test(
            {
                perp.socket(
                    testWsUrl,
                    mock.accountsChannel.v4_subscribed_for_orders_sorting,
                    0,
                    null,
                )
            },
            """
            {
            }
            """.trimIndent(),
            { it ->
                val subaccount = it.state?.subaccount(0)
                val orders = subaccount?.orders

                assertNotNull(orders)
                val sortedOrders = orders.sortedBy {
                    it.createdAtHeight
                        ?: (if (it.goodTilBlock != null) it.goodTilBlock!! - SHORT_TERM_ORDER_DURATION else 0)
                }.reversed()
                for (i in 0 until orders.size) {
                    assertEquals(orders[i].id, sortedOrders[i].id)
                }
            },
        )
    }
}
