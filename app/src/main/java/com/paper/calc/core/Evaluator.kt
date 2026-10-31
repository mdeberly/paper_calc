package com.paper.calc.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Evaluates canonical expressions while respecting operator precedence:
 *   parentheses > postfix (² !) > power (^, right associative) > unary (− √) > × ÷ > + −
 *
 * Canonical alphabet (internal representation, independent of display settings):
 *   digits, '.', '+', '-', '*', '/', '^', '(', ')', '~' (unary minus), '√' (square root),
 *   '²' (square), '!' (factorial), 'π' and 'e' (constants).
 */
object Evaluator {

    private val MC = MathContext(34, RoundingMode.HALF_EVEN)
    private val MAX_ABS = BigDecimal.ONE.scaleByPowerOfTen(100)

    sealed class Result {
        data class Value(val value: BigDecimal) : Result()
        object DivisionByZero : Result()
        object Overflow : Result()
        object Undefined : Result()
        object Invalid : Result()
    }

    private class DivZero : RuntimeException()
    private class TooBig : RuntimeException()
    private class Syntax : RuntimeException()
    private class Undef : RuntimeException()

    const val BINARY_OPS = "+-*/^"

    /** Characters that can end an operand. */
    const val OPERAND_END = "0123456789.)πe²!"

    /** Characters that can be left dangling while typing (dropped for the live result). */
    private const val DANGLING = BINARY_OPS + "~√("

    private val PI = BigDecimal("3.141592653589793238462643383279503")
    private val E = BigDecimal("2.718281828459045235360287471352662")
    private const val MAX_FACTORIAL = 69

    /** True when the expression contains at least one operation (so a result is worth showing). */
    fun hasOperation(expr: String): Boolean = expr.any { it in BINARY_OPS || it in "~√²!πe" }

    /**
     * Makes an in-progress expression evaluable: drops trailing operators / open
     * parentheses and closes the parentheses left open.
     */
    fun complete(expr: String): String {
        var e = expr
        while (e.isNotEmpty() && e.last() in DANGLING) {
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
        } catch (e: Undef) {
            Result.Undefined
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
            return when (peek()) {
                '~' -> { pos++; parseUnary().negate() }
                '√' -> { pos++; sqrt(parseUnary()) }
                else -> parsePower()
            }
        }

        private fun parsePower(): BigDecimal {
            val base = parsePostfix()
            if (peek() != '^') return base
            pos++
            return check(power(base, parseUnary()))
        }

        private fun parsePostfix(): BigDecimal {
            var v = parsePrimary()
            while (true) {
                v = when (peek()) {
                    '²' -> { pos++; check(v.multiply(v, MC)) }
                    '!' -> { pos++; factorial(v) }
                    else -> return v
                }
            }
        }

        private fun parsePrimary(): BigDecimal {
            val c = peek() ?: throw Syntax()
            if (c == 'π') { pos++; return PI }
            if (c == 'e') { pos++; return E }
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

    private fun sqrt(x: BigDecimal): BigDecimal {
        if (x.signum() < 0) throw Undef()
        if (x.signum() == 0) return BigDecimal.ZERO
        // Newton iteration starting from the double approximation.
        var r = BigDecimal(Math.sqrt(x.toDouble()), MC)
        if (r.signum() == 0) r = BigDecimal.ONE
        val two = BigDecimal(2)
        repeat(8) { r = r.add(x.divide(r, MC), MC).divide(two, MC) }
        return r
    }

    private fun power(x: BigDecimal, y: BigDecimal): BigDecimal {
        if (x.signum() == 0) {
            if (y.signum() < 0) throw DivZero()
            return if (y.signum() == 0) BigDecimal.ONE else BigDecimal.ZERO
        }
        val estimate = Math.log10(x.abs().toDouble()) * y.toDouble()
        if (estimate > 100.5) throw TooBig()
        if (estimate < -1000) return BigDecimal.ZERO
        val isInt = y.signum() == 0 || y.stripTrailingZeros().scale() <= 0
        if (isInt && y.abs() <= BigDecimal(9999)) {
            return x.pow(y.intValueExact(), MC)
        }
        if (x.signum() < 0) throw Undef()
        val d = Math.pow(x.toDouble(), y.toDouble())
        if (d.isNaN() || d.isInfinite()) throw Undef()
        return BigDecimal(d, MathContext(15))
    }

    private fun factorial(n: BigDecimal): BigDecimal {
        val isInt = n.signum() == 0 || n.stripTrailingZeros().scale() <= 0
        if (!isInt || n.signum() < 0) throw Undef()
        if (n > BigDecimal(MAX_FACTORIAL)) throw TooBig()
        var r = BigDecimal.ONE
        for (i in 2..n.toInt()) r = r.multiply(BigDecimal(i))
        return r
    }
}
