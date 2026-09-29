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

class CalculatorEngine {
    fun evaluate(expression: String): CalculatorEvaluation {
        if (expression.isBlank()) {
            return CalculatorEvaluation.Failure(CalculatorEngineError.INVALID_EXPRESSION)
        }

        return try {
            val tokens = CalculatorTokenizer.tokenize(expression)
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
}
