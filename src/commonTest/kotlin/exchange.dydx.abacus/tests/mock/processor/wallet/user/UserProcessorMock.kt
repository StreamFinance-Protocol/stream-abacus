package exchange.stream.abacus.tests.mock.processor.wallet.user

import exchange.stream.abacus.processor.wallet.user.UserProcessorProtocol
import exchange.stream.abacus.state.internalstate.InternalUserState
import indexer.models.chain.OnChainUserFeeTier
import indexer.models.chain.OnChainUserStatsResponse

internal class UserProcessorMock : UserProcessorProtocol {
    var processOnChainUserFeeTierCallCount = 0
    var processOnChainUserFeeTierAction: ((existing: InternalUserState?, payload: OnChainUserFeeTier?) -> InternalUserState)? = null
    var processOnChainUserStatsCallCount = 0
    var processOnChainUserStatsAction: ((existing: InternalUserState?, payload: OnChainUserStatsResponse?) -> InternalUserState)? = null

    override fun processOnChainUserFeeTier(
        existing: InternalUserState?,
        payload: OnChainUserFeeTier?
    ): InternalUserState {
        processOnChainUserFeeTierCallCount++
        return processOnChainUserFeeTierAction?.invoke(existing, payload) ?: InternalUserState()
    }

    override fun processOnChainUserStats(
        existing: InternalUserState?,
        payload: OnChainUserStatsResponse?
    ): InternalUserState {
        processOnChainUserStatsCallCount++
        return processOnChainUserStatsAction?.invoke(existing, payload) ?: InternalUserState()
    }
}
