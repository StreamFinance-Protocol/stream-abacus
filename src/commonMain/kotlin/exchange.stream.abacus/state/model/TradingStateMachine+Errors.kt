package exchange.stream.abacus.state.model

import exchange.stream.abacus.responses.ParsingError
import exchange.stream.abacus.responses.ParsingErrorType

internal fun TradingStateMachine.cannotModify(typeText: String): ParsingError {
    return ParsingError(
        ParsingErrorType.InvalidInput,
        "$typeText cannot be modified for the selected trade input",
    )
}
