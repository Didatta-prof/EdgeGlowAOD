package com.edgeglow.aod.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AudioStreamRecorder(
    private val onAudioChunk: (buffer: ShortArray, readCount: Int, rmsLevel: Float) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 16000
        private const val TAG = "AudioStreamRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    @Volatile
    var isRecording: Boolean = false
        private set

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError("Configuração de áudio não suportada pelo hardware")
            return
        }

        val bufferSize = max(minBufferSize, SAMPLE_RATE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback para MIC simples se VOICE_RECOGNITION falhar
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Não foi possível inicializar o microfone (AudioRecord)")
                return
            }

            // Ativa cancelamento de eco e supressão de ruído nativos se disponíveis
            val audioSessionId = audioRecord!!.audioSessionId
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                    enabled = true
                }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                    enabled = true
                }
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val readBuffer = ShortArray(1024)
                try {
                    while (isActive && isRecording) {
                        val record = audioRecord ?: break
                        val readCount = record.read(readBuffer, 0, readBuffer.size)
                        if (readCount > 0) {
                            val rms = calculateRms(readBuffer, readCount)
                            onAudioChunk(readBuffer, readCount, rms)
                        } else if (readCount < 0) {
                            Log.w(TAG, "Erro de leitura do buffer: $readCount")
                        }
                    }
                } catch (e: CancellationException) {
                    // Normal coroutine cancellation
                } catch (e: Exception) {
                    onError("Erro na captura de áudio: ${e.localizedMessage}")
                } finally {
                    cleanUp()
                }
            }
        } catch (e: Exception) {
            onError("Exceção ao iniciar microfone: ${e.localizedMessage}")
            cleanUp()
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        cleanUp()
    }

    private fun cleanUp() {
        try {
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    try {
                        it.stop()
                    } catch (ignored: Exception) {}
                }
                it.release()
            }
        } catch (ignored: Exception) {}
        audioRecord = null

        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
        } catch (ignored: Exception) {}
        echoCanceler = null
        noiseSuppressor = null
    }

    private fun calculateRms(buffer: ShortArray, readCount: Int): Float {
        if (readCount <= 0) return 0f
        var sumSquares = 0.0
        for (i in 0 until readCount) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        val meanSquare = sumSquares / readCount
        val rms = sqrt(meanSquare)
        // Normaliza entre 0f e 1f (onde 32767 é amplitude máxima de 16-bit)
        val normalized = (rms / 32768.0).toFloat()
        return min(1.0f, max(0.0f, normalized * 3.5f))
    }
}
