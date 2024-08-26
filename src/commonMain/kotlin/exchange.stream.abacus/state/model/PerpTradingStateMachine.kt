package exchange.stream.abacus.state.model

import exchange.stream.abacus.protocols.LocalizerProtocol
import exchange.stream.abacus.state.app.helper.Formatter
import exchange.stream.abacus.state.manager.V4Environment

class PerpTradingStateMachine(
    environment: V4Environment?,
    localizer: LocalizerProtocol?,
    formatter: Formatter?,
    maxSubaccountNumber: Int,
    useParentSubaccount: Boolean,
    staticTyping: Boolean = false,
) :
    TradingStateMachine(environment, localizer, formatter, maxSubaccountNumber, useParentSubaccount, staticTyping) {
    /*
    Placeholder for now. Eventually, the code specifically for Perpetual will be in this class
     */
}
