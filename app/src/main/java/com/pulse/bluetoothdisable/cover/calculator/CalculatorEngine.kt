package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal

sealed interface CalculatorEvaluation {
    data class Success(val value: BigDecimal) : CalculatorEvaluation
    data class Failure(val error: CalculatorEngineError) : CalculatorEvaluation
}

enum class CalculatorEngineError {
    DIVISION_BY_ZERO,
    INVALID_EXPRESSION,
}

internal data class CalculatorRepeatOperation(
    val operator: BinaryOperator,
    val operand: BigDecimal,
    val operandIsPercent: Boolean,
) {
    fun expressionSuffix(): String {
        val symbol = when (operator) {
            BinaryOperator.ADD -> "+"
            BinaryOperator.SUBTRACT -> "-"
            BinaryOperator.MULTIPLY -> "×"
            BinaryOperator.DIVIDE -> "÷"
        }
        val operandText = CalculatorFormatter.toExpression(
            if (operandIsPercent) operand.multiply(BigDecimal("100")) else operand,
        )
        return "$symbol$operandText${if (operandIsPercent) "%" else ""}"
    }
}

class CalculatorEngine {
    fun evaluate(expression: String): CalculatorEvaluation {
        if (expression.isBlank()) {
            return CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
        }

        return try {
            val normalized = normalizeForEvaluation(expression)
            val tokens = CalculatorTokenizer.tokenize(normalized)
            val ast = CalculatorParser(tokens).parse()
            CalculatorEvaluation.Success(CalculatorEvaluator.evaluate(ast))
        } catch (_: CalculatorDivisionByZeroException) {
            CalculatorEvaluation.Failure(CalculatorEngineError.DIVISION_BY_ZERO)
        } catch (_: CalculatorParseException) {
            CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
        } catch (_: ArithmeticException) {
            CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
        }
    }

    internal fun repeatOperation(expression: String): CalculatorRepeatOperation? {
        if (expression.isBlank()) return null
        return try {
            val normalized = normalizeForEvaluation(expression)
            val ast = CalculatorParser(CalculatorTokenizer.tokenize(normalized)).parse()
            val binary = rightMostBinary(ast) ?: return null
            CalculatorRepeatOperation(
                operator = binary.operator,
                operand = CalculatorEvaluator.evaluate(binary.right),
                operandIsPercent = binary.right is PercentNode,
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun applyRepeat(
        current: BigDecimal,
        operation: CalculatorRepeatOperation,
    ): CalculatorEvaluation = try {
        CalculatorEvaluation.Success(
            CalculatorEvaluator.applyBinary(
                left = current,
                operator = operation.operator,
                right = operation.operand,
                rightIsPercent = operation.operandIsPercent,
            ),
        )
    } catch (_: CalculatorDivisionByZeroException) {
        CalculatorEvaluation.Failure(CalculatorEngineError.DIVISION_BY_ZERO)
    } catch (_: ArithmeticException) {
        CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
    }

    internal fun normalizeForEvaluation(expression: String): String {
        var balance = 0
        expression.forEach { char ->
            when (char) {
                '(' -> balance++
                ')' -> {
                    balance--
                    if (balance < 0) throw CalculatorParseException()
                }
            }
        }
        return if (balance > 0) expression + ")".repeat(balance) else expression
    }

    private fun rightMostBinary(node: CalculatorNode): BinaryNode? = when (node) {
        is BinaryNode -> rightMostBinary(node.right) ?: node
        is UnaryMinusNode -> rightMostBinary(node.operand)
        is PercentNode -> rightMostBinary(node.operand)
        is NumberNode -> null
    }
}
