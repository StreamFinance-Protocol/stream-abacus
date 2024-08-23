package exchange.stream.abacus.state.v2.supervisor

import exchange.stream.abacus.output.input.TransferType
import exchange.stream.abacus.protocols.QueryType
import exchange.stream.abacus.state.model.TradingStateMachine
import exchange.stream.abacus.state.model.onChainEquityTiers
import exchange.stream.abacus.state.model.onChainFeeTiers
import exchange.stream.abacus.state.model.onChainWithdrawalCapacity
import exchange.stream.abacus.state.model.onChainWithdrawalGating
import exchange.stream.abacus.utils.AnalyticsUtils
import exchange.stream.abacus.utils.ServerTime
import exchange.stream.abacus.utils.iMapOf

internal class SystemSupervisor(
    stateMachine: TradingStateMachine,
    helper: NetworkHelper,
    analyticsUtils: AnalyticsUtils,
    internal val configs: SystemConfigs,
) : NetworkSupervisor(stateMachine, helper, analyticsUtils) {
    override fun didSetReadyToConnect(readyToConnect: Boolean) {
        super.didSetReadyToConnect(readyToConnect)
        if (readyToConnect) {
            if (configs.retrieveMarketConfigs) {
                // get from web deployment
                retrieveMarketConfigs()
            }
        }
    }

    override fun didSetIndexerConnected(indexerConnected: Boolean) {
        super.didSetIndexerConnected(indexerConnected)

        if (indexerConnected) {
            if (configs.retrieveServerTime) {
                retrieveServerTime()
            }
        }
    }

    override fun didSetValidatorConnected(validatorConnected: Boolean) {
        super.didSetValidatorConnected(validatorConnected)

        if (validatorConnected) {
            if (configs.retrieveEquityTiers) {
                retrieveEquityTiers()
            }
            if (configs.retrieveFeeTiers) {
                retrieveFeeTiers()
            }
            if (configs.retrieveWithdrawSafetyChecks) {
                stateMachine.state?.input?.transfer?.type?.let { transferType ->
                    retrieveWithdrawSafetyChecks(transferType)
                }
            }
        }
    }

    internal fun didSetTransferType(transferType: TransferType) {
        if (stateMachine.featureFlags.withdrawalSafetyEnabled &&
            configs.retrieveWithdrawSafetyChecks &&
            (transferType == TransferType.withdrawal || transferType == TransferType.transferOut)
        ) {
            retrieveWithdrawSafetyChecks(transferType)
        }
    }

    private fun retrieveServerTime() {
        val url = helper.configs.publicApiUrl("time")
        if (url != null) {
            helper.get(url, null, null) { _, response, httpCode, _ ->
                if (helper.success(httpCode) && response != null) {
                    val json = helper.parser.decodeJsonObject(response)
                    val time = helper.parser.asDatetime(json?.get("time"))
                    if (time != null) {
                        ServerTime.overWrite = time
                    }
                }
            }
        }
    }

    private fun retrieveMarketConfigs() {
        val oldState = stateMachine.state
        val url = helper.configs.configsUrl("markets")
        if (url != null) {
            helper.get(url, null, null) { _, response, httpCode, _ ->
                if (helper.success(httpCode) && response != null) {
                    update(
                        // TODO, subaccountNumber required to refresh
                        stateMachine.configurations(response, null, helper.deploymentUri),
                        oldState,
                    )
                }
            }
        }
    }

    private fun retrieveEquityTiers() {
        helper.getOnChain(QueryType.EquityTiers, null) { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainEquityTiers(response), oldState)
        }
    }

    private fun retrieveFeeTiers() {
        helper.getOnChain(QueryType.FeeTiers, null) { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainFeeTiers(response), oldState)
        }
    }

    fun retrieveWithdrawSafetyChecks(transferType: TransferType) {
        when (transferType) {
            TransferType.withdrawal -> {
                updateWithdrawalCapacity()
                updateWithdrawalGating()
            }

            TransferType.transferOut -> {
                updateWithdrawalGating()
            }

            else -> {
                // do nothing
            }
        }
    }
    private fun updateWithdrawalCapacity() {
        var denom = helper.environment.tokens["usdc"]?.denom
        val params = iMapOf(
            "denom" to denom,
        )
        val paramsInJson = helper.jsonEncoder.encode(params)
        helper.getOnChain(QueryType.GetWithdrawalCapacityByDenom, paramsInJson) { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainWithdrawalCapacity(response), oldState)
        }
    }

    private fun updateWithdrawalGating() {
        helper.getOnChain(QueryType.GetWithdrawalAndTransferGatingStatus, null) { response ->
            val oldState = stateMachine.state
            update(stateMachine.onChainWithdrawalGating(response), oldState)
        }
    }
}
