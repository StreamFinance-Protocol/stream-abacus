package exchange.stream.abacus.output.account

import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.protocols.ParserProtocol
import exchange.stream.abacus.state.internalstate.InternalAccountState
import exchange.stream.abacus.state.manager.TokenInfo
import exchange.stream.abacus.utils.IMap
import exchange.stream.abacus.utils.IMutableMap
import exchange.stream.abacus.utils.Logger
import kollections.JsExport
import kollections.iMapOf
import kollections.iMutableMapOf
import kotlinx.serialization.Serializable

@Suppress("UNCHECKED_CAST")
@JsExport
@Serializable
data class Account(
    var balances: IMap<String, AccountBalance>?,
    var subaccounts: IMap<String, Subaccount>?,
    var groupedSubaccounts: IMap<String, Subaccount>?,
) {
    companion object {
        internal fun create(
            existing: Account?,
            parser: ParserProtocol,
            data: Map<String, Any>,
            tokensInfo: Map<String, TokenInfo>,
            localizer: LocalizerProtocol?,
            staticTyping: Boolean,
            internalState: InternalAccountState,
        ): Account {
            Logger.d { "creating Account\n" }

            val balances: IMutableMap<String, AccountBalance> =
                iMutableMapOf()
            if (staticTyping) {
                internalState.balances?.forEach { (key, value) ->
                    AccountBalance.create(
                        existing = existing?.balances?.get(key),
                        parser = parser,
                        data = emptyMap(),
                        decimals = findTokenInfo(tokensInfo, key)?.decimals ?: 0,
                        internalState = value,
                    )?.let { balance ->
                        balances[key] = balance
                    }
                }
            } else {
                val balancesData = parser.asMap(data["balances"])
                if (balancesData != null) {
                    for ((key, value) in balancesData) {
                        val balanceData = parser.asMap(value) ?: iMapOf()
                        // key is the denom
                        val tokenInfo = findTokenInfo(tokensInfo, key)
                        if (tokenInfo != null) {
                            AccountBalance.create(
                                existing = existing?.balances?.get(key),
                                parser = parser,
                                data = balanceData,
                                decimals = tokenInfo.decimals,
                                internalState = internalState.balances?.get(key),
                            )?.let { balance ->
                                balances[key] = balance
                            }
                        }
                    }
                }
            }

            val subaccounts: IMutableMap<String, Subaccount> =
                iMutableMapOf()

            val subaccountsData = parser.asMap(data["subaccounts"])
            if (subaccountsData != null) {
                for ((key, value) in subaccountsData) {
                    val subaccountData = parser.asMap(value) ?: iMapOf()

                    val subaccountNumber = parser.asInt(key) ?: 0
                    Subaccount.create(
                        existing = existing?.subaccounts?.get(key),
                        parser = parser,
                        data = subaccountData,
                        localizer = localizer,
                        staticTyping = staticTyping,
                        internalState = internalState.subaccounts[subaccountNumber],
                    )
                        ?.let { subaccount ->
                            subaccounts[key] = subaccount
                        }
                }
            }

            val groupedSubaccounts: IMutableMap<String, Subaccount> =
                iMutableMapOf()

            val groupedSubaccountsData = parser.asMap(data["groupedSubaccounts"])
            if (groupedSubaccountsData != null) {
                for ((key, value) in groupedSubaccountsData) {
                    val subaccountData = parser.asMap(value) ?: iMapOf()

                    val subaccountNumber = parser.asInt(key) ?: 0
                    Subaccount.create(
                        existing = existing?.subaccounts?.get(key),
                        parser = parser,
                        data = subaccountData,
                        localizer = localizer,
                        staticTyping = staticTyping,
                        internalState = internalState.subaccounts[subaccountNumber],
                    )
                        ?.let { subaccount ->
                            groupedSubaccounts[key] = subaccount
                        }
                }
            }

            return Account(
                balances = balances,
                subaccounts = subaccounts,
                groupedSubaccounts = groupedSubaccounts,
            )
        }

        private fun findTokenInfo(tokensInfo: Map<String, TokenInfo>, denom: String): TokenInfo? {
            return tokensInfo.firstNotNullOfOrNull { if (it.value.denom == denom) it.value else null }
        }
    }
}
