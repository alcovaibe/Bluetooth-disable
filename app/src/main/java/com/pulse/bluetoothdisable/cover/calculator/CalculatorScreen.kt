package com.pulse.bluetoothdisable.cover.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.bluetoothdisable.R
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onUnlock: () -> Unit,
) {
    val state = viewModel.uiState
    val useComma = Locale.getDefault().language == "ru"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CalculatorHistoryDrawer(
                history = state.history,
                useComma = useComma,
                onClear = viewModel::clearHistory,
            )
        },
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.calculator_history),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val previous = CalculatorFormatter.localize(state.previousExpression, useComma)
                val display = CalculatorFormatter.localize(state.display, useComma)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = previous,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = display,
                        fontSize = when {
                            display.length > 34 -> 28.sp
                            display.length > 20 -> 36.sp
                            else -> 52.sp
                        },
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }

                state.error?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                CalculatorEngineError.DIVISION_BY_ZERO ->
                                    R.string.calculator_error_divide_by_zero
                                CalculatorEngineError.INVALID_EXPRESSION ->
                                    R.string.calculator_error_invalid_expression
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.End,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                CalculatorKeyRow(
                    keys = listOf("C", "⌫", "%", "÷"),
                    onKey = { key ->
                        when (key) {
                            "C" -> viewModel.clear()
                            "⌫" -> viewModel.backspace()
                            "%" -> viewModel.inputPercent()
                            "÷" -> viewModel.inputOperator('÷')
                        }
                    },
                )
                CalculatorKeyRow(listOf("7", "8", "9", "×")) { key ->
                    if (key == "×") viewModel.inputOperator('×') else viewModel.inputDigit(key[0])
                }
                CalculatorKeyRow(listOf("4", "5", "6", "−")) { key ->
                    if (key == "−") viewModel.inputOperator('-') else viewModel.inputDigit(key[0])
                }
                CalculatorKeyRow(listOf("1", "2", "3", "+")) { key ->
                    if (key == "+") viewModel.inputOperator('+') else viewModel.inputDigit(key[0])
                }
                CalculatorKeyRow(listOf("()", "0", if (useComma) "," else ".", "=")) { key ->
                    when (key) {
                        "()" -> viewModel.inputParenthesis()
                        ",", "." -> viewModel.inputDecimal()
                        "=" -> if (viewModel.equalsPressed()) onUnlock()
                        else -> viewModel.inputDigit(key[0])
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorKeyRow(
    keys: List<String>,
    onKey: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        keys.forEach { key ->
            Button(
                onClick = { onKey(key) },
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(text = key, fontSize = 22.sp)
            }
        }
    }
}
