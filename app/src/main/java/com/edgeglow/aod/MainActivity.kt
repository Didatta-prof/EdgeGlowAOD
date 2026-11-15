package com.edgeglow.aod

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edgeglow.aod.updater.UpdateInfo
import com.edgeglow.aod.updater.UpdateManager
import com.edgeglow.aod.voice.ContinuousVoiceService
import com.edgeglow.aod.voice.VoiceState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF101014))) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF101014)
                ) {
                    MainSettingsScreen(
                        onRequestNotificationAccess = { openNotificationAccessSettings() },
                        onRequestBatteryOptimization = { openBatteryOptimizationSettings() },
                        onTestAod = { testAodNotification() }
                    )
                }
            }
        }
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun testAodNotification() {
        val intent = Intent(this, AodActivity::class.java).apply {
            putExtra("notif_title", "WhatsApp • Notificação Teste")
            putExtra("notif_text", "Mensagem de demonstração do Edge Glow!")
        }
        startActivity(intent)
    }
}

// Paletas vibrantes pré-definidas
data class GlowPalette(
    val name: String,
    val colors: List<Color>
)

val PRESET_PALETTES = listOf(
    GlowPalette("Cyberpunk Neon", listOf(Color(0xFF00F5FF), Color(0xFFBF00FF), Color(0xFFFF007F))),
    GlowPalette("Aurora Boreal", listOf(Color(0xFF00FF87), Color(0xFF60EFFF), Color(0xFF0061FF))),
    GlowPalette("Fogo Cósmico", listOf(Color(0xFFFF0844), Color(0xFFFFB199), Color(0xFFFF8008))),
    GlowPalette("Ouro Eletrizante", listOf(Color(0xFFFFDF00), Color(0xFFFF9900), Color(0xFFFF4500))),
    GlowPalette("Tóxico Futurista", listOf(Color(0xFF39FF14), Color(0xFF00FFFF), Color(0xFF2E0854)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onRequestNotificationAccess: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onTestAod: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isEnabled by remember { mutableStateOf(AppPreferences.isAodEnabled(context)) }
    var selectedPaletteIndex by remember { mutableIntStateOf(0) }
    var strokeWidth by remember { mutableFloatStateOf(AppPreferences.getStrokeWidth(context)) }
    var cornerRadius by remember { mutableFloatStateOf(AppPreferences.getCornerRadius(context)) }
    var durationSec by remember { mutableIntStateOf(AppPreferences.getDurationSeconds(context)) }
    var speedMs by remember { mutableIntStateOf(AppPreferences.getAnimationSpeedMs(context)) }

    // Estados do Assistente de Voz Ininterrupto
    var isVoiceEnabled by remember { mutableStateOf(AppPreferences.isVoiceServiceEnabled(context)) }
    var wakeWordText by remember { mutableStateOf(AppPreferences.getWakeWord(context)) }
    var isContinuousConvEnabled by remember { mutableStateOf(AppPreferences.isContinuousConversationEnabled(context)) }
    var geminiApiKey by remember { mutableStateOf(AppPreferences.getGeminiApiKey(context)) }
    var voiceSensitivity by remember { mutableFloatStateOf(AppPreferences.getVoiceSensitivity(context)) }

    val currentVoiceState by ContinuousVoiceService.voiceState.collectAsState()
    val currentRms by ContinuousVoiceService.rmsLevel.collectAsState()

    // Estados de Atualização Automática
    var updateAvailable by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isCheckingUpdatesManual by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Launcher de Permissão do Microfone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isVoiceEnabled = true
            AppPreferences.setVoiceServiceEnabled(context, true)
            ContinuousVoiceService.startService(context)
            Toast.makeText(context, "Escuta ininterrupta iniciada!", Toast.LENGTH_SHORT).show()
        } else {
            isVoiceEnabled = false
            AppPreferences.setVoiceServiceEnabled(context, false)
            Toast.makeText(context, "Permissão do microfone necessária.", Toast.LENGTH_LONG).show()
        }
    }

    // Checagem automática de atualização ao abrir o app
    LaunchedEffect(Unit) {
        if (AppPreferences.isAutoUpdateCheckEnabled(context)) {
            val result = UpdateManager.checkForUpdates(context, forceCheck = false)
            result.getOrNull()?.let { info ->
                updateAvailable = info
            }
        }
    }

    // Diálogo de Atualização Automática
    updateAvailable?.let { info ->
        AlertDialog(
            onDismissRequest = {
                if (!info.mandatory && !isDownloadingUpdate) updateAvailable = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFF00F5FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Nova Versão ${info.versionName}")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Uma nova versão do Edge Glow AOD está disponível!",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                    if (info.changelog.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Novidades:\n${info.changelog}",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    if (isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Baixando atualização: ${(downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = Color(0xFF00F5FF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF00F5FF)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDownloadingUpdate) {
                            isDownloadingUpdate = true
                            coroutineScope.launch {
                                val result = UpdateManager.downloadApk(context, info.apkUrl) { progress ->
                                    downloadProgress = progress
                                }
                                isDownloadingUpdate = false
                                result.onSuccess { apkFile ->
                                    updateAvailable = null
                                    UpdateManager.promptInstallApk(context, apkFile)
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        "Erro no download: ${error.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5FF), contentColor = Color.Black),
                    enabled = !isDownloadingUpdate
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isDownloadingUpdate) "Baixando..." else "Instalar Agora")
                }
            },
            dismissButton = {
                if (!info.mandatory && !isDownloadingUpdate) {
                    TextButton(onClick = {
                        AppPreferences.setRemindLater24Hours(context)
                        updateAvailable = null
                        Toast.makeText(context, "Lembrete adiado por 24 horas", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(text = "Lembrar Depois", color = Color.Gray)
                    }
                }
            },
            containerColor = Color(0xFF1E1E28),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo para Chave de API do Gemini
    if (showApiKeyDialog) {
        var keyInput by remember { mutableStateOf(geminiApiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(text = "Chave de API do Gemini", color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Insira sua API Key do Google AI Studio para que o assistente responda a perguntas completas com a inteligência do Gemini.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Chave de API (AIza...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            focusedLabelColor = Color(0xFF00F5FF),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        geminiApiKey = keyInput.trim()
                        AppPreferences.setGeminiApiKey(context, geminiApiKey)
                        showApiKeyDialog = false
                        Toast.makeText(context, "Chave API salva!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5FF), contentColor = Color.Black)
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E28),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Edge Glow AOD (Moto Edge 50)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF16161D),
                titleContentColor = Color.White
            )
        )

        Column(modifier = Modifier.padding(16.dp)) {

            // Seção de Status Principal do AOD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ativar Always On Display",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnabled) "Ativo para notificações" else "Desativado",
                            fontSize = 13.sp,
                            color = if (isEnabled) Color(0xFF00FF87) else Color.Gray
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            AppPreferences.setAodEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00F5FF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SEÇÃO 1: ASSISTENTE DE VOZ ININTERRUPTO (GEMINI / ALEXA)
            // ==========================================
            Text(
                text = "ASSISTENTE DE VOZ ININTERRUPTO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F5FF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Switch Ativação de Escuta Ininterrupta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Escuta Ininterrupta de Microfone",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Captura contínua sem timeouts ou bips constantes",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }

                        Switch(
                            checked = isVoiceEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        isVoiceEnabled = true
                                        AppPreferences.setVoiceServiceEnabled(context, true)
                                        ContinuousVoiceService.startService(context)
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    isVoiceEnabled = false
                                    AppPreferences.setVoiceServiceEnabled(context, false)
                                    ContinuousVoiceService.stopService(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00F5FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch Conversa Contínua
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conversa Contínua (Gemini Live)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Mantém escutando após a IA responder, sem repetir a palavra-chave",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = isContinuousConvEnabled,
                            onCheckedChange = {
                                isContinuousConvEnabled = it
                                AppPreferences.setContinuousConversationEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFBF00FF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status ao Vivo & RMS visual
                    val (statusLabel, statusColor) = when (currentVoiceState) {
                        is VoiceState.Idle -> "Desligado" to Color.Gray
                        is VoiceState.ListeningWakeWord -> "Aguardando '$wakeWordText'..." to Color(0xFF00F5FF)
                        is VoiceState.ActiveListening -> "Ouvindo você (Conversa Ativa)..." to Color(0xFF00FF87)
                        is VoiceState.Processing -> "Processando com IA..." to Color(0xFFFF9900)
                        is VoiceState.Speaking -> "IA Respondendo..." to Color(0xFFFF007F)
                        is VoiceState.Error -> "Erro de Microfone" to Color.Red
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF14141C))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isVoiceEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusLabel,
                                fontSize = 13.sp,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isVoiceEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${(currentRms * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Palavra de Ativação
                    OutlinedTextField(
                        value = wakeWordText,
                        onValueChange = {
                            wakeWordText = it
                            AppPreferences.setWakeWord(context, it)
                        },
                        label = { Text("Palavra de Ativação (Wake Word)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            focusedLabelColor = Color(0xFF00F5FF),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sensibilidade do Microfone
                    Text(
                        text = "Sensibilidade do Microfone: ${(voiceSensitivity * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Slider(
                        value = voiceSensitivity,
                        onValueChange = {
                            voiceSensitivity = it
                            AppPreferences.setVoiceSensitivity(context, it)
                        },
                        valueRange = 0.2f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F5FF),
                            activeTrackColor = Color(0xFF00F5FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Botões de Ação de Voz
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00F5FF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (geminiApiKey.isNotBlank()) "API Key OK" else "Configurar Gemini",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                if (isVoiceEnabled) {
                                    ContinuousVoiceService.triggerManualListening(context)
                                } else {
                                    Toast.makeText(context, "Ative a escuta contínua primeiro", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBF00FF),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Falar Agora", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SEÇÃO 2: ATUALIZAÇÕES AUTOMÁTICAS (AUTO-UPDATER)
            // ==========================================
            Text(
                text = "ATUALIZAÇÕES DO APLICATIVO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Versão Atual Instalada",
                                fontSize = 14.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "v${UpdateManager.getCurrentVersionName(context)} (Build ${UpdateManager.getCurrentVersionCode(context)})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isCheckingUpdatesManual) {
                                    isCheckingUpdatesManual = true
                                    coroutineScope.launch {
                                        val res = UpdateManager.checkForUpdates(context, forceCheck = true)
                                        isCheckingUpdatesManual = false
                                        res.onSuccess { info ->
                                            if (info != null) {
                                                updateAvailable = info
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "O Edge Glow já está na versão mais recente!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                "Falha ao checar atualizações: ${err.localizedMessage}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00F5FF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCheckingUpdatesManual) "Checando..." else "Verificar",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sempre ao abrir, o app verifica silenciosamente se há nova versão e exibe a opção de 'Instalar Agora' ou 'Lembrar Depois'.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Permissões Essenciais do Sistema
            Text(
                text = "PERMISSÕES NECESSÁRIAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRequestNotificationAccess,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00F5FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Autorizar Acesso a Notificações", color = Color.White)
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onRequestBatteryOptimization,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Desativar Otimização de Bateria", color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Seção de Cores Vibrantes (Presets)
            Text(
                text = "PALETAS DE CORES VIBRANTES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            PRESET_PALETTES.forEachIndexed { index, palette ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedPaletteIndex = index
                            val c = palette.colors
                            AppPreferences.setColors(context, c[0], c[1], c[2])
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPaletteIndex == index) Color(0xFF282836) else Color(0xFF1E1E28)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (selectedPaletteIndex == index) {
                        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00F5FF))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            palette.colors.forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = palette.name, color = Color.White, fontSize = 14.sp)
                        }

                        if (selectedPaletteIndex == index) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00F5FF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Customização da Borda do Moto Edge 50
            Text(
                text = "AJUSTE DA TELA CURVA (MOTO EDGE 50)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Espessura da Borda: ${strokeWidth.toInt()} dp",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = strokeWidth,
                        onValueChange = {
                            strokeWidth = it
                            AppPreferences.setStrokeWidth(context, it)
                        },
                        valueRange = 6f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F5FF),
                            activeTrackColor = Color(0xFF00F5FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Curvatura dos Cantos: ${cornerRadius.toInt()} dp",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = cornerRadius,
                        onValueChange = {
                            cornerRadius = it
                            AppPreferences.setCornerRadius(context, it)
                        },
                        valueRange = 20f..65f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFBF00FF),
                            activeTrackColor = Color(0xFFBF00FF)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Tempo de Exibição: $durationSec segundos",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = durationSec.toFloat(),
                        onValueChange = {
                            durationSec = it.toInt()
                            AppPreferences.setDurationSeconds(context, durationSec)
                        },
                        valueRange = 5f..30f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF007F),
                            activeTrackColor = Color(0xFFFF007F)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botão de Testar Agora
            Button(
                onClick = onTestAod,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F5FF),
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Testar Efeito Agora",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
