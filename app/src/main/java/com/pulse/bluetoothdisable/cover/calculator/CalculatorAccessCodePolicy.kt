package com.pulse.bluetoothdisable.cover.calculator

object CalculatorAccessCodePolicy {
    const val CODE_LENGTH = 5

    fun isValid(code: String): Boolean =
        code.length == CODE_LENGTH && code.all(Char::isDigit)

    fun matches(code: String, confirmation: String): Boolean =
        isValid(code) && code == confirmation
}
