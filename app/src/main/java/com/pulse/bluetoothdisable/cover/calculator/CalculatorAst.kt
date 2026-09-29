package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal

internal sealed interface CalculatorNode

internal data class NumberNode(val value: BigDecimal) : CalculatorNode
internal data class UnaryMinusNode(val operand: CalculatorNode) : CalculatorNode
internal data class PercentNode(val operand: CalculatorNode) : CalculatorNode
internal data class BinaryNode(
    val left: CalculatorNode,
    val operator: BinaryOperator,
    val right: CalculatorNode,
) : CalculatorNode

internal enum class BinaryOperator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
}
