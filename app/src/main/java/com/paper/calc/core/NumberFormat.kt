package com.paper.calc.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Converts canonical expressions / results into human readable text using the
 * user-selected decimal separator and thousands (group) separator.
 */
class NumberFormat(val decimalSeparator: Char, val groupSeparator: Char) {

    init {
        require(decimalSeparator != groupSeparator) { "Separators must differ" }
    }

    /** Formats a number literal as typed (keeps a trailing dot or trailing zeros). */
    fun formatLiteral(literal: String): String {
        val dot = literal.indexOf('.')
        val intPart = if (dot >= 0) literal.substring(0, dot) else literal
        val sb = StringBuilder(group(intPart))
        if (dot >= 0) {
            sb.append(decimalSeparator)
            sb.append(literal.substring(dot + 1))
        }
        return sb.toString()
    }

    /** Formats a full canonical expression for display. */
    fun formatExpression(expr: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                val start = i
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                out.append(formatLiteral(expr.substring(start, i)))
                continue
            }
            out.append(
                when (c) {
                    '+' -> "+"
                    '-' -> "−"
                    '*' -> "×"
                    '/' -> "÷"
                    '~' -> "−"
                    else -> c.toString()
                }
            )
            i++
        }
        return out.toString()
    }

    /** Formats a computed result. */
    fun formatResult(value: BigDecimal): String {
        val v = roundResult(value)
        if (v.signum() == 0) return "0"
        val abs = v.abs()
        if (abs >= SCI_HIGH || abs < SCI_LOW) return formatScientific(v)
        val plain = v.toPlainString()
        val negative = plain.startsWith("-")
        val body = if (negative) plain.substring(1) else plain
        return (if (negative) "−" else "") + formatLiteral(body)
    }

    private fun formatScientific(v: BigDecimal): String {
        val exponent = v.precision() - v.scale() - 1
        val mantissa = v.movePointLeft(exponent)
            .round(MathContext(10, RoundingMode.HALF_UP))
            .stripTrailingZeros()
        val m = mantissa.toPlainString()
        val negative = m.startsWith("-")
        val body = if (negative) m.substring(1) else m
        return (if (negative) "−" else "") + formatLiteral(body) + "E" + exponent
    }

    private fun group(intPart: String): String {
        if (intPart.length <= 3) return intPart
        val sb = StringBuilder()
        val first = intPart.length % 3
        if (first > 0) sb.append(intPart, 0, first)
        var i = first
        while (i < intPart.length) {
            if (sb.isNotEmpty()) sb.append(groupSeparator)
            sb.append(intPart, i, i + 3)
            i += 3
        }
        return sb.toString()
    }

    companion object {
        const val SPACE = ' ' // non-breaking space keeps numbers on one line

        private val SCI_HIGH = BigDecimal("1E15")
        private val SCI_LOW = BigDecimal("1E-9")

        /** Rounds to 15 significant digits and strips trailing zeros. */
        fun roundResult(value: BigDecimal): BigDecimal {
            val r = value.round(MathContext(15, RoundingMode.HALF_UP))
            return if (r.signum() == 0) BigDecimal.ZERO else r.stripTrailingZeros()
        }

        /** Canonical expression text for a result, so it can be reused in a new calculation. */
        fun canonical(value: BigDecimal): String {
            val v = roundResult(value)
            val plain = v.toPlainString()
            return if (plain.startsWith("-")) "~" + plain.substring(1) else plain
        }
    }
}
