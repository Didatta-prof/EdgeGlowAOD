package com.edgeglow.aod

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object AppPreferences {
    private const val PREF_NAME = "edge_glow_prefs"
    private const val KEY_COLOR_1 = "glow_color_1"
    private const val KEY_COLOR_2 = "glow_color_2"
    private const val KEY_COLOR_3 = "glow_color_3"
    private const val KEY_STROKE_WIDTH = "stroke_width_dp"
    private const val KEY_DURATION_SEC = "duration_seconds"
    private const val KEY_CORNER_RADIUS = "corner_radius_dp"
    private const val KEY_ENABLE_AOD = "enable_aod"
    private const val KEY_SPEED = "animation_speed"

    // Configurações de Atualização Automática
    private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
    private const val KEY_REMIND_LATER_TIME = "update_remind_later_time"
    private const val KEY_UPDATE_URL = "update_json_url"

    // Configurações de Voz Ininterrupta & Wake Word
    private const val KEY_VOICE_ENABLED = "voice_continuous_enabled"
    private const val KEY_WAKE_WORD = "voice_wake_word"
    private const val KEY_CONTINUOUS_CONVERSATION = "voice_continuous_conversation"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_VOICE_SENSITIVITY = "voice_mic_sensitivity"

    // Cores vibrantes padrão: Neon Cyan, Cyber Purple, Hot Pink
    const val DEFAULT_COLOR_1 = 0xFF00F5FF.toInt() // Neon Cyan
    const val DEFAULT_COLOR_2 = 0xFFBF00FF.toInt() // Electric Purple
    const val DEFAULT_COLOR_3 = 0xFFFF007F.toInt() // Neon Pink

    // URL padrão para verificação de atualização (ex: GitHub Releases ou servidor)
    const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/edgeglow/releases/main/version.json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getColors(context: Context): List<Color> {
        val prefs = getPrefs(context)
        val c1 = prefs.getInt(KEY_COLOR_1, DEFAULT_COLOR_1)
        val c2 = prefs.getInt(KEY_COLOR_2, DEFAULT_COLOR_2)
        val c3 = prefs.getInt(KEY_COLOR_3, DEFAULT_COLOR_3)
        return listOf(Color(c1), Color(c2), Color(c3))
    }

    fun setColors(context: Context, c1: Color, c2: Color, c3: Color) {
        getPrefs(context).edit()
            .putInt(KEY_COLOR_1, c1.toArgb())
            .putInt(KEY_COLOR_2, c2.toArgb())
            .putInt(KEY_COLOR_3, c3.toArgb())
            .apply()
    }

    fun getStrokeWidth(context: Context): Float {
        return getPrefs(context).getFloat(KEY_STROKE_WIDTH, 14f)
    }

    fun setStrokeWidth(context: Context, width: Float) {
        getPrefs(context).edit().putFloat(KEY_STROKE_WIDTH, width).apply()
    }

    fun getCornerRadius(context: Context): Float {
        // Motorola Edge 50 tem cantos curvos generosos (~42dp)
        return getPrefs(context).getFloat(KEY_CORNER_RADIUS, 42f)
    }

    fun setCornerRadius(context: Context, radius: Float) {
        getPrefs(context).edit().putFloat(KEY_CORNER_RADIUS, radius).apply()
    }

    fun getDurationSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_DURATION_SEC, 10)
    }

    fun setDurationSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_DURATION_SEC, seconds).apply()
    }

    fun isAodEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLE_AOD, true)
    }

    fun setAodEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_AOD, enabled).apply()
    }

    fun getAnimationSpeedMs(context: Context): Int {
        return getPrefs(context).getInt(KEY_SPEED, 3000)
    }

    fun setAnimationSpeedMs(context: Context, speedMs: Int) {
        getPrefs(context).edit().putInt(KEY_SPEED, speedMs).apply()
    }

    // --- Módulo de Atualizações ---

    fun isAutoUpdateCheckEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_UPDATE_CHECK, true)
    }

    fun setAutoUpdateCheckEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
    }

    fun getUpdateUrl(context: Context): String {
        return getPrefs(context).getString(KEY_UPDATE_URL, DEFAULT_UPDATE_URL) ?: DEFAULT_UPDATE_URL
    }

    fun setUpdateUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_UPDATE_URL, url).apply()
    }

    fun getRemindLaterTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_REMIND_LATER_TIME, 0L)
    }

    fun setRemindLaterTimestamp(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_REMIND_LATER_TIME, timestamp).apply()
    }

    // Adia por 24 horas a notificação de nova versão
    fun setRemindLater24Hours(context: Context) {
        val nextReminder = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        setRemindLaterTimestamp(context, nextReminder)
    }

    fun shouldShowUpdatePrompt(context: Context): Boolean {
        val remindTime = getRemindLaterTimestamp(context)
        return System.currentTimeMillis() >= remindTime
    }

    // --- Módulo de Voz Contínua & Wake Word ---

    fun isVoiceServiceEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VOICE_ENABLED, false)
    }

    fun setVoiceServiceEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
    }

    fun getWakeWord(context: Context): String {
        return getPrefs(context).getString(KEY_WAKE_WORD, "Edge") ?: "Edge"
    }

    fun setWakeWord(context: Context, wakeWord: String) {
        getPrefs(context).edit().putString(KEY_WAKE_WORD, wakeWord).apply()
    }

    fun isContinuousConversationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CONTINUOUS_CONVERSATION, true)
    }

    fun setContinuousConversationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CONTINUOUS_CONVERSATION, enabled).apply()
    }

    fun getGeminiApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setGeminiApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    fun getVoiceSensitivity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_VOICE_SENSITIVITY, 0.5f)
    }

    fun setVoiceSensitivity(context: Context, sensitivity: Float) {
        getPrefs(context).edit().putFloat(KEY_VOICE_SENSITIVITY, sensitivity).apply()
    }
}
