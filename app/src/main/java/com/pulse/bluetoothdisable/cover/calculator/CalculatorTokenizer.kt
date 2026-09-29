package com.pulse.bluetoothdisable.cover.calculator

internal enum class CalculatorTokenType {
    NUMBER,
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    PERCENT,
    LEFT_PAREN,
    RIGHT_PAREN,
    EOF,
}

internal data class CalculatorToken(
    val type: CalculatorTokenType,
    val text: String = "",
)

internal class CalculatorParseException : IllegalArgumentException()

internal object CalculatorTokenizer {
    fun tokenize(input: String): List<CalculatorToken> {
        val tokens = mutableListOf<CalculatorToken>()
        var index = 0

        while (index < input.length) {
            val char = input[index]
            when {
                char.isWhitespace() -> index++
                char.isDigit() || char == '.' || char == ',' -> {
                    val number = StringBuilder()
                    var decimalSeen = false
                    var digitSeen = false
                    while (index < input.length) {
                        val current = input[index]
                        if (current.isDigit()) {
                            digitSeen = true
                            number.append(current)
                            index++
                        } else if (current == '.' || current == ',') {
                            if (decimalSeen) throw CalculatorParseException()
                            decimalSeen = true
                            number.append('.')
                            index++
                        } else {
                            break
                        }
                    }
                    if (!digitSeen) throw CalculatorParseException()
                    tokens += CalculatorToken(CalculatorTokenType.NUMBER, number.toString())
                }
                char == '+' -> {
                    tokens += CalculatorToken(CalculatorTokenType.PLUS)
                    index++
                }
                char == '-' || char == '−' -> {
                    tokens += CalculatorToken(CalculatorTokenType.MINUS)
                    index++
                }
                char == '×' || char == '*' -> {
                    tokens += CalculatorToken(CalculatorTokenType.MULTIPLY)
                    index++
                }
                char == '÷' || char == '/' -> {
                    tokens += CalculatorToken(CalculatorTokenType.DIVIDE)
                    index++
                }
                char == '%' -> {
                    tokens += CalculatorToken(CalculatorTokenType.PERCENT)
                    index++
                }
                char == '(' -> {
                    tokens += CalculatorToken(CalculatorTokenType.LEFT_PAREN)
                    index++
                }
                char == ')' -> {
                    tokens += CalculatorToken(CalculatorTokenType.RIGHT_PAREN)
                    index++
                }
                else -> throw CalculatorParseException()
            }
        }

        tokens += CalculatorToken(CalculatorTokenType.EOF)
        return tokens
    }
}
