package com.pulse.bluetoothdisable.cover.calculator

import java.math.BigDecimal

internal class CalculatorParser(private val tokens: List<CalculatorToken>) {
    private var index = 0

    fun parse(): CalculatorNode {
        val result = parseAddSubtract()
        if (current().type != CalculatorTokenType.EOF) throw CalculatorParseException()
        return result
    }

    private fun parseAddSubtract(): CalculatorNode {
        var left = parseMultiplyDivide()
        while (true) {
            left = when {
                match(CalculatorTokenType.PLUS) ->
                    BinaryNode(left, BinaryOperator.ADD, parseMultiplyDivide())
                match(CalculatorTokenType.MINUS) ->
                    BinaryNode(left, BinaryOperator.SUBTRACT, parseMultiplyDivide())
                else -> return left
            }
        }
    }

    private fun parseMultiplyDivide(): CalculatorNode {
        var left = parsePercent()
        while (true) {
            left = when {
                match(CalculatorTokenType.MULTIPLY) ->
                    BinaryNode(left, BinaryOperator.MULTIPLY, parsePercent())
                match(CalculatorTokenType.DIVIDE) ->
                    BinaryNode(left, BinaryOperator.DIVIDE, parsePercent())
                else -> return left
            }
        }
    }

    private fun parsePercent(): CalculatorNode {
        var node = parseUnary()
        while (match(CalculatorTokenType.PERCENT)) {
            node = PercentNode(node)
        }
        return node
    }

    private fun parseUnary(): CalculatorNode =
        if (match(CalculatorTokenType.MINUS)) {
            UnaryMinusNode(parseUnary())
        } else {
            parsePrimary()
        }

    private fun parsePrimary(): CalculatorNode {
        if (match(CalculatorTokenType.LEFT_PAREN)) {
            val expression = parseAddSubtract()
            if (!match(CalculatorTokenType.RIGHT_PAREN)) throw CalculatorParseException()
            return expression
        }

        val token = current()
        if (token.type != CalculatorTokenType.NUMBER) throw CalculatorParseException()
        index++
        return try {
            NumberNode(BigDecimal(token.text))
        } catch (_: NumberFormatException) {
            throw CalculatorParseException()
        }
    }

    private fun match(type: CalculatorTokenType): Boolean {
        if (current().type != type) return false
        index++
        return true
    }

    private fun current(): CalculatorToken = tokens[index]
}
