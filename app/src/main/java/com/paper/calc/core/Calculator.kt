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
        this.expression = expression.filter { it.isDigit() || it in ".+-*/^()~√²!πe" }
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
        if (endsWithNonNumberOperand()) expression += "*"
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
        if (endsWithNonNumberOperand()) expression += "*"
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
            last == '(' || last == '√' -> if (op == '-') expression += "~"
            last == '~' -> {
                if (op == '-') return
                expression = expression.dropLast(1)
                val prev = expression.lastOrNull()
                if (prev != null && prev in Evaluator.BINARY_OPS) {
                    expression = expression.dropLast(1) + op
                }
            }
            last in Evaluator.BINARY_OPS -> {
                expression = if (op == '-' && (last == '*' || last == '/' || last == '^')) {
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
        expression += if (last != null && last in Evaluator.OPERAND_END) {
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
            last !in Evaluator.OPERAND_END -> expression += "~"
            else -> {
                val start = lastOperandStart()
                if (start < 0) return
                expression = if (start > 0 && expression[start - 1] == '~') {
                    expression.removeRange(start - 1, start)
                } else {
                    expression.substring(0, start) + "~" + expression.substring(start)
                }
            }
        }
    }

    /** π or e: a constant operand. */
    fun constant(c: Char) {
        require(c == 'π' || c == 'e')
        startFreshIfSaved()
        if (expression.lastOrNull()?.let { it in Evaluator.OPERAND_END } == true) expression += "*"
        expression += c
    }

    /** Square root (prefix): applies to what is typed next. */
    fun squareRoot() {
        startFreshIfSaved()
        if (expression.lastOrNull()?.let { it in Evaluator.OPERAND_END } == true) expression += "*"
        expression += "√"
    }

    /** Postfix operator: '²' (square) or '!' (factorial), applied to the last operand. */
    fun postfix(op: Char) {
        require(op == '²' || op == '!')
        continueFromResultIfSaved()
        val last = expression.lastOrNull() ?: return
        if (last !in Evaluator.OPERAND_END) return
        if (last == '.') expression = expression.dropLast(1)
        expression += op
    }

    /** 1/x: replaces the last operand x by (1/x). */
    fun reciprocal() {
        continueFromResultIfSaved()
        val last = expression.lastOrNull()
        if (last == null || last !in Evaluator.OPERAND_END) {
            expression += "(1/"
            return
        }
        val start = lastOperandStart()
        if (start < 0) return
        expression = expression.substring(0, start) + "(1/" + expression.substring(start) + ")"
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

    private fun endsWithNonNumberOperand(): Boolean {
        val last = expression.lastOrNull() ?: return false
        return last in ")πe²!"
    }

    /** Start index of the last complete operand (including its √ prefixes), or -1. */
    private fun lastOperandStart(): Int {
        var i = expression.length - 1
        while (i >= 0 && (expression[i] == '²' || expression[i] == '!')) i--
        if (i < 0) return -1
        var start = when (val c = expression[i]) {
            ')' -> matchingOpen(i)
            'π', 'e' -> i
            else -> if (c.isDigit() || c == '.') {
                var j = i
                while (j > 0 && (expression[j - 1].isDigit() || expression[j - 1] == '.')) j--
                j
            } else -1
        }
        if (start < 0) return -1
        while (start > 0 && expression[start - 1] == '√') start--
        return start
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
