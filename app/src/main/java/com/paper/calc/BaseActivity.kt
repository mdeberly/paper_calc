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
    fun haptic() = vibrate(prefs.hapticLevel)

    /**
     * Vibrates for the given strength level (0 = off). Very short pulses are not
     * felt on the Kompakt's vibration motor, so the shortest level is 35 ms and
     * the full amplitude is used when the motor supports amplitude control.
     */
    fun vibrate(level: Int) {
        if (level <= Prefs.HAPTIC_OFF) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val ms = HAPTIC_MS[level.coerceIn(1, HAPTIC_MS.size - 1)]
        val amplitude = if (v.hasAmplitudeControl()) 255 else VibrationEffect.DEFAULT_AMPLITUDE
        try {
            v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } catch (e: RuntimeException) {
            // Some ROMs reject vibration requests; never crash the calculator for that.
        }
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
        /** Vibration length per level: off, light, medium, strong. */
        private val HAPTIC_MS = longArrayOf(0L, 35L, 60L, 100L)

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
