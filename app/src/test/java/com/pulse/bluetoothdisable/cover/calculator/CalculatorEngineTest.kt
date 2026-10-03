package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private val engine = CalculatorEngine()

    @Test fun addition() = assertValue("2 + 2", "4")
    @Test fun subtraction() = assertValue("10 - 3", "7")
    @Test fun multiplication() = assertValue("8 × 4", "32")
    @Test fun division() = assertValue("20 ÷ 5", "4")
    @Test fun precedence() = assertValue("2 + 3 × 4", "14")
    @Test fun subtractionPrecedence() = assertValue("10 - 2 × 3", "4")
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
    @Test fun addFivePercent() = assertValue("1000 + 5%", "1050")
    @Test fun multiplyTwentyPercent() = assertValue("50 × 20%", "10")
    @Test fun divideTwentyPercent() = assertValue("10 ÷ 20%", "50")

    @Test fun repeatedAddition() = assertRepeated("5 + 2", "7", "9")
    @Test fun repeatedSubtraction() = assertRepeated("10 - 3", "7", "4")
    @Test fun repeatedMultiplication() = assertRepeated("4 × 3", "12", "36")
    @Test fun repeatedDivision() = assertRepeated("20 ÷ 2", "10", "5")
    @Test fun repeatedPercentUsesCurrentResultAsNewBase() = assertRepeated("200 + 10%", "220", "242")

    @Test
    fun scientificDisplayAvoidsENotationAndUsesDecimalComma() {
        assertEquals("1 × 10^100", CalculatorFormatter.formatDisplay(BigDecimal("1E+100")))
        assertEquals("1 × 10^-100", CalculatorFormatter.formatDisplay(BigDecimal("1E-100")))
        assertEquals("1,25", CalculatorFormatter.formatDisplay(BigDecimal("1.25")))
        assertTrue(!CalculatorFormatter.format(BigDecimal("1E+100")).contains('E'))
    }

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

    private fun assertRepeated(expression: String, current: String, expected: String) {
        val operation = engine.repeatOperation(expression)
        assertNotNull(operation)
        val result = engine.evaluateRepeat(BigDecimal(current), operation!!)
        assertTrue(result is CalculatorEvaluation.Success)
        assertEquals(
            0,
            (result as CalculatorEvaluation.Success).value.compareTo(BigDecimal(expected)),
        )
    }

    private fun assertValue(expression: String, expected: String) {
        val result = engine.evaluate(expression)
        assertTrue(result is CalculatorEvaluation.Success)
        val value = (result as CalculatorEvaluation.Success).value
        assertEquals(0, value.compareTo(BigDecimal(expected)))
        assertEquals(expected, CalculatorFormatter.format(value))
    }
}
