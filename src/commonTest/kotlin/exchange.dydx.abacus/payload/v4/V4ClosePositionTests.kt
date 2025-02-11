package exchange.stream.abacus.payload.v4

import exchange.stream.abacus.state.model.ClosePositionInputField
import exchange.stream.abacus.state.model.closePosition
import exchange.stream.abacus.tests.extensions.log
import exchange.stream.abacus.utils.ServerTime
import kotlin.test.Test

class V4ClosePositionTests : V4BaseTests() {
    @Test
    fun testDataFeed() {
        setup()

        var time = ServerTime.now()
        testClosePositionInput()
        time = perp.log("Close Position", time)

        testCloseShortPositionInput()
        time = perp.log("Close Position", time)
    }

    override fun setup() {
        loadMarkets()
        loadMarketsConfigurations()
        // do not load account

        loadOrderbook()
        loadSubaccounts()
    }

    private fun testClosePositionInput() {
        /*
        Initial setup
         */
        test(
            {
                perp.closePosition("ETH-USD", ClosePositionInputField.market, 0)
            },
            """
            {
                "input": {
                    "current": "closePosition",
                    "closePosition": {
                        "type": "MARKET",
                        "side": "SELL",
                        "reduceOnly": true
                    }
                }
            }
            """.trimIndent(),
        )

        test(
            {
                perp.closePosition("0.25", ClosePositionInputField.percent, 0)
            },
            """
            {
                "input": {
                    "current": "closePosition",
                    "closePosition": {
                        "type": "MARKET",
                        "side": "SELL",
                        "size": {
                            "percent": 0.25,
                            "input": "size.percent",
                            "size": 2.692,
                            "usdcSize": 4453.3756
                        },
                        "reduceOnly": true,
                        "summary": {
                            "price": 1654.3,
                            "size": 2.692,
                            "usdcSize": 4453.3756,
                             "total": 4453.3756
                        }
                    }
                },
                "wallet": {
                    "account": {
                        "subaccounts": {
                            "0": {
                                "quoteBalance": {
                                    "current": 99872.368956,
                                    "postOrder": 104592.23425040001
                                },
                                "openPositions": {
                                    "ETH-USD": {
                                        "size": {
                                            "current": 10.771577,
                                            "postOrder": 8.079577
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        test(
            {
                perp.closePosition("9", ClosePositionInputField.size, 0)
            },
            """
            {
                "input": {
                    "current": "closePosition",
                    "closePosition": {
                        "type": "MARKET",
                        "side": "SELL",
                        "size": {
                            "percent": null,
                            "input": "size.size",
                            "size": "9",
                            "usdcSize": 14888.699999999999
                        },
                        "reduceOnly": true,
                        "summary": {
                            "price": 1654.3,
                            "size": 9.0,
                            "usdcSize": 14888.699999999999,
                            "total": 14888.699999999999
                        }
                    }
                },
                "wallet": {
                    "account": {
                        "subaccounts": {
                            "0": {
                                "quoteBalance": {
                                    "current": 99872.368956,
                                    "postOrder": 115652.007756
                                },
                                "openPositions": {
                                    "ETH-USD": {
                                        "size": {
                                            "current": 10.771577,
                                            "postOrder": 1.7715770000000006
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
        )
    }

    private fun testCloseShortPositionInput() {
        test(
            {
                perp.socket(mock.socketUrl, mock.accountsChannel.v4_subscribed, 0, null)
            },
            """
                {
                }
            """.trimIndent(),
        )
        /*
        Initial setup
         */
        test(
            {
                perp.closePosition("ETH-USD", ClosePositionInputField.market, 0)
            },
            """
            {
                "input": {
                    "current": "closePosition",
                    "closePosition": {
                        "type": "MARKET",
                        "side": "BUY",
                        "reduceOnly": true
                    }
                }
            }
            """.trimIndent(),
        )

        test(
            {
                perp.closePosition("0.25", ClosePositionInputField.percent, 0)
            },
            """
            {
                "input": {
                    "current": "closePosition",
                    "closePosition": {
                        "type": "MARKET",
                        "side": "BUY",
                        "size": {
                            "percent": 0.25,
                            "input": "size.percent",
                            "size": 2.6544E+1
                        },
                        "reduceOnly": true
                    }
                },
                "wallet": {
                    "account": {
                        "subaccounts": {
                            "0": {
                                "quoteBalance": {
                                    "current": 68257.22,
                                    "postOrder": 24308.3
                                },
                                "openPositions": {
                                    "ETH-USD": {
                                        "size": {
                                            "current": -106.18,
                                            "postOrder": -79.64
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        test(
            {
                perp.closePosition("15", ClosePositionInputField.size, 0)
            },
            """
            {
                "input": {
                    "current": "closePosition",
                    "closePosition": {
                        "type": "MARKET",
                        "side": "BUY",
                        "size": {
                            "percent": null,
                            "input": "size.size",
                            "size": 15,
                            "usdcSize": 24835.5
                        },
                        "reduceOnly": true,
                        "summary": {
                            "price": 1655.7,
                            "size": 15.0,
                            "usdcSize": 24835.5,
                            "total": -24835.5
                        }
                    }
                },
                "wallet": {
                    "account": {
                        "subaccounts": {
                            "0": {
                                "quoteBalance": {
                                    "current": 68257.22,
                                    "postOrder": 43421.72
                                },
                                "openPositions": {
                                    "ETH-USD": {
                                        "size": {
                                            "current": -106.18,
                                            "postOrder": -91.18
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
        )
    }
}
