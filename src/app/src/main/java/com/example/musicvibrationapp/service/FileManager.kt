// File: FileManager.kt
package com.example.musicvibrationapp.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
        const val SAMPLE_RATE = 44100 
        const val CHANNELS = 2        
        const val BIT_DEPTH = 16
        private const val BUFFER_SIZE = 1024 * 1024  // 1MB buffer
    }

    private var pcmFile: File? = null
    private var wavFile: File? = null
    private var vocalPcmFile: File? = null
    private var vocalWavFile: File? = null
    private var bgmPcmFile: File? = null
    private var bgmWavFile: File? = null

    init {
        setupFiles()
    }

    fun setupFiles() {
        vocalPcmFile?.delete()
        bgmPcmFile?.delete()
        vocalWavFile?.delete()
        bgmWavFile?.delete()

        vocalPcmFile = File(context.getExternalFilesDir(null), "vocal_audio.pcm").apply { createNewFile() }
        bgmPcmFile = File(context.getExternalFilesDir(null), "bgm_audio.pcm").apply { createNewFile() }

        pcmFile?.delete()
        pcmFile = File(context.getExternalFilesDir(null), "debug_audio.pcm").apply { createNewFile() }
    }

    fun saveAudioToFile(audioData: ByteArray) {
        try {
            if (pcmFile == null) {
                pcmFile = File(context.getExternalFilesDir(null), "debug_audio.pcm")
                pcmFile?.createNewFile()
            }
            FileOutputStream(pcmFile, true).use { it.write(audioData) }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio data", e)
        }
    }

    fun saveVocalData(vocalData: ByteArray): String? {
        if (vocalData.isEmpty()) return null
        try {
            FileOutputStream(vocalPcmFile).use { it.write(vocalData) }
            val wavPath = convertPcmToWav(vocalPcmFile, true)
            if (wavPath != null) {
                vocalWavFile = File(wavPath)
            }
            return wavPath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving vocal data", e)
            return null
        }
    }

    fun saveBGMData(bgmData: ByteArray): String? {
        if (bgmData.isEmpty()) return null
        try {
            FileOutputStream(bgmPcmFile).use { it.write(bgmData) }
            val wavPath = convertPcmToWav(bgmPcmFile, false)
            if (wavPath != null) {
                bgmWavFile = File(wavPath)
            }
            return wavPath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving BGM data", e)
            return null
        }
    }

    fun convertPcmToWav() {
        try {
            val pcm = pcmFile ?: return
            if (!pcm.exists()) {
                Log.e(TAG, "PCM file does not exist.")
                return
            }
            val wav = File(context.getExternalFilesDir(null), "captured_audio.wav")
            wav.createNewFile()

            // Using buffered read/write
            FileInputStream(pcm).use { input ->
                FileOutputStream(wav).use { output ->
                    // First write WAV header
                    val fileSize = pcm.length()
                    val wavHeader = createWavHeader(fileSize, SAMPLE_RATE, CHANNELS, BIT_DEPTH)
                    output.write(wavHeader)

                    // Read PCM data in chunks
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            wavFile = wav
            pcm.delete()
            pcmFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error converting PCM to WAV", e)
        }
    }

    private fun convertPcmToWav(pcmFile: File?, isVocal: Boolean): String? {
        try {
            if (pcmFile == null || !pcmFile.exists()) {
                Log.e(TAG, "PCM file does not exist.")
                return null
            }
            val wavFile = File(
                context.getExternalFilesDir(null),
                if (isVocal) "vocal_audio_separated.wav" else "bgm_audio_separated.wav"
            ).apply {
                if (exists()) delete()
                createNewFile()
            }

            // Using buffered read/write
            FileInputStream(pcmFile).use { input ->
                FileOutputStream(wavFile).use { output ->
                    // First write WAV header
                    val fileSize = pcmFile.length()
                    val wavHeader = createWavHeader(fileSize, SAMPLE_RATE, CHANNELS, BIT_DEPTH)
                    output.write(wavHeader)

                    // Read PCM data in chunks
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            return wavFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error converting PCM to WAV", e)
            return null
        }
    }

    private fun createWavHeader(
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ): ByteArray {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitDepth / 8
        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen.toInt())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) 
        header.putShort(1.toShort()) 
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitDepth / 8).toShort())
        header.putShort(bitDepth.toShort())
        header.put("data".toByteArray())
        header.putInt(totalAudioLen.toInt())
        return header.array()
    }

    fun getVocalWavFilePath(): String? {
        return vocalWavFile?.absolutePath
    }

    fun getBgmWavFilePath(): String? {
        return bgmWavFile?.absolutePath
    }

    fun getCapturedWavFilePath(): String? {
        return wavFile?.absolutePath
    }
}