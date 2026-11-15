package com.edgeglow.aod.voice

sealed class VoiceState {
    object Idle : VoiceState()
    object ListeningWakeWord : VoiceState()
    data class ActiveListening(val rmsLevel: Float = 0f) : VoiceState()
    object Processing : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}
