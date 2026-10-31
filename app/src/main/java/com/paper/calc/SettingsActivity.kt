package com.paper.calc

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import java.math.BigDecimal

class SettingsActivity : BaseActivity() {

    private lateinit var rgDecimal: RadioGroup
    private lateinit var rgGroup: RadioGroup
    private lateinit var tvExample: TextView
    private var updating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupTopBar(getString(R.string.settings))

        findViewById<TextView>(R.id.btn_history).onTap {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        rgDecimal = findViewById(R.id.rg_decimal)
        rgGroup = findViewById(R.id.rg_group)
        tvExample = findViewById(R.id.tv_example)

        listOf(R.id.rb_dec_comma, R.id.rb_dec_point, R.id.rb_grp_point, R.id.rb_grp_comma, R.id.rb_grp_space)
            .forEach { id -> findViewById<RadioButton>(id).onTap { } }

        rgDecimal.setOnCheckedChangeListener { _, id ->
            if (updating) return@setOnCheckedChangeListener
            prefs.decimalSeparator = if (id == R.id.rb_dec_comma) ',' else '.'
            bindSeparators()
        }
        rgGroup.setOnCheckedChangeListener { _, id ->
            if (updating) return@setOnCheckedChangeListener
            prefs.groupSeparator = when (id) {
                R.id.rb_grp_point -> '.'
                R.id.rb_grp_comma -> ','
                else -> Prefs.SPACE
            }
            bindSeparators()
        }

        val rgCopy = findViewById<RadioGroup>(R.id.rg_copy)
        rgCopy.check(if (prefs.copyResultOnly) R.id.rb_copy_result else R.id.rb_copy_full)
        findViewById<RadioButton>(R.id.rb_copy_full).onTap { }
        findViewById<RadioButton>(R.id.rb_copy_result).onTap { }
        rgCopy.setOnCheckedChangeListener { _, id -> prefs.copyResultOnly = id == R.id.rb_copy_result }

        val hapticIds = mapOf(
            Prefs.HAPTIC_OFF to R.id.rb_haptic_off,
            Prefs.HAPTIC_LIGHT to R.id.rb_haptic_light,
            Prefs.HAPTIC_MEDIUM to R.id.rb_haptic_medium,
            Prefs.HAPTIC_STRONG to R.id.rb_haptic_strong,
        )
        val rgHaptic = findViewById<RadioGroup>(R.id.rg_haptic)
        rgHaptic.check(hapticIds[prefs.hapticLevel] ?: R.id.rb_haptic_medium)
        rgHaptic.setOnCheckedChangeListener { _, id ->
            val level = hapticIds.entries.firstOrNull { it.value == id }?.key ?: Prefs.HAPTIC_MEDIUM
            prefs.hapticLevel = level
            vibrate(level) // immediate test of the chosen strength
        }

        val btnLanguage = findViewById<TextView>(R.id.btn_language)
        btnLanguage.text = currentLanguageName()
        btnLanguage.onTap { chooseLanguage() }

        bindAbout()
        bindSeparators()
    }

    private fun bindSeparators() {
        updating = true
        val dec = prefs.decimalSeparator
        val grp = prefs.groupSeparator
        rgDecimal.check(if (dec == ',') R.id.rb_dec_comma else R.id.rb_dec_point)
        rgGroup.check(
            when (grp) {
                '.' -> R.id.rb_grp_point
                ',' -> R.id.rb_grp_comma
                else -> R.id.rb_grp_space
            }
        )
        // The group separator cannot be the same as the decimal separator.
        findViewById<RadioButton>(R.id.rb_grp_point).isEnabled = dec != '.'
        findViewById<RadioButton>(R.id.rb_grp_comma).isEnabled = dec != ','
        updating = false

        val example = prefs.numberFormat().formatResult(BigDecimal("1234567.89"))
        tvExample.text = getString(R.string.example, example)
    }

    private fun currentLanguageName(): String =
        LANGUAGES[prefs.language] ?: getString(R.string.language_system)

    private fun chooseLanguage() {
        val tags = listOf("") + LANGUAGES.keys
        val names = (listOf(getString(R.string.language_system)) + LANGUAGES.values).toTypedArray()
        val checked = tags.indexOf(prefs.language).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.language)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                haptic()
                dialog.dismiss()
                if (tags[which] != prefs.language) {
                    prefs.language = tags[which]
                    recreate()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun bindAbout() {
        findViewById<TextView>(R.id.tv_app_version).text =
            getString(R.string.about_version, getString(R.string.app_name), BuildConfig.VERSION_NAME)
        findViewById<TextView>(R.id.tv_about).text = getString(R.string.about_author)
        findViewById<TextView>(R.id.tv_release_url).text = RELEASES_URL
        findViewById<TextView>(R.id.btn_latest_release).onTap { openLatestRelease() }
    }

    private fun openLatestRelease() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)))
        } catch (e: ActivityNotFoundException) {
            // No browser on the device: copy the link instead.
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("URL", RELEASES_URL))
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val RELEASES_URL = "https://github.com/mdeberly/paper_calc/releases/latest"
    }
}
