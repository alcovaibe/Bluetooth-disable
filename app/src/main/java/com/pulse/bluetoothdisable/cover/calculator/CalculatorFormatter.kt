package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal

object CalculatorFormatter {
    fun format(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val normalized = value.stripTrailingZeros()
        val plain = normalized.toPlainString()
        if (plain.length <= MAX_PLAIN_LENGTH) return plain

        val exponent = normalized.precision() - normalized.scale() - 1
        val mantissa = normalized.movePointLeft(exponent).stripTrailingZeros().toPlainString()
        return "$mantissa × 10^$exponent"
    }

    fun toExpression(value: BigDecimal): String =
        if (value.compareTo(BigDecimal.ZERO) == 0) "0" else value.stripTrailingZeros().toPlainString()

    fun localize(value: String, useComma: Boolean): String =
        if (useComma) value.replace('.', ',') else value

    private const val MAX_PLAIN_LENGTH = 60
}
