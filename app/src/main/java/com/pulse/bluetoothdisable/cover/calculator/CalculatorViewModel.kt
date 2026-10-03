package com.pulse.bluetoothdisable.cover.calculator

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

data class CalculatorUiState(
    val expression: String = "",
    val previousExpression: String = "",
    val display: String = "0",
    val error: CalculatorEngineError? = null,
    val afterResult: Boolean = false,
    val history: List<CalculatorHistoryEntry> = emptyList(),
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = CalculatorEngine()
    private val historyStore = CalculatorHistoryStore(application)
    private val accessCodeManager = CalculatorAccessCodeManager(application)
    private var repeatOperation: CalculatorRepeatOperation? = null

    var uiState by mutableStateOf(
        CalculatorUiState(history = historyStore.entries()),
    )
        private set

    fun inputDigit(digit: Char) {
        if (!digit.isDigit()) return
        val base = uiState.expression
        updateExpression(base + digit, clearPrevious = uiState.afterResult)
    }

    fun inputDecimal() {
        var base = uiState.expression
        val currentNumber = base.takeLastWhile { it.isDigit() || it == '.' }
        if (currentNumber.contains('.')) return

        base += if (base.isEmpty() || isBinaryOperator(base.last()) || base.last() == '(') {
            "0."
        } else if (base.last().isDigit()) {
            "."
        } else {
            return
        }
        updateExpression(base, clearPrevious = uiState.afterResult)
    }

    fun inputOperator(operator: Char) {
        if (operator !in charArrayOf('+', '-', '×', '÷')) return
        var expression = uiState.expression

        if (uiState.afterResult) {
            expression += operator
            updateExpression(expression, clearPrevious = true)
            return
        }

        if (expression.isEmpty()) {
            if (operator == '-') updateExpression("-", clearPrevious = false)
            return
        }

        val last = expression.last()
        when {
            last == '(' -> {
                if (operator == '-') updateExpression(expression + operator, clearPrevious = false)
            }
            isBinaryOperator(last) -> {
                val previous = expression.getOrNull(expression.lastIndex - 1)
                val lastMinusIsUnary = last == '-' &&
                    (previous == null || previous == '(' || isBinaryOperator(previous))

                when {
                    lastMinusIsUnary && previous != null && isBinaryOperator(previous) &&
                        operator != '-' -> {
                        updateExpression(
                            expression.dropLast(2) + operator,
                            clearPrevious = false,
                        )
                    }
                    lastMinusIsUnary -> Unit
                    operator == '-' ->
                        updateExpression(expression + operator, clearPrevious = false)
                    else ->
                        updateExpression(expression.dropLast(1) + operator, clearPrevious = false)
                }
            }
            last == '.' -> updateExpression(expression + "0" + operator, clearPrevious = false)
            last.isDigit() || last == ')' || last == '%' ->
                updateExpression(expression + operator, clearPrevious = false)
        }
    }

    fun inputPercent() {
        if (uiState.afterResult) return
        val expression = uiState.expression
        if (expression.isNotEmpty() &&
            (expression.last().isDigit() || expression.last() == ')')
        ) {
            updateExpression("$expression%", clearPrevious = false)
        }
    }

    fun inputParenthesis() {
        if (uiState.afterResult) {
            updateExpression("(", clearPrevious = true)
            return
        }

        val expression = uiState.expression
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val last = expression.lastOrNull()

        when {
            expression.isEmpty() || last == '(' || (last != null && isBinaryOperator(last)) ->
                updateExpression("$expression(", clearPrevious = false)
            openCount > closeCount && last != null &&
                (last.isDigit() || last == ')' || last == '%') ->
                updateExpression("$expression)", clearPrevious = false)
        }
    }

    fun backspace() {
        val source = uiState.expression
        if (source.isEmpty()) return
        repeatOperation = null
        val updated = source.dropLast(1)
        uiState = uiState.copy(
            expression = updated,
            previousExpression = if (uiState.afterResult) "" else uiState.previousExpression,
            display = updated.ifEmpty { "0" },
            error = null,
            afterResult = false,
        )
    }

    fun clear() {
        repeatOperation = null
        uiState = CalculatorUiState(history = uiState.history)
    }

    fun clearHistory() {
        historyStore.clear()
        uiState = uiState.copy(history = emptyList())
    }

    fun equalsPressed(): Boolean {
        val raw = uiState.expression
        val directAccessCandidate = !uiState.afterResult && CalculatorAccessCodePolicy.isValid(raw)

        if (directAccessCandidate && accessCodeManager.verify(raw)) {
            clear()
            return true
        }

        if (uiState.afterResult) {
            val operation = repeatOperation ?: return false
            val current = when (val evaluation = engine.evaluate(uiState.expression)) {
                is CalculatorEvaluation.Success -> evaluation.value
                is CalculatorEvaluation.Failure -> {
                    repeatOperation = null
                    uiState = uiState.copy(error = evaluation.error)
                    return false
                }
            }
            val repeatedExpression = uiState.expression + operation.expressionSuffix()
            applyEvaluation(
                expression = repeatedExpression,
                evaluation = engine.applyRepeat(current, operation),
                keepRepeatOperation = true,
            )
            return false
        }

        val normalized = try {
            engine.normalizeForEvaluation(raw)
        } catch (_: CalculatorParseException) {
            raw
        }
        val operation = engine.repeatOperation(normalized)
        repeatOperation = operation
        applyEvaluation(
            expression = normalized,
            evaluation = engine.evaluate(normalized),
            keepRepeatOperation = operation != null,
        )
        return false
    }

    private fun applyEvaluation(
        expression: String,
        evaluation: CalculatorEvaluation,
        keepRepeatOperation: Boolean,
    ) {
        when (evaluation) {
            is CalculatorEvaluation.Success -> {
                val resultExpression = CalculatorFormatter.toExpression(evaluation.value)
                val resultDisplay = CalculatorFormatter.format(evaluation.value)
                historyStore.add(expression, resultDisplay)
                uiState = uiState.copy(
                    expression = resultExpression,
                    previousExpression = "$expression =",
                    display = resultDisplay,
                    error = null,
                    afterResult = true,
                    history = historyStore.entries(),
                )
                if (!keepRepeatOperation) repeatOperation = null
            }
            is CalculatorEvaluation.Failure -> {
                repeatOperation = null
                uiState = uiState.copy(error = evaluation.error, afterResult = false)
            }
        }
    }

    private fun updateExpression(value: String, clearPrevious: Boolean) {
        repeatOperation = null
        uiState = uiState.copy(
            expression = value,
            previousExpression = if (clearPrevious) "" else uiState.previousExpression,
            display = value.ifEmpty { "0" },
            error = null,
            afterResult = false,
        )
    }

    private fun isBinaryOperator(char: Char): Boolean =
        char == '+' || char == '-' || char == '×' || char == '÷'
}
