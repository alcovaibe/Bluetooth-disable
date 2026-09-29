package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private val engine = CalculatorEngine()

    @Test fun addition() = assertValue("2 + 2", "4")
    @Test fun subtraction() = assertValue("10 - 3", "7")
    @Test fun multiplication() = assertValue("8 × 4", "32")
    @Test fun division() = assertValue("20 ÷ 5", "4")
    @Test fun precedence() = assertValue("2 + 3 × 4", "14")
    @Test fun parentheses() = assertValue("(2 + 3) × 4", "20")
    @Test fun unaryMinus() = assertValue("-5 + 7", "2")
    @Test fun unaryMinusAfterMultiply() = assertValue("2 × -3", "-6")
    @Test fun unaryMinusInParentheses() = assertValue("(-5 + 2) × 3", "-9")
    @Test fun decimalPrecision() = assertValue("0.1 + 0.2", "0.3")
    @Test fun addPercent() = assertValue("200 + 10%", "220")
    @Test fun subtractPercent() = assertValue("200 - 10%", "180")
    @Test fun multiplyPercent() = assertValue("200 × 10%", "20")
    @Test fun dividePercent() = assertValue("200 ÷ 10%", "2000")
    @Test fun standalonePercent() = assertValue("50%", "0.5")
    @Test fun addTwentyFivePercent() = assertValue("100 + 25%", "125")
    @Test fun subtractTwentyFivePercent() = assertValue("80 - 25%", "60")
    @Test fun multiplyTwentyPercent() = assertValue("50 × 20%", "10")
    @Test fun divideTwentyPercent() = assertValue("10 ÷ 20%", "50")

    @Test
    fun divisionByZeroIsControlled() {
        assertEquals(
            CalculatorEvaluation.Failure(CalculatorEngineError.DIVISION_BY_ZERO),
            engine.evaluate("1 ÷ 0"),
        )
    }

    @Test
    fun largeIntegerMultiplicationIsExact() {
        assertValue(
            "999999999999999999 × 999999999999999999",
            "999999999999999998000000000000000001",
        )
    }

    @Test
    fun malformedExpressionsNeverEscapeAsExceptions() {
        listOf("", "2 + × 3", "(2 + 3", "1.2.3", ")1(", "%", "2 ÷")
            .forEach { expression ->
                val result = engine.evaluate(expression)
                assertTrue(result is CalculatorEvaluation.Failure)
            }
    }

    private fun assertValue(expression: String, expected: String) {
        val result = engine.evaluate(expression)
        assertTrue(result is CalculatorEvaluation.Success)
        val value = (result as CalculatorEvaluation.Success).value
        assertEquals(0, value.compareTo(BigDecimal(expected)))
        assertEquals(expected, CalculatorFormatter.format(value))
    }
}
