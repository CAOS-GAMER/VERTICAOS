package com.verticaos.games

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreditManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("credit_prefs", Context.MODE_PRIVATE)
    private val _credits = MutableStateFlow(prefs.getInt("total_credits", 0))
    val credits: StateFlow<Int> = _credits

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "total_credits") {
            _credits.value = prefs.getInt("total_credits", 0)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val coinSoundId: Int = soundPool.load(context, R.raw.coin, 1)
    private val navSoundId: Int = soundPool.load(context, R.raw.sound, 1)

    fun addCredit() {
        val newVal = _credits.value + 1
        saveCredits(newVal)
        soundPool.play(coinSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun playNavigationSound() {
        soundPool.play(navSoundId, 0.7f, 0.7f, 1, 0, 1f)
    }

    /**
     * Calculates available game credits and corresponding total pulses without deducting credits in advance.
     * @return Pair(totalCredits, totalPulsesToInject)
     */
    fun calculateCreditsToInject(pulsesPerCredit: Int): Pair<Int, Int> {
        val totalAvailable = prefs.getInt("total_credits", 0)
        val required = if (pulsesPerCredit <= 0) 1 else pulsesPerCredit
        if (totalAvailable >= required) {
            val totalCredits = totalAvailable / required
            val totalPulsesToInject = totalCredits * required
            return Pair(totalCredits, totalPulsesToInject)
        }
        return Pair(0, 0)
    }

    /**
     * Deducts the specified number of pulses from the total accumulated credits in SharedPreferences.
     * Should be called ONLY AFTER all pulses have been successfully dispatched.
     */
    fun deductPulses(pulses: Int) {
        val current = prefs.getInt("total_credits", 0)
        val newVal = (current - pulses).coerceAtLeast(0)
        saveCredits(newVal)
    }

    fun consume(amount: Int): Int {
        val (creditsToInject, pulsesConsumed) = calculateCreditsToInject(amount)
        if (pulsesConsumed > 0) {
            deductPulses(pulsesConsumed)
            return creditsToInject
        }
        return 0
    }

    private fun saveCredits(value: Int) {
        prefs.edit().putInt("total_credits", value).apply()
        _credits.value = value
    }

    fun release() {
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        soundPool.release()
    }
}
