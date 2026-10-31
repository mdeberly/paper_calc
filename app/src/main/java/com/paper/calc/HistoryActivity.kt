package com.paper.calc

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.paper.calc.core.Calculator
import com.paper.calc.core.NumberFormat

/** Last 20 operations validated with "=". Tap = reuse, long press = copy. */
class HistoryActivity : BaseActivity() {

    private lateinit var list: ListView
    private lateinit var empty: TextView
    private lateinit var hint: TextView
    private lateinit var btnClear: TextView
    private lateinit var format: NumberFormat
    private var entries: List<Calculator.Entry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        btnClear = setupTopBar(getString(R.string.history))
        btnClear.text = getString(R.string.clear)
        btnClear.onTap { confirmClear() }

        list = findViewById(R.id.list)
        empty = findViewById(R.id.tv_empty)
        hint = findViewById(R.id.tv_hint)
        format = prefs.numberFormat()

        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ ->
            haptic()
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_EXPRESSION, entries[pos].expression)
            )
            finish()
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            haptic()
            val text = MainActivity.entryText(format, entries[pos])
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
            Toast.makeText(this, getString(R.string.copied, text), Toast.LENGTH_SHORT).show()
            true
        }
        load()
    }

    private fun load() {
        entries = prefs.history()
        adapter.notifyDataSetChanged()
        val isEmpty = entries.isEmpty()
        list.visibility = if (isEmpty) View.GONE else View.VISIBLE
        hint.visibility = if (isEmpty) View.GONE else View.VISIBLE
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        btnClear.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_history_confirm)
            .setPositiveButton(R.string.clear) { _, _ ->
                haptic()
                prefs.clearHistory()
                load()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private val adapter = object : BaseAdapter() {
        override fun getCount() = entries.size
        override fun getItem(position: Int) = entries[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.item_history, parent, false)
            val e = entries[position]
            v.findViewById<TextView>(R.id.tv_expr).text = format.formatExpression(e.expression)
            v.findViewById<TextView>(R.id.tv_result).text =
                "= " + format.formatResult(Calculator.resultValue(e))
            return v
        }
    }
}
