package com.paper.calc

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import java.util.Locale

/** Applies the in-app language and provides haptic feedback helpers. */
abstract class BaseActivity : Activity() {

    protected lateinit var prefs: Prefs
    private var appliedLanguage: String = ""
    private var vibrator: Vibrator? = null

    override fun attachBaseContext(newBase: Context) {
        val lang = Prefs(newBase).language
        appliedLanguage = lang
        super.attachBaseContext(wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onResume() {
        super.onResume()
        // Language changed in the settings while this screen was in the back stack.
        if (prefs.language != appliedLanguage) recreate()
    }

    /** Short "click" vibration, if enabled. */
    fun haptic() {
        if (!prefs.haptic) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(HAPTIC_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * Click listener that vibrates as soon as the finger touches the key
     * (more responsive than waiting for the release).
     */
    protected fun View.onTap(action: () -> Unit) {
        isSoundEffectsEnabled = false
        @Suppress("ClickableViewAccessibility")
        setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_DOWN) haptic()
            false
        }
        setOnClickListener { action() }
    }

    /** Wires the shared top bar (back arrow + title). */
    protected fun setupTopBar(title: CharSequence): TextView {
        findViewById<TextView>(R.id.tv_title).text = title
        findViewById<ImageButton>(R.id.btn_back).onTap { finish() }
        return findViewById(R.id.btn_action)
    }

    companion object {
        private const val HAPTIC_MS = 22L

        /** Supported languages: tag to native name. "" = system. */
        val LANGUAGES = linkedMapOf(
            "fr" to "Français",
            "en" to "English",
            "pl" to "Polski",
            "de" to "Deutsch",
            "es" to "Español",
            "pt" to "Português",
            "it" to "Italiano",
            "nl" to "Nederlands",
            "cs" to "Čeština",
            "sv" to "Svenska",
        )

        fun wrap(base: Context, language: String): Context {
            if (language.isEmpty()) return base
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }
    }
}
