package com.pulse.bluetoothdisable.cover.calculator

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculatorViewModelInstrumentedTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val history = CalculatorHistoryStore(application)

    @Before fun before() {
        history.clear()
    }

    @After fun after() {
        history.clear()
    }

    @Test
    fun digitAfterResultAppendsToResult() {
        val viewModel = newViewModel()
        enter(viewModel, '5', '+', '2')
        viewModel.equalsPressed()
        assertEquals("7", viewModel.uiState.display)
        assertTrue(viewModel.uiState.afterResult)

        viewModel.inputDigit('3')

        assertEquals("73", viewModel.uiState.expression)
        assertEquals("73", viewModel.uiState.display)
        assertFalse(viewModel.uiState.afterResult)
        assertEquals("", viewModel.uiState.previousExpression)
    }

    @Test
    fun operatorAfterResultContinuesFromResult() {
        val viewModel = newViewModel()
        enter(viewModel, '5', '+', '2')
        viewModel.equalsPressed()

        viewModel.inputOperator('×')
        viewModel.inputDigit('3')
        viewModel.equalsPressed()

        assertEquals("21", viewModel.uiState.expression)
        assertEquals("21", viewModel.uiState.display)
    }

    @Test
    fun repeatedEqualsRepeatsLastBinaryOperation() {
        val viewModel = newViewModel()
        enter(viewModel, '5', '+', '2')
        viewModel.equalsPressed()
        assertEquals("7", viewModel.uiState.display)

        viewModel.equalsPressed()
        assertEquals("9", viewModel.uiState.display)

        viewModel.equalsPressed()
        assertEquals("11", viewModel.uiState.display)
    }

    @Test
    fun equalsAutoClosesMissingRightParentheses() {
        val viewModel = newViewModel()
        viewModel.inputDigit('2')
        viewModel.inputOperator('×')
        viewModel.inputParenthesis()
        viewModel.inputDigit('3')
        viewModel.inputOperator('+')
        viewModel.inputDigit('4')

        viewModel.equalsPressed()

        assertEquals("14", viewModel.uiState.display)
        assertEquals("2×(3+4) =", viewModel.uiState.previousExpression)
        assertEquals(CalculatorHistoryEntry("2×(3+4)", "14"), viewModel.uiState.history.first())
    }

    @Test
    fun divisionByZeroDoesNotEnterHistory() {
        val viewModel = newViewModel()
        enter(viewModel, '1', '÷', '0')

        viewModel.equalsPressed()

        assertEquals(CalculatorEngineError.DIVISION_BY_ZERO, viewModel.uiState.error)
        assertTrue(viewModel.uiState.history.isEmpty())
        assertTrue(history.entries().isEmpty())
    }

    @Test
    fun digitAfterErrorStartsFreshExpression() {
        val viewModel = newViewModel()
        enter(viewModel, '1', '÷', '0')
        viewModel.equalsPressed()
        assertEquals(CalculatorEngineError.DIVISION_BY_ZERO, viewModel.uiState.error)

        viewModel.inputDigit('7')

        assertEquals(null, viewModel.uiState.error)
        assertEquals("7", viewModel.uiState.expression)
        assertEquals("7", viewModel.uiState.display)
        assertEquals("", viewModel.uiState.previousExpression)
    }

    @Test
    fun unfinishedExpressionDoesNotSurviveNewViewModel() {
        val first = newViewModel()
        first.inputDigit('1')
        first.inputDigit('2')
        assertEquals("12", first.uiState.expression)

        val recreated = newViewModel()

        assertEquals("", recreated.uiState.expression)
        assertEquals("0", recreated.uiState.display)
    }

    @Test
    fun historyKeepsNewestFiftyEntries() {
        repeat(55) { index -> history.add("$index+0", index.toString()) }

        val entries = history.entries()

        assertEquals(CalculatorHistoryStore.MAX_ENTRIES, entries.size)
        assertEquals("54+0", entries.first().expression)
        assertEquals("5+0", entries.last().expression)
    }

    private fun newViewModel() = CalculatorViewModel(application)

    private fun enter(viewModel: CalculatorViewModel, left: Char, operator: Char, right: Char) {
        viewModel.inputDigit(left)
        viewModel.inputOperator(operator)
        viewModel.inputDigit(right)
    }
}
