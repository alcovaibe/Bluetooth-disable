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
    val right: CalculatorNode,
)

class CalculatorEngine {
    fun evaluate(expression: String): CalculatorEvaluation {
        if (expression.isBlank()) {
            return CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
        }

        return evaluateSafely {
            val ast = parse(expression)
            CalculatorEvaluator.evaluate(ast)
        }
    }

    internal fun repeatOperation(expression: String): CalculatorRepeatOperation? {
        if (expression.isBlank()) return null

        return try {
            val ast = parse(expression) as? BinaryNode ?: return null
            CalculatorRepeatOperation(
                operator = ast.operator,
                right = ast.right,
            )
        } catch (_: CalculatorParseException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }

    internal fun evaluateRepeat(
        currentValue: BigDecimal,
        operation: CalculatorRepeatOperation,
    ): CalculatorEvaluation = evaluateSafely {
        CalculatorEvaluator.evaluate(
            BinaryNode(
                left = NumberNode(currentValue),
                operator = operation.operator,
                right = operation.right,
            ),
        )
    }

    internal fun repeatExpression(
        currentValue: String,
        operation: CalculatorRepeatOperation,
    ): String = buildString {
        append(currentValue)
        append(operatorSymbol(operation.operator))
        append(render(operation.right))
    }

    private fun parse(expression: String): CalculatorNode =
        CalculatorParser(CalculatorTokenizer.tokenize(expression)).parse()

    private inline fun evaluateSafely(block: () -> BigDecimal): CalculatorEvaluation = try {
        CalculatorEvaluation.Success(block())
    } catch (_: CalculatorDivisionByZeroException) {
        CalculatorEvaluation.Failure(CalculatorEngineError.DIVISION_BY_ZERO)
    } catch (_: CalculatorParseException) {
        CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
    } catch (_: ArithmeticException) {
        CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
    }

    private fun render(node: CalculatorNode): String = when (node) {
        is NumberNode -> CalculatorFormatter.format(node.value)
        is UnaryMinusNode -> "-(${render(node.operand)})"
        is PercentNode -> {
            val operand = if (node.operand is BinaryNode) {
                "(${render(node.operand)})"
            } else {
                render(node.operand)
            }
            "$operand%"
        }
        is BinaryNode -> "(${render(node.left)}${operatorSymbol(node.operator)}${render(node.right)})"
    }

    private fun operatorSymbol(operator: BinaryOperator): Char = when (operator) {
        BinaryOperator.ADD -> '+'
        BinaryOperator.SUBTRACT -> '-'
        BinaryOperator.MULTIPLY -> '×'
        BinaryOperator.DIVIDE -> '÷'
    }
}
