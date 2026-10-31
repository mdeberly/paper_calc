package com.paper.calc.core

import java.math.BigDecimal

/**
 * Input state machine of the calculator. Holds the canonical expression and
 * applies key presses to it. Free of Android dependencies so it can be unit tested.
 */
class Calculator {

    data class Entry(val expression: String, val result: String)

    /** Canonical expression (see [Evaluator]). */
    var expression: String = ""
        private set

    /** True right after "=" was pressed: the operation has been saved to history. */
    var saved: Boolean = false
        private set

    fun load(expression: String, saved: Boolean = false) {
        this.expression = expression.filter { it.isDigit() || it in ".+-*/()~" }
        this.saved = saved
    }

    fun preview(): Evaluator.Result = Evaluator.preview(expression)

    /** True when the result line is worth displaying. */
    fun showsResult(): Boolean = Evaluator.hasOperation(Evaluator.complete(expression))

    // ---------------------------------------------------------------- keys

    fun clear() {
        expression = ""
        saved = false
    }

    fun digit(d: Char) {
        require(d in '0'..'9')
        startFreshIfSaved()
        if (expression.lastOrNull() == ')') expression += "*"
        val num = trailingNumber()
        if (num == "0") {
            expression = expression.dropLast(1) + d
            return
        }
        if (num.count { it.isDigit() } >= MAX_DIGITS) return
        expression += d
    }

    fun dot() {
        startFreshIfSaved()
        if (expression.lastOrNull() == ')') expression += "*"
        val num = trailingNumber()
        if (num.contains('.')) return
        expression += if (num.isEmpty()) "0." else "."
    }

    fun operator(op: Char) {
        require(op in Evaluator.BINARY_OPS)
        continueFromResultIfSaved()
        val last = expression.lastOrNull()
        when {
            last == null -> if (op == '-') expression = "~"
            last == '(' -> if (op == '-') expression += "~"
            last == '~' -> {
                if (op == '-') return
                expression = expression.dropLast(1)
                val prev = expression.lastOrNull()
                if (prev != null && prev in Evaluator.BINARY_OPS) {
                    expression = expression.dropLast(1) + op
                }
            }
            last in Evaluator.BINARY_OPS -> {
                expression = if (op == '-' && (last == '*' || last == '/')) {
                    "$expression~"
                } else {
                    expression.dropLast(1) + op
                }
            }
            else -> expression += op
        }
    }

    /** Smart parenthesis: closes one when it makes sense, otherwise opens one. */
    fun parenthesis() {
        startFreshIfSaved()
        val last = expression.lastOrNull()
        val open = expression.count { it == '(' } - expression.count { it == ')' }
        expression += if (last != null && (last.isDigit() || last == '.' || last == ')')) {
            if (open > 0) ")" else "*("
        } else {
            "("
        }
    }

    fun toggleSign() {
        continueFromResultIfSaved()
        val last = expression.lastOrNull()
        when {
            last == null -> expression = "~"
            last == '~' -> expression = expression.dropLast(1)
            last in Evaluator.BINARY_OPS || last == '(' -> expression += "~"
            else -> {
                val start = if (last == ')') matchingOpen(expression.length - 1)
                else expression.length - trailingNumber().length
                if (start < 0) return
                expression = if (start > 0 && expression[start - 1] == '~') {
                    expression.removeRange(start - 1, start)
                } else {
                    expression.substring(0, start) + "~" + expression.substring(start)
                }
            }
        }
    }

    fun backspace() {
        saved = false
        expression = expression.dropLast(1)
    }

    /**
     * "=" key: validates the current operation. Returns the entry to store in
     * the history, or null when there is nothing (valid) to save.
     */
    fun equals(): Entry? {
        val completed = Evaluator.complete(expression)
        if (!Evaluator.hasOperation(completed)) return null
        val r = Evaluator.evaluate(completed) as? Evaluator.Result.Value ?: return null
        expression = completed
        saved = true
        return Entry(completed, NumberFormat.roundResult(r.value).toPlainString())
    }

    // ------------------------------------------------------------- helpers

    private fun startFreshIfSaved() {
        if (saved) {
            expression = ""
            saved = false
        }
    }

    private fun continueFromResultIfSaved() {
        if (!saved) return
        saved = false
        val r = Evaluator.evaluate(expression)
        expression = if (r is Evaluator.Result.Value) NumberFormat.canonical(r.value) else ""
    }

    private fun trailingNumber(): String {
        var i = expression.length
        while (i > 0 && (expression[i - 1].isDigit() || expression[i - 1] == '.')) i--
        return expression.substring(i)
    }

    private fun matchingOpen(closeIndex: Int): Int {
        var depth = 0
        for (i in closeIndex downTo 0) {
            when (expression[i]) {
                ')' -> depth++
                '(' -> { depth--; if (depth == 0) return i }
            }
        }
        return -1
    }

    companion object {
        const val MAX_DIGITS = 15

        fun resultValue(entry: Entry): BigDecimal = BigDecimal(entry.result)
    }
}
