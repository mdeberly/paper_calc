package com.paper.calc.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Evaluates canonical expressions while respecting operator precedence:
 *   parentheses > unary minus > × ÷ > + −
 *
 * Canonical alphabet (internal representation, independent of display settings):
 *   digits, '.', '+', '-', '*', '/', '(', ')', and '~' for unary minus.
 */
object Evaluator {

    private val MC = MathContext(34, RoundingMode.HALF_EVEN)
    private val MAX_ABS = BigDecimal.ONE.scaleByPowerOfTen(100)

    sealed class Result {
        data class Value(val value: BigDecimal) : Result()
        object DivisionByZero : Result()
        object Overflow : Result()
        object Invalid : Result()
    }

    private class DivZero : RuntimeException()
    private class TooBig : RuntimeException()
    private class Syntax : RuntimeException()

    const val BINARY_OPS = "+-*/"

    /** True when the expression contains at least one operation (so a result is worth showing). */
    fun hasOperation(expr: String): Boolean = expr.any { it in BINARY_OPS || it == '~' }

    /**
     * Makes an in-progress expression evaluable: drops trailing operators / open
     * parentheses and closes the parentheses left open.
     */
    fun complete(expr: String): String {
        var e = expr
        while (e.isNotEmpty() && (e.last() in BINARY_OPS || e.last() == '~' || e.last() == '(')) {
            e = e.dropLast(1)
        }
        if (e.isEmpty()) return e
        val open = e.count { it == '(' } - e.count { it == ')' }
        return if (open > 0) e + ")".repeat(open) else e
    }

    /** Evaluates a complete expression. */
    fun evaluate(expr: String): Result {
        if (expr.isEmpty()) return Result.Invalid
        return try {
            val p = Parser(expr)
            val v = p.parseExpression()
            if (!p.atEnd()) throw Syntax()
            Result.Value(v)
        } catch (e: DivZero) {
            Result.DivisionByZero
        } catch (e: TooBig) {
            Result.Overflow
        } catch (e: Syntax) {
            Result.Invalid
        } catch (e: ArithmeticException) {
            Result.Invalid
        } catch (e: NumberFormatException) {
            Result.Invalid
        }
    }

    /** Evaluates an expression that is still being typed. */
    fun preview(expr: String): Result = evaluate(complete(expr))

    private class Parser(private val s: String) {
        private var pos = 0

        fun atEnd() = pos >= s.length

        private fun peek(): Char? = if (pos < s.length) s[pos] else null

        private fun check(v: BigDecimal): BigDecimal {
            if (v.abs() >= MAX_ABS) throw TooBig()
            return v
        }

        fun parseExpression(): BigDecimal {
            var v = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; v = check(v.add(parseTerm(), MC)) }
                    '-' -> { pos++; v = check(v.subtract(parseTerm(), MC)) }
                    else -> return v
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var v = parseUnary()
            while (true) {
                when (peek()) {
                    '*' -> { pos++; v = check(v.multiply(parseUnary(), MC)) }
                    '/' -> {
                        pos++
                        val d = parseUnary()
                        if (d.signum() == 0) throw DivZero()
                        v = check(v.divide(d, MC))
                    }
                    else -> return v
                }
            }
        }

        private fun parseUnary(): BigDecimal {
            if (peek() == '~') {
                pos++
                return parseUnary().negate()
            }
            return parsePrimary()
        }

        private fun parsePrimary(): BigDecimal {
            val c = peek() ?: throw Syntax()
            if (c == '(') {
                pos++
                val v = parseExpression()
                if (peek() != ')') throw Syntax()
                pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) throw Syntax()
            var lit = s.substring(start, pos)
            if (lit == ".") throw Syntax()
            if (lit.endsWith('.')) lit = lit.dropLast(1)
            if (lit.startsWith('.')) lit = "0$lit"
            return BigDecimal(lit)
        }
    }
}
