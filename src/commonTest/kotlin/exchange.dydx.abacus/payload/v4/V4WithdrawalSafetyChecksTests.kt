package exchange.stream.abacus.payload.v4

import exchange.stream.abacus.output.input.TransferType
import exchange.stream.abacus.responses.ParsingError
import exchange.stream.abacus.responses.ParsingException
import exchange.stream.abacus.responses.StateResponse
import exchange.stream.abacus.state.changes.StateChanges
import exchange.stream.abacus.state.model.TradingStateMachine
import exchange.stream.abacus.state.model.TransferInputField
import exchange.stream.abacus.state.model.onChainWithdrawalCapacity
import exchange.stream.abacus.state.model.onChainWithdrawalGating
import exchange.stream.abacus.state.model.transfer
import exchange.stream.abacus.tests.extensions.loadAccounts
import kollections.iListOf
import kotlin.test.Test

class V4WithdrawalSafetyChecksTests : V4BaseTests() {

    override fun setup() {
        perp.loadAccounts(mock)
        perp.currentBlockAndHeight = mock.heightMock.currentBlockAndHeight
        perp.transfer(TransferType.deposit.rawValue, TransferInputField.type)
    }

    @Test
    fun testGating() {
        setup()
        test(
            {
                perp.parseOnChainWithdrawalGating(mock.v4WithdrawalSafetyChecksMock.withdrawal_and_transfer_gating_status_data)
            },
            """
            {
                "configs": {
                    "withdrawalGating": {
                        "negativeTncSubaccountSeenAtBlock" : 8521777,
                        "chainOutageSeenAtBlock" : 8489769,
                        "withdrawalsAndTransfersUnblockedAtBlock" : 16750
                    }
                }
            }
            """.trimIndent(),
        )
        perp.currentBlockAndHeight = mock.heightMock.beforeCurrentBlockAndHeight
        perp.transfer(TransferType.withdrawal.rawValue, TransferInputField.type)
        test(
            {
                perp.transfer("1235.0", TransferInputField.usdcSize)
            },
            """
            {
                "configs": {
                    "withdrawalGating": {
                        "negativeTncSubaccountSeenAtBlock" : 8521777,
                        "chainOutageSeenAtBlock" : 8489769,
                        "withdrawalsAndTransfersUnblockedAtBlock" : 16750
                    }
                },
                "input": {
                    "errors": [
                        {
                            "type": "ERROR",
                            "code": "",
                            "linkText": "APP.GENERAL.LEARN_MORE_ARROW",
                            "resources": {
                                "title": {
                                    "stringKey": "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_PAUSED_TITLE"
                                },
                                "text": {
                                    "stringKey": "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_PAUSED_DESCRIPTION",
                                    "params": [
                                        {
                                            "value": 1.0,
                                            "format": "string",
                                            "key": "SECONDS"
                                        }
                                    ]
                                },
                                "action": {
                                    "stringKey": "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_PAUSED_ACTION"
                                }
                            }
                        }
                    ]
                }
            }
            """.trimIndent(),
        )
        perp.currentBlockAndHeight = mock.heightMock.afterCurrentBlockAndHeight
        perp.transfer(TransferType.transferOut.rawValue, TransferInputField.type)
        test(
            {
                perp.transfer("1235.0", TransferInputField.usdcSize)
            },
            """
            {
                "configs": {
                    "withdrawalGating": {
                        "negativeTncSubaccountSeenAtBlock" : 8521777,
                        "chainOutageSeenAtBlock" : 8489769,
                        "withdrawalsAndTransfersUnblockedAtBlock" : 16750
                    }
                },
                "input": {
                    "errors": [
                        {
                            "type": "REQUIRED",
                            "code": "REQUIRED_ADDRESS",
                            "fields": [
                                "address"
                            ],
                            "resources": {
                                "action": {
                                    "stringKey": "APP.DIRECT_TRANSFER_MODAL.ENTER_ETH_ADDRESS"
                                }
                            }
                        }
                    ]
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testCapacity() {
        setup()
        perp.transfer("WITHDRAWAL", TransferInputField.type)
        perp.transfer("1235.0", TransferInputField.usdcSize)
        test(
            {
                perp.parseOnChainWithdrawalCapacity(mock.v4WithdrawalSafetyChecksMock.withdrawal_capacity_by_denom_data_daily_less_than_weekly)
            },
            """
            {
                "input": {
                    "errors": [
                        {
                            "type": "ERROR",
                            "code": "",
                            "linkText": "APP.GENERAL.LEARN_MORE_ARROW",
                            "resources": {
                                "title": {
                                    "stringKey": "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_LIMIT_OVER_TITLE"
                                },
                                "text": {
                                    "stringKey": "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_LIMIT_OVER_DESCRIPTION",
                                    "params": [
                                        {
                                            "value": 1234.567891,
                                            "format": "price",
                                            "key": "USDC_LIMIT"
                                        }
                                    ]
                                },
                                "action": {
                                    "stringKey": "WARNINGS.ACCOUNT_FUND_MANAGEMENT.WITHDRAWAL_LIMIT_OVER_ACTION"
                                }
                            }
                        }
                    ]
                },
                "configs": {
                    "withdrawalCapacity": {
                        "limiterCapacityList": [
                            {
                                "seconds": "3600",
                                "capacity": "1234567891",
                                "baselineMinimum": "1000000000000",
                                "nanos": 0.0,
                                "baselineTvlPpm": 10000.0
                            },
                            {
                                "seconds": "86400",
                                "capacity": "1234567892",
                                "baselineMinimum": "10000000000000",
                                "nanos": 0.0,
                                "baselineTvlPpm": 100000.0
                            }
                        ],
                        "maxWithdrawalCapacity": "1234.567891"
                    }
                }
            }
            """.trimIndent(),
        )
    }
}

private fun TradingStateMachine.parseOnChainWithdrawalCapacity(payload: String): StateResponse {
    var changes: StateChanges? = null
    var error: ParsingError? = null
    try {
        changes = onChainWithdrawalCapacity(payload)
    } catch (e: ParsingException) {
        error = e.toParsingError()
    }
    if (changes != null) {
        update(changes)
    }

    val errors = if (error != null) iListOf(error) else null
    return StateResponse(state, changes, errors)
}

private fun TradingStateMachine.parseOnChainWithdrawalGating(payload: String): StateResponse {
    var changes: StateChanges? = null
    var error: ParsingError? = null
    try {
        changes = onChainWithdrawalGating(payload)
    } catch (e: ParsingException) {
        error = e.toParsingError()
    }
    if (changes != null) {
        update(changes)
    }

    val errors = if (error != null) iListOf(error) else null
    return StateResponse(state, changes, errors)
}
