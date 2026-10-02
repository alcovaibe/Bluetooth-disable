package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal

object CalculatorFormatter {
    /** Canonical, parser-safe representation used for calculator state and persistence. */
    fun format(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        return value.stripTrailingZeros().toPlainString()
    }

    /** User-facing representation. Very large/small values use y × 10^c instead of E notation. */
    fun formatDisplay(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"

        val normalized = value.stripTrailingZeros()
        val plain = normalized.toPlainString()
        return if (plain.length <= MAX_PLAIN_LENGTH) {
            localize(plain, useComma = true)
        } else {
            scientific(normalized)
        }
    }

    /** Calculator Cover Mode always displays a decimal comma in every app locale. */
    @Suppress("UNUSED_PARAMETER")
    fun localize(value: String, useComma: Boolean): String = value.replace('.', ',')

    private fun scientific(value: BigDecimal): String {
        val exponent = value.precision() - 1 - value.scale()
        val mantissa = value
            .movePointLeft(exponent)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
        return "$mantissa × 10^$exponent"
    }

    private const val MAX_PLAIN_LENGTH = 60
}
