package com.edgeglow.aod

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AodActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockscreenDisplay()

        val notifTitle = intent.getStringExtra("notif_title") ?: ""
        val notifText = intent.getStringExtra("notif_text") ?: ""

        val colors = AppPreferences.getColors(this)
        val strokeWidth = AppPreferences.getStrokeWidth(this).dp
        val cornerRadius = AppPreferences.getCornerRadius(this).dp
        val durationSec = AppPreferences.getDurationSeconds(this)
        val speedMs = AppPreferences.getAnimationSpeedMs(this)

        setContent {
            AodScreen(
                colors = colors,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius,
                speedMs = speedMs,
                notifTitle = notifTitle,
                notifText = notifText,
                autoCloseAfterSec = durationSec,
                onDismiss = { dismissGracefully() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun dismissGracefully() {
        if (!isFinishing && !isDestroyed) {
            finish()
        }
    }

    private fun setupLockscreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // REMOVIDO: requestDismissKeyguard, que provocava conflito com o bloqueio do aparelho
            // e forçava a tela a apagar instantaneamente ao acender.
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Modo tela cheia total imersivo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }
}

@Composable
fun AodScreen(
    colors: List<Color>,
    strokeWidth: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    speedMs: Int,
    notifTitle: String,
    notifText: String,
    autoCloseAfterSec: Int,
    onDismiss: () -> Unit
) {
    var currentTime by remember { mutableStateOf(getFormattedTime()) }
    var currentDate by remember { mutableStateOf(getFormattedDate()) }

    // Atualiza relógio a cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getFormattedTime()
            currentDate = getFormattedDate()
            delay(1000)
        }
    }

    // Auto dismiss configurável para economizar bateria OLED
    if (autoCloseAfterSec > 0) {
        LaunchedEffect(Unit) {
            delay(autoCloseAfterSec * 1000L)
            onDismiss()
        }
    }

    // Fundo 100% Preto OLED (pixels completamente desligados)
    // Toque na tela fecha o AOD e dá acesso imediato à tela de bloqueio
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
    ) {
        // Efeito vibrante nas bordas do Moto Edge 50
        EdgeGlowBorder(
            colors = colors,
            strokeWidth = strokeWidth,
            cornerRadius = cornerRadius,
            animationDurationMs = speedMs
        )

        // Conteúdo AOD central (Relógio Minimalista e Notificação)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )

            Text(
                text = currentDate,
                fontSize = 15.sp,
                color = Color.Gray
            )

            if (notifTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = notifTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.firstOrNull() ?: Color.Cyan
                )

                if (notifText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notifText,
                        fontSize = 14.sp,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun getFormattedTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun getFormattedDate(): String {
    return SimpleDateFormat("EEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
}
