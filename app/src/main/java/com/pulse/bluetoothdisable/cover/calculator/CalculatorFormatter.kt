package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal

object CalculatorFormatter {
    fun format(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val normalized = value.stripTrailingZeros()
        val plain = normalized.toPlainString()
        return if (plain.length <= MAX_PLAIN_LENGTH) plain else normalized.toEngineeringString()
    }

    fun localize(value: String, useComma: Boolean): String =
        if (useComma) value.replace('.', ',') else value

    private const val MAX_PLAIN_LENGTH = 60
}
