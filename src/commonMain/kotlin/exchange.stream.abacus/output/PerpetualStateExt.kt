package exchange.stream.abacus.output

import exchange.stream.abacus.output.account.PositionSide
import exchange.stream.abacus.output.input.OrderSide

fun OrderSide.isOppositeOf(that: PositionSide): Boolean =
    (this == OrderSide.Buy && that == PositionSide.SHORT) || (this == OrderSide.Sell && that == PositionSide.LONG)
