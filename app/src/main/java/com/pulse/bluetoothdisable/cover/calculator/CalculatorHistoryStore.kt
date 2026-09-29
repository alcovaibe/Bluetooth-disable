package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context
import androidx.core.content.edit

data class CalculatorHistoryEntry(
    val expression: String,
    val result: String,
)

class CalculatorHistoryStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun entries(): List<CalculatorHistoryEntry> {
        val count = preferences.getInt(KEY_COUNT, 0)
            .coerceIn(0, MAX_ENTRIES)

        return buildList {
            repeat(count) { index ->
                val expression = preferences.getString(
                    expressionKey(index),
                    null,
                )
                val result = preferences.getString(
                    resultKey(index),
                    null,
                )

                if (expression != null && result != null) {
                    add(
                        CalculatorHistoryEntry(
                            expression = expression,
                            result = result,
                        ),
                    )
                }
            }
        }
    }

    fun add(expression: String, result: String) {
        val updated = (
                listOf(
                    CalculatorHistoryEntry(
                        expression = expression,
                        result = result,
                    ),
                ) + entries()
                ).take(MAX_ENTRIES)

        preferences.edit(commit = true) {
            clear()
            putInt(KEY_COUNT, updated.size)

            updated.forEachIndexed { index, entry ->
                putString(
                    expressionKey(index),
                    entry.expression,
                )
                putString(
                    resultKey(index),
                    entry.result,
                )
            }
        }
    }

    fun clear() {
        preferences.edit(commit = true) {
            clear()
        }
    }

    private fun expressionKey(index: Int): String =
        "expression_$index"

    private fun resultKey(index: Int): String =
        "result_$index"

    companion object {
        const val PREFERENCES_NAME =
            "calculator_history_preferences"

        private const val KEY_COUNT =
            "entry_count"

        const val MAX_ENTRIES = 50
    }
}
