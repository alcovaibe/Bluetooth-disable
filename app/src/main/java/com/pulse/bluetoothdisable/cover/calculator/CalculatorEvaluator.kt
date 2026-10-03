package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal
import java.math.MathContext

internal class CalculatorDivisionByZeroException : ArithmeticException()

internal object CalculatorEvaluator {
    private val hundred = BigDecimal("100")

    fun evaluate(node: CalculatorNode): BigDecimal = when (node) {
        is NumberNode -> node.value
        is UnaryMinusNode -> evaluate(node.operand).negate()
        is PercentNode -> evaluate(node.operand).divide(hundred)
        is BinaryNode -> evaluateBinary(node)
    }

    fun applyBinary(
        left: BigDecimal,
        operator: BinaryOperator,
        right: BigDecimal,
        rightIsPercent: Boolean = false,
    ): BigDecimal {
        if (rightIsPercent &&
            (operator == BinaryOperator.ADD || operator == BinaryOperator.SUBTRACT)
        ) {
            val delta = left.multiply(right)
            return if (operator == BinaryOperator.ADD) {
                left.add(delta)
            } else {
                left.subtract(delta)
            }
        }

        return when (operator) {
            BinaryOperator.ADD -> left.add(right)
            BinaryOperator.SUBTRACT -> left.subtract(right)
            BinaryOperator.MULTIPLY -> left.multiply(right)
            BinaryOperator.DIVIDE -> divide(left, right)
        }
    }

    private fun evaluateBinary(node: BinaryNode): BigDecimal {
        val left = evaluate(node.left)
        val right = evaluate(node.right)
        return applyBinary(
            left = left,
            operator = node.operator,
            right = right,
            rightIsPercent = node.right is PercentNode,
        )
    }

    private fun divide(left: BigDecimal, right: BigDecimal): BigDecimal {
        if (right.compareTo(BigDecimal.ZERO) == 0) {
            throw CalculatorDivisionByZeroException()
        }
        return try {
            left.divide(right)
        } catch (_: ArithmeticException) {
            left.divide(right, MathContext.DECIMAL128)
        }
    }
}
