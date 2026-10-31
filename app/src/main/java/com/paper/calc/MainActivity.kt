package com.paper.calc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.paper.calc.core.Calculator
import com.paper.calc.core.Evaluator
import com.paper.calc.core.NumberFormat

class MainActivity : BaseActivity() {

    private val calc = Calculator()
    private lateinit var format: NumberFormat

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var keyDot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.expression)
        tvResult = findViewById(R.id.result)
        keyDot = findViewById(R.id.key_dot)

        calc.load(prefs.expression, prefs.expressionSaved)
        handleIntent(intent)

        val digits = intArrayOf(
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4,
            R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9,
        )
        digits.forEachIndexed { d, id -> key(id) { calc.digit('0' + d) } }
        key(R.id.key_dot) { calc.dot() }
        key(R.id.key_add) { calc.operator('+') }
        key(R.id.key_sub) { calc.operator('-') }
        key(R.id.key_mul) { calc.operator('*') }
        key(R.id.key_div) { calc.operator('/') }
        key(R.id.key_paren) { calc.parenthesis() }
        key(R.id.key_sign) { calc.toggleSign() }
        key(R.id.key_back) { calc.backspace() }
        key(R.id.key_clear) { calc.clear() }
        key(R.id.key_eq) { onEquals() }

        // Long press on backspace clears everything.
        findViewById<View>(R.id.key_back).setOnLongClickListener {
            haptic(); calc.clear(); refresh(); true
        }

        findViewById<View>(R.id.btn_copy).onTap { copyToClipboard() }
        findViewById<View>(R.id.btn_more).onTap {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        // Separators may have been changed in the settings.
        format = prefs.numberFormat()
        keyDot.text = format.decimalSeparator.toString()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        prefs.expression = calc.expression
        prefs.expressionSaved = calc.saved
    }

    private fun handleIntent(intent: Intent?) {
        val expr = intent?.getStringExtra(EXTRA_EXPRESSION) ?: return
        intent.removeExtra(EXTRA_EXPRESSION)
        calc.load(expr, saved = true)
    }

    private fun key(id: Int, action: () -> Unit) {
        findViewById<View>(id).onTap {
            action()
            refresh()
        }
    }

    private fun onEquals() {
        val entry = calc.equals() ?: return
        prefs.addHistory(entry)
    }

    /** Recomputes the result on every key press. */
    private fun refresh() {
        tvExpression.text = format.formatExpression(calc.expression)
        tvResult.text = resultText()
    }

    private fun resultText(): String {
        if (!calc.showsResult()) return ""
        return when (val r = calc.preview()) {
            is Evaluator.Result.Value -> {
                val s = format.formatResult(r.value)
                if (calc.saved) "= $s" else s
            }
            Evaluator.Result.DivisionByZero -> getString(R.string.error_division_by_zero)
            Evaluator.Result.Overflow -> getString(R.string.error_overflow)
            Evaluator.Result.Invalid -> ""
        }
    }

    private fun copyToClipboard() {
        if (calc.expression.isEmpty()) return
        val expr = format.formatExpression(calc.expression)
        val text = when (val r = calc.preview()) {
            is Evaluator.Result.Value ->
                if (calc.showsResult()) "$expr = ${format.formatResult(r.value)}" else expr
            else -> expr
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
        Toast.makeText(this, getString(R.string.copied, text), Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_EXPRESSION = "expression"

        fun entryText(format: NumberFormat, e: Calculator.Entry) =
            format.formatExpression(e.expression) + " = " + format.formatResult(Calculator.resultValue(e))
    }
}
