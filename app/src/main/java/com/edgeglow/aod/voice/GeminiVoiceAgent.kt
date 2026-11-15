package com.edgeglow.aod.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class GeminiVoiceAgent(
    private val context: Context,
    private val onTtsStart: (String) -> Unit,
    private val onTtsDone: () -> Unit
) {
    companion object {
        private const val TAG = "GeminiVoiceAgent"
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("pt", "BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                isTtsInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onTtsStart(utteranceId ?: "")
                    }

                    override fun onDone(utteranceId: String?) {
                        onTtsDone()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onTtsDone()
                    }
                })
            } else {
                Log.e(TAG, "Falha ao inicializar TextToSpeech nativo")
            }
        }
    }

    fun speak(text: String, utteranceId: String = "assistant_reply") {
        if (!isTtsInitialized || tts == null) {
            onTtsDone()
            return
        }
        val params = android.os.Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    suspend fun queryGemini(
        apiKey: String,
        userPrompt: String,
        systemInstruction: String = "Você é o assistente de voz do Edge Glow AOD. Responda em português de forma concisa, natural e direta em 1 a 2 frases para leitura em voz alta."
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.success("Estou ouvindo continuamente! Configure sua chave do Gemini nas configurações para respostas completas de IA.")
        }

        try {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseString)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val text = parts.getJSONObject(0).getString("text")
                    Result.success(text.trim())
                } else {
                    Result.failure(Exception("Resposta vazia da API do Gemini"))
                }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("Erro na API Gemini ($responseCode): $errorStream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
