package exchange.stream.abacus.state.internalstate

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import exchange.stream.abacus.output.Asset
import exchange.stream.abacus.output.account.SubaccountFill
import exchange.stream.abacus.output.account.SubaccountHistoricalPNL
import exchange.stream.abacus.output.account.SubaccountOrder
import exchange.stream.abacus.output.account.SubaccountPositionResources
import exchange.stream.abacus.output.input.MarginMode
import exchange.stream.abacus.utils.NUM_PARENT_SUBACCOUNTS
import indexer.codegen.IndexerPerpetualPositionStatus
import indexer.codegen.IndexerPositionSide
import kotlinx.datetime.Instant

internal data class InternalState(
    var assets: MutableMap<String, Asset> = mutableMapOf(),
    val transfer: InternalTransferInputState = InternalTransferInputState(),
    val wallet: InternalWalletState = InternalWalletState(),
)

internal data class InternalWalletState(
    var account: InternalAccountState = InternalAccountState(),
    var user: InternalUserState? = null,
    var walletAddress: String? = null,
)

internal data class InternalUserState(
    var feeTierId: String? = null,
    var makerFeeRate: Double? = null,
    var takerFeeRate: Double? = null,
    var makerVolume30D: Double? = null,
    var takerVolume30D: Double? = null,
)

internal data class InternalAccountState(
    // token denom -> balance
    var balances: Map<String, InternalAccountBalanceState>? = null,

    // subaccount number -> subaccount state
    var subaccounts: MutableMap<Int, InternalSubaccountState> = mutableMapOf(),

    // subaccount number -> subaccount state
    var groupedSubaccounts: MutableMap<Int, InternalSubaccountState> = mutableMapOf(),
)

internal data class InternalSubaccountState(
    var fills: List<SubaccountFill>? = null,
    var orders: List<SubaccountOrder>? = null,
    var historicalPNLs: List<SubaccountHistoricalPNL>? = null,
    var positions: Map<String, InternalPerpetualPosition>? = null,
    var subaccountNumber: Int,
    var address: String? = null,
    var equity: String? = null,
    var freeCollateral: String? = null,
    var marginEnabled: Boolean? = null,
    var updatedAtHeight: String? = null,
    var latestProcessedBlockHeight: String? = null
) {
    val openPositions: Map<String, InternalPerpetualPosition>?
        get() {
            return positions?.filterValues { it.status == IndexerPerpetualPositionStatus.OPEN }
        }
}

internal data class InternalPerpetualPosition(
    val market: String? = null,
    val status: IndexerPerpetualPositionStatus? = null,
    val side: IndexerPositionSide? = null,
    val size: Double? = null,
    val maxSize: Double? = null,
    val entryPrice: Double? = null,
    val realizedPnl: Double? = null,
    val createdAt: Instant? = null,
    val createdAtHeight: Double? = null,
    val sumOpen: Double? = null,
    val sumClose: Double? = null,
    val netFunding: Double? = null,
    val unrealizedPnl: Double? = null,
    val closedAt: Instant? = null,
    val exitPrice: Double? = null,
    val subaccountNumber: Int? = null,
    val resources: SubaccountPositionResources? = null,
) {
    val marginMode: MarginMode?
        get() {
            return if (subaccountNumber != null) {
                if (subaccountNumber >= NUM_PARENT_SUBACCOUNTS) {
                    MarginMode.Cross
                } else {
                    MarginMode.Isolated
                }
            } else {
                null
            }
        }
}

internal data class InternalAccountBalanceState(
    val denom: String,
    val amount: BigDecimal,
)
