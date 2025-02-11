package exchange.stream.abacus.payload

import exchange.stream.abacus.payload.v4.V4BaseTests
import exchange.stream.abacus.state.model.TransferInputField
import exchange.stream.abacus.state.model.transfer
import exchange.stream.abacus.tests.extensions.log
import exchange.stream.abacus.utils.ServerTime
import kotlin.test.Test
import kotlin.test.assertTrue

class TransferInputTests : V4BaseTests() {
    @Test
    fun testDepositTransferInputOnce() {
        setup()
        var time = ServerTime.now()
        testDepositTransferInput()
        time = perp.log("Deposit", time)
    }

    @Test
    fun testSlowWithdrawalTransferInputOnce() {
        setup()
        var time = ServerTime.now()
        testSlowWithdrawalTransferInput()
        time = perp.log("Slow Withdrawal", time)
    }

    @Test
    fun testTransferOutTransferInputOnce() {
        setup()
        var time = ServerTime.now()
        testTransferOutTransferInput()
        perp.log("Transfer Out", time)
    }

    @Test
    fun testTransferInputTypeChangeOnce() {
        setup()
        testTransferInputTypeChange()
    }

    private fun testDepositTransferInput() {
        /*
        Designed workflow
        transfer("DEPOSIT", TransferInputField.type)
        transfer("1000", TransferInputField.usdcSize)
        transfer("0", TransferInputField.usdcFee)   // if fee is charged outside usdcSize
        transfer("1.3", TransferInputField.usdcFee) // if fee is deducted from usdcSize
         */

        test(
            {
                perp.transfer("DEPOSIT", TransferInputField.type)
            },
            """
                {
                    "input": {
                        "current": "transfer",
                        "transfer": {
                            "type": "DEPOSIT"
                        }
                    }
                }
            """.trimIndent(),
        )

        test(
            {
                perp.transfer("1", TransferInputField.usdcSize)
            },
            """
                {
                    "input": {
                        "current": "transfer",
                        "transfer": {
                            "type": "DEPOSIT",
                            "size": {
                                "usdcSize": 1.0
                            }
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                    "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 108117.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 106641.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 99873.368956
                                    },
                                     "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.27310138690337965
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.013655069345169024
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2132827.5345397857
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
                perp.transfer("5000.0", TransferInputField.usdcSize)
            },
            """
                {
                    "input": {
                        "transfer": {
                            "type": "DEPOSIT",
                            "size": {
                                "usdcSize": 5000.0
                            },
                            "summary": {
                                "usdcSize": 5000.0
                            },
                            "options": {
                                "needsSize": true,
                                "needsGasless": true
                            }
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                   "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 113116.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 111640.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 104872.368956
                                    },
                                    "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.2610321394033229
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.013051606970166163
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2232807.5345397857
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
        )

        /*
        size = 1000.0
         */

        test(
            {
                perp.transfer("1000.0", TransferInputField.usdcSize)
            },
            """
                {
                    "input": {
                        "transfer": {
                            "type": "DEPOSIT",
                            "size": {
                                "usdcSize": 1000.0
                            },
                            "summary": {
                                "usdcSize": 1000.0
                            },
                            "options": {
                                "needsSize": true,
                                "needsGasless": true
                            }
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                    "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 109116.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 107640.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 100872.368956
                                    },
                                    "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.2706010528035248
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.01353005264017626
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2152807.5345397857
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
                perp.transfer("10.0", TransferInputField.usdcFee)
            },
            """
                {
                    "input": {
                        "transfer": {
                            "type": "DEPOSIT",
                            "size": {
                                "usdcSize": 1000.0
                            },
                            "summary": {
                                "usdcSize": 1000.0,
                                "filled": true,
                                "fee": 10.0
                            },
                            "options": {
                                "needsSize": true,
                                "needsFastSpeed": false
                            },
                            "fee": 10.0
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                    "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 109106.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 107630.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 100862.368956
                                    },
                                    "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.27062585430277314
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.013531292715138643
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2152607.5345397857
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
        )
    }

    private fun testSlowWithdrawalTransferInput() {
        test({
            perp.transfer("WITHDRAWAL", TransferInputField.type)
        }, null)

        test({
            perp.transfer("false", TransferInputField.fastSpeed)
        }, null)

        test({
            perp.transfer("0", TransferInputField.usdcFee)
        }, null)

        /*
        size = 1000.0
         */
        test({
            perp.transfer("5000.0", TransferInputField.usdcSize)
        }, null)

        test(
            {
                perp.transfer("1000.0", TransferInputField.usdcSize)
            },
            """
                {
                    "input": {
                        "transfer": {
                            "type": "WITHDRAWAL",
                            "size": {
                                "usdcSize": 1000.0
                            },
                            "summary": {
                                "usdcSize": 1000.0,
                                "filled": true
                            },
                            "options": {
                                "needsSize": true,
                                "needsFastSpeed": true
                            }
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                    "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 107116.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 105640.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 98872.368956
                                    },
                                    "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.2756535044256519
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.013782675221282625
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2112807.5345397857
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
                perp.transfer("10.0", TransferInputField.usdcFee)
            },
            """
                {
                    "input": {
                        "transfer": {
                            "type": "WITHDRAWAL",
                            "size": {
                                "usdcSize": 1000.0
                            },
                            "summary": {
                                "usdcSize": 1000.0,
                                "filled": true
                            },
                            "options": {
                                "needsSize": true,
                                "needsFastSpeed": true
                            }
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                    "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 107106.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 105630.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 98862.368956
                                    },
                                    "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.2756792407635699
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.013783962038178554
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2112607.5345397857
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
        )
    }

    private fun testTransferOutTransferInput() {
        test({
            perp.transfer("TRANSFER_OUT", TransferInputField.type)
        }, null)

        test({
            perp.transfer("0.0", TransferInputField.usdcFee)
        }, null)

        /*
        size = 1000.0
         */
        test({
            perp.transfer("5000.0", TransferInputField.usdcSize)
        }, null)

        test({
            perp.transfer("test memo", TransferInputField.MEMO)
        }, null)

        test(
            {
                perp.transfer("1000.0", TransferInputField.usdcSize)
            },
            """
                {
                    "input": {
                        "transfer": {
                            "type": "TRANSFER_OUT",
                            "memo": "test memo",
                            "size": {
                                "usdcSize": 1000.0
                            },
                            "summary": {
                                "usdcSize": 1000.0,
                                "filled": true
                            },
                            "options": {
                                "needsSize": true
                            }
                        }
                    },
                    "wallet": {
                        "account": {
                            "subaccounts": {
                                "0": {
                                    "equity": {
                                        "current": 108116.7318528828,
                                        "postOrder": 107116.7318528828
                                    },
                                    "freeCollateral": {
                                        "current": 106640.3767269893,
                                        "postOrder": 105640.3767269893
                                    },
                                    "quoteBalance": {
                                        "current": 99872.368956,
                                        "postOrder": 98872.368956
                                    },
                                    "leverage": {
                                        "current": 0.2731039128897115,
                                        "postOrder": 0.2756535044256519
                                    },
                                    "marginUsage": {
                                        "current": 0.013655195644485585,
                                        "postOrder": 0.013782675221282625
                                    },
                                    "buyingPower": {
                                        "current": 2132807.5345397857,
                                        "postOrder": 2112807.5345397857
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
            { response ->
                assertTrue { response.state?.input?.transfer?.transferOutOptions?.assets?.count() == 2 }
                assertTrue { response.state?.input?.transfer?.transferOutOptions?.chains?.count() == 1 }
            },
        )
    }

    private fun testTransferInputTypeChange() {
        test(
            {
                perp.transfer("DEPOSIT", TransferInputField.type)
            },
            """
        {
            "input": {
                "transfer": {
                    "type": "DEPOSIT",
                    "memo": null
                }
            }
        }
            """.trimIndent(),
        )

        test(
            {
                perp.transfer("TRANSFER_OUT", TransferInputField.type)
            },
            """
    {
        "input": {
            "transfer": {
                "type": "TRANSFER_OUT",
                "memo": null
            }
        }
    }
            """.trimIndent(),
        )

        test(
            {
                perp.transfer("test memo", TransferInputField.MEMO)
            },
            """
        {
            "input": {
                "transfer": {
                    "type": "TRANSFER_OUT",
                    "memo": "test memo"
                }
            }
        }
            """.trimIndent(),
        )

        test(
            {
                perp.transfer("WITHDRAWAL", TransferInputField.type)
            },
            """
    {
        "input": {
            "transfer": {
                "type": "WITHDRAWAL",
                "memo": null
            }
        }
    }
            """.trimIndent(),
        )
    }
}
