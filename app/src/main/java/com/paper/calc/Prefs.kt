package com.paper.calc

import android.content.Context
import android.content.SharedPreferences
import com.paper.calc.core.Calculator
import com.paper.calc.core.NumberFormat
import org.json.JSONArray
import org.json.JSONObject

/** Persistent settings, current expression and history (last [HISTORY_SIZE] operations). */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("paper_calc", Context.MODE_PRIVATE)

    var decimalSeparator: Char
        get() = sp.getString(KEY_DECIMAL, null)?.firstOrNull() ?: '.'
        set(value) {
            sp.edit().putString(KEY_DECIMAL, value.toString()).apply()
            // separators must differ: move the group separator out of the way
            if (groupSeparator == value) groupSeparator = if (value == ',') '.' else ','
        }

    var groupSeparator: Char
        get() = sp.getString(KEY_GROUP, null)?.firstOrNull() ?: ','
        set(value) = sp.edit().putString(KEY_GROUP, value.toString()).apply()

    /** Haptic feedback strength: [HAPTIC_OFF], [HAPTIC_LIGHT], [HAPTIC_MEDIUM] or [HAPTIC_STRONG]. */
    var hapticLevel: Int
        get() = when {
            sp.contains(KEY_HAPTIC_LEVEL) -> sp.getInt(KEY_HAPTIC_LEVEL, HAPTIC_MEDIUM)
            // migration from the former on/off setting
            !sp.getBoolean(KEY_HAPTIC, true) -> HAPTIC_OFF
            else -> HAPTIC_MEDIUM
        }
        set(value) = sp.edit().putInt(KEY_HAPTIC_LEVEL, value).apply()

    /** Copy button: true = result only, false = "operation = result". */
    var copyResultOnly: Boolean
        get() = sp.getBoolean(KEY_COPY_RESULT_ONLY, false)
        set(value) = sp.edit().putBoolean(KEY_COPY_RESULT_ONLY, value).apply()

    /** Language tag ("" = follow the system). */
    var language: String
        get() = sp.getString(KEY_LANGUAGE, "") ?: ""
        set(value) = sp.edit().putString(KEY_LANGUAGE, value).apply()

    var expression: String
        get() = sp.getString(KEY_EXPR, "") ?: ""
        set(value) = sp.edit().putString(KEY_EXPR, value).apply()

    var expressionSaved: Boolean
        get() = sp.getBoolean(KEY_EXPR_SAVED, false)
        set(value) = sp.edit().putBoolean(KEY_EXPR_SAVED, value).apply()

    fun numberFormat(): NumberFormat {
        val dec = decimalSeparator
        var grp = groupSeparator
        if (grp == dec) grp = if (dec == ',') '.' else ','
        return NumberFormat(dec, grp)
    }

    // --------------------------------------------------------------- history

    fun history(): List<Calculator.Entry> {
        val raw = sp.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Calculator.Entry(o.getString("e"), o.getString("r"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(entry: Calculator.Entry) {
        val list = history().toMutableList()
        if (list.firstOrNull() == entry) return
        list.add(0, entry)
        saveHistory(list.take(HISTORY_SIZE))
    }

    fun clearHistory() = saveHistory(emptyList())

    private fun saveHistory(list: List<Calculator.Entry>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("e", it.expression).put("r", it.result)) }
        sp.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        const val HISTORY_SIZE = 20

        const val HAPTIC_OFF = 0
        const val HAPTIC_LIGHT = 1
        const val HAPTIC_MEDIUM = 2
        const val HAPTIC_STRONG = 3

        /** Group separator used for "space". */
        const val SPACE = NumberFormat.SPACE

        private const val KEY_DECIMAL = "decimal_sep"
        private const val KEY_GROUP = "group_sep"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_HAPTIC_LEVEL = "haptic_level"
        private const val KEY_COPY_RESULT_ONLY = "copy_result_only"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_EXPR = "expression"
        private const val KEY_EXPR_SAVED = "expression_saved"
        private const val KEY_HISTORY = "history"
    }
}
