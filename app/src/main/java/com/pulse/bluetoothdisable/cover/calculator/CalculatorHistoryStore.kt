package com.pulse.bluetoothdisable.cover.calculator

import android.content.Context

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
        val count = preferences.getInt(KEY_COUNT, 0).coerceIn(0, MAX_ENTRIES)
        return buildList {
            repeat(count) { index ->
                val expression = preferences.getString(expressionKey(index), null)
                val result = preferences.getString(resultKey(index), null)
                if (expression != null && result != null) {
                    add(CalculatorHistoryEntry(expression, result))
                }
            }
        }
    }

    fun add(expression: String, result: String) {
        val updated = (listOf(CalculatorHistoryEntry(expression, result)) + entries())
            .take(MAX_ENTRIES)
        val editor = preferences.edit().clear().putInt(KEY_COUNT, updated.size)
        updated.forEachIndexed { index, entry ->
            editor.putString(expressionKey(index), entry.expression)
            editor.putString(resultKey(index), entry.result)
        }
        editor.commit()
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    private fun expressionKey(index: Int) = "expression_$index"
    private fun resultKey(index: Int) = "result_$index"

    companion object {
        const val PREFERENCES_NAME = "calculator_history_preferences"
        private const val KEY_COUNT = "entry_count"
        const val MAX_ENTRIES = 50
    }
}
