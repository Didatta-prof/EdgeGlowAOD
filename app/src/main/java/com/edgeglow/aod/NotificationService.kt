package com.edgeglow.aod

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationService"
        private const val WAKE_COOLDOWN_MS = 6000L // Evita acordar a tela repetidamente em spam de mensagens
        private var lastWakeTime = 0L
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // 1. Não reagir a notificações do próprio app ou ongoing (player de música, downloads, chamadas em andamento)
        if (sbn.packageName == packageName || sbn.isOngoing) {
            return
        }

        // 2. Verifica se o AOD está habilitado
        if (!AppPreferences.isAodEnabled(this)) {
            return
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // 3. ECONOMIA CRÍTICA DE BATERIA:
        // Se a tela já estiver ligada/em uso pelo usuário (isInteractive == true), NÃO acorda nem abre o AOD por cima
        if (powerManager.isInteractive) {
            return
        }

        // 4. Rate-limiting contra drenagem de bateria por notificações em sequência (ex: grupos de WhatsApp)
        val now = System.currentTimeMillis()
        if (now - lastWakeTime < WAKE_COOLDOWN_MS) {
            return
        }

        val notification = sbn.notification ?: return

        // 5. Ignora notificações silenciosas ou de baixa prioridade (ex: resumo de passos, sync em segundo plano)
        if (notification.priority < Notification.PRIORITY_DEFAULT) {
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) {
            return
        }

        lastWakeTime = now
        wakeUpAndLaunchAod(title, text)
    }

    private fun wakeUpAndLaunchAod(title: String, text: String) {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "EdgeGlow:AodWakeLock"
            )
            wakeLock.acquire(2000L)

            val intent = Intent(this, AodActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notif_title", title)
                putExtra("notif_text", text)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao acordar tela para AOD: ${e.message}")
        }
    }
}
