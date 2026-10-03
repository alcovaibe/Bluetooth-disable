package com.pulse.bluetoothdisable.cover.calculator

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorViewModelInstrumentedTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val history = CalculatorHistoryStore(application)
    private val access = CalculatorAccessCodeManager(application)

    @Before
    fun before() {
        history.clear()
        access.clearCode()
    }

    @After
    fun after() {
        history.clear()
        access.clearCode()
    }

    @Test
    fun digitAfterResultAppendsToDisplayedResult() {
        val viewModel = CalculatorViewModel(application)
        enter(viewModel, "5+2")
        assertFalse(viewModel.equalsPressed())
        assertEquals("7", viewModel.uiState.display)

        viewModel.inputDigit('3')

        assertEquals("73", viewModel.uiState.expression)
        assertEquals("73", viewModel.uiState.display)
        assertEquals("", viewModel.uiState.previousExpression)
        assertFalse(viewModel.uiState.afterResult)
    }

    @Test
    fun operatorAfterResultContinuesFromResult() {
        val viewModel = CalculatorViewModel(application)
        enter(viewModel, "5+2")
        viewModel.equalsPressed()

        viewModel.inputOperator('×')
        viewModel.inputDigit('3')
        viewModel.equalsPressed()

        assertEquals("21", viewModel.uiState.display)
    }

    @Test
    fun repeatedEqualsRepeatsLastBinaryOperation() {
        val viewModel = CalculatorViewModel(application)
        enter(viewModel, "5+2")

        viewModel.equalsPressed()
        assertEquals("7", viewModel.uiState.display)
        viewModel.equalsPressed()
        assertEquals("9", viewModel.uiState.display)
        viewModel.equalsPressed()
        assertEquals("11", viewModel.uiState.display)
    }

    @Test
    fun computedAccessCodeValueDoesNotUnlockCover() {
        access.setCode("58317")
        val viewModel = CalculatorViewModel(application)
        enter(viewModel, "58317×1")

        assertFalse(viewModel.equalsPressed())
        assertEquals("58317", viewModel.uiState.display)
        assertFalse(viewModel.equalsPressed())
        assertEquals("58317", viewModel.uiState.display)
    }

    @Test
    fun wrongFiveDigitCandidateIsRecordedAsNormalCalculation() {
        access.setCode("58317")
        val viewModel = CalculatorViewModel(application)
        enter(viewModel, "12345")

        assertFalse(viewModel.equalsPressed())

        assertEquals(
            listOf(CalculatorHistoryEntry("12345", "12345")),
            CalculatorHistoryStore(application).entries(),
        )
    }

    @Test
    fun clearDropsExpressionButPreservesHistory() {
        val viewModel = CalculatorViewModel(application)
        enter(viewModel, "1+1")
        viewModel.equalsPressed()
        viewModel.inputDigit('3')

        viewModel.clear()

        assertEquals("", viewModel.uiState.expression)
        assertEquals("0", viewModel.uiState.display)
        assertEquals(listOf(CalculatorHistoryEntry("1+1", "2")), viewModel.uiState.history)
    }

    private fun enter(viewModel: CalculatorViewModel, expression: String) {
        expression.forEach { char ->
            when (char) {
                in '0'..'9' -> viewModel.inputDigit(char)
                '+' -> viewModel.inputOperator('+')
                '-' -> viewModel.inputOperator('-')
                '×' -> viewModel.inputOperator('×')
                '÷' -> viewModel.inputOperator('÷')
                '.' -> viewModel.inputDecimal()
                '%' -> viewModel.inputPercent()
                '(', ')' -> viewModel.inputParenthesis()
            }
        }
    }
}
