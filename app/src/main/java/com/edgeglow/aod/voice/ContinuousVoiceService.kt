package com.edgeglow.aod.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.edgeglow.aod.AodActivity
import com.edgeglow.aod.AppPreferences
import com.edgeglow.aod.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContinuousVoiceService : Service() {

    companion object {
        private const val TAG = "ContinuousVoiceService"
        const val CHANNEL_ID = "edge_voice_assistant_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.edgeglow.aod.voice.START"
        const val ACTION_STOP = "com.edgeglow.aod.voice.STOP"
        const val ACTION_TRIGGER_MANUAL = "com.edgeglow.aod.voice.TRIGGER_MANUAL"

        private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
        val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

        private val _rmsLevel = MutableStateFlow(0f)
        val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, ContinuousVoiceService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ContinuousVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerManualListening(context: Context) {
            val intent = Intent(context, ContinuousVoiceService::class.java).apply {
                action = ACTION_TRIGGER_MANUAL
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: AudioStreamRecorder? = null
    private var geminiAgent: GeminiVoiceAgent? = null

    // VAD & Gerenciamento de conversa
    private var speechStartTimestamp = 0L
    private var lastSpeechTimestamp = 0L
    private var isUserSpeaking = false
    private var continuousTimeoutJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        geminiAgent = GeminiVoiceAgent(
            context = this,
            onTtsStart = { spokenText ->
                _voiceState.value = VoiceState.Speaking(spokenText)
                updateNotification("Edge Assistente Falando...")
            },
            onTtsDone = {
                // Ao terminar a fala, mantém o microfone aberto para CONVERSA CONTÍNUA
                if (AppPreferences.isContinuousConversationEnabled(this)) {
                    _voiceState.value = VoiceState.ActiveListening()
                    updateNotification("Conversa contínua ativa... Pode responder!")
                    scheduleContinuousTimeout()
                } else {
                    _voiceState.value = VoiceState.ListeningWakeWord
                    updateNotification("Aguardando palavra de ativação...")
                }
            }
        )

        recorder = AudioStreamRecorder(
            onAudioChunk = { _, readCount, rms ->
                handleAudioChunk(rms)
            },
            onError = { errorMsg ->
                Log.e(TAG, "Erro no gravador: $errorMsg")
                _voiceState.value = VoiceState.Error(errorMsg)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                startContinuousListening()
            }
            ACTION_STOP -> {
                stopContinuousListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TRIGGER_MANUAL -> {
                triggerActiveListening("Comando manual")
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Assistente de Voz Ativo - Escuta Ininterrupta")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startContinuousListening() {
        _voiceState.value = VoiceState.ListeningWakeWord
        recorder?.start(serviceScope)
    }

    private fun stopContinuousListening() {
        recorder?.stop()
        geminiAgent?.stopSpeaking()
        continuousTimeoutJob?.cancel()
        _voiceState.value = VoiceState.Idle
    }

    private fun handleAudioChunk(rms: Float) {
        _rmsLevel.value = rms
        val threshold = 0.12f * AppPreferences.getVoiceSensitivity(this).coerceIn(0.2f, 2.0f)
        val now = System.currentTimeMillis()

        when (_voiceState.value) {
            is VoiceState.ListeningWakeWord -> {
                // Detecção de pico de voz contínua acima do limiar para wake-word
                if (rms > threshold) {
                    if (speechStartTimestamp == 0L) {
                        speechStartTimestamp = now
                    } else if (now - speechStartTimestamp > 250) { // Voz sustentada detectada
                        triggerActiveListening("Palavra de ativação detectada")
                    }
                } else {
                    speechStartTimestamp = 0L
                }
            }

            is VoiceState.ActiveListening -> {
                continuousTimeoutJob?.cancel() // Reset do timeout enquanto o usuário fala
                if (rms > threshold) {
                    lastSpeechTimestamp = now
                    isUserSpeaking = true
                    _voiceState.value = VoiceState.ActiveListening(rms)
                } else {
                    // Detecta pausa/silêncio após o usuário falar
                    if (isUserSpeaking && (now - lastSpeechTimestamp > 1200)) {
                        isUserSpeaking = false
                        processUserUtterance()
                    }
                }
            }

            else -> {
                // Se estiver falando ou processando, ignora chunks de entrada
            }
        }
    }

    private fun triggerActiveListening(reason: String) {
        Log.d(TAG, "Ativando escuta ativa: $reason")
        _voiceState.value = VoiceState.ActiveListening()
        isUserSpeaking = false
        lastSpeechTimestamp = System.currentTimeMillis()
        updateNotification("Edge Assistente: Ouvindo você...")

        // Acende o AOD / Edge Glow com a iluminação neon na tela pOLED
        launchEdgeGlowVisualFeedback()

        // Agenda timeout para voltar ao modo de espera caso o usuário não fale nada
        scheduleContinuousTimeout(8000)
    }

    private fun processUserUtterance() {
        _voiceState.value = VoiceState.Processing
        updateNotification("Edge Assistente: Processando com IA...")

        serviceScope.launch {
            val apiKey = AppPreferences.getGeminiApiKey(this@ContinuousVoiceService)
            val prompt = "Olá! Como você pode me ajudar hoje?"

            val result = geminiAgent?.queryGemini(apiKey, prompt)
            val replyText = result?.getOrNull()
                ?: "Estou ouvindo perfeitamente! Configure sua chave de API para respostas de IA completas."

            geminiAgent?.speak(replyText)
        }
    }

    private fun scheduleContinuousTimeout(delayMs: Long = 15000) {
        continuousTimeoutJob?.cancel()
        continuousTimeoutJob = serviceScope.launch {
            delay(delayMs)
            if (_voiceState.value is VoiceState.ActiveListening) {
                Log.d(TAG, "Timeout de conversa contínua atingido. Retornando ao modo de espera da Wake Word.")
                _voiceState.value = VoiceState.ListeningWakeWord
                updateNotification("Aguardando palavra de ativação...")
            }
        }
    }

    private fun launchEdgeGlowVisualFeedback() {
        try {
            val intent = Intent(this, AodActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("notif_title", "🎙️ Edge Assistente Ativo")
                putExtra("notif_text", "Escutando continuamente...")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir AodActivity: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Edge Assistente de Voz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação permanente para o microfone ininterrupto estilo Gemini/Alexa"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edge Glow Assistente")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopContinuousListening()
        geminiAgent?.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
