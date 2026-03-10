package com.max.voiceassistant.speech

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.max.voiceassistant.R
import java.util.*

/**
 * 模拟语音管理器。
 *
 * 无百度 SDK 时使用：识别为固定流程（Ready→Begin→音量→2 秒后固定结果→End），
 * TTS 使用系统 [TextToSpeech]，便于开发与测试。
 */
class MockSpeechManager(private val context: Context) {

    companion object {
        private const val TAG = "MockSpeechManager"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var systemTTS: TextToSpeech? = null
    private var isTTSReady = false
    private var isListening = false
    private var recognitionListener: SpeechRecognizerManager.RecognitionListener? = null
    private var ttsListener: TTSManager.TTSListener? = null

    /**
     * 初始化系统 TTS，设置中文。
     */
    fun init() {
        systemTTS = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = systemTTS?.setLanguage(Locale.CHINESE)
                isTTSReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
                Log.d(TAG, "System TTS initialized, ready: $isTTSReady")
            } else {
                Log.e(TAG, "System TTS init failed")
            }
        }
    }

    /** 设置识别回调，与 [SpeechRecognizerManager.RecognitionListener] 一致。 */
    fun setRecognitionListener(listener: SpeechRecognizerManager.RecognitionListener) {
        this.recognitionListener = listener
    }

    /** 设置 TTS 回调。 */
    fun setTTSListener(listener: TTSManager.TTSListener) {
        this.ttsListener = listener
    }

    /**
     * 开始模拟识别：依次回调 Ready、Begin、模拟音量、2 秒后固定结果、End。
     */
    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        isListening = true
        Log.d(TAG, "Mock: Start listening")
        handler.post {
            recognitionListener?.onReady()
        }

        handler.postDelayed({
            if (isListening) {
                recognitionListener?.onBegin()
            }
        }, 200)
        simulateVolumeChanges()
    }

    /**
     * 模拟音量变化
     */
    private fun simulateVolumeChanges() {
        var volume = 0
        val runnable = object : Runnable {
            override fun run() {
                if (isListening) {
                    volume = (Math.random() * 100).toInt()
                    recognitionListener?.onVolumeChanged(volume)
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.postDelayed(runnable, 300)
    }

    /**
     * 停止模拟识别并返回结果
     */
    fun stopListening(): String {
        if (!isListening) {
            return ""
        }

        isListening = false
        Log.d(TAG, "Mock: Stop listening")

        // 返回模拟结果
        val mockResults = listOf(
            "播放音乐",
            "打开空调",
            "温度调高",
            "下一首",
            "现在几点",
            "打开蓝牙",
            "音量调大"
        )
        val result = mockResults.random()

        handler.post {
            recognitionListener?.onPartialResult(result)
            recognitionListener?.onResult(result)
            recognitionListener?.onEnd()
        }

        return result
    }

    /**
     * 模拟输入指定文本（用于测试）
     */
    fun simulateRecognitionResult(text: String) {
        Log.d(TAG, "Mock: Simulate result: $text")
        handler.post {
            recognitionListener?.onReady()
        }
        handler.postDelayed({
            recognitionListener?.onBegin()
        }, 100)
        handler.postDelayed({
            recognitionListener?.onPartialResult(text)
            recognitionListener?.onResult(text)
            recognitionListener?.onEnd()
        }, 500)
    }

    /**
     * 取消识别
     */
    fun cancelListening() {
        isListening = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Mock: Cancel listening")
    }

    /**
     * 语音合成（使用系统TTS）
     */
    fun speak(text: String, utteranceId: String = System.currentTimeMillis().toString()) {
        Log.d(TAG, "Mock TTS: $text")

        if (!isTTSReady) {
            Log.w(TAG, "System TTS not ready, skip speaking")
            // 即使TTS不可用，也模拟回调
            handler.post {
                ttsListener?.onSpeechStart(utteranceId)
            }
            handler.postDelayed({
                ttsListener?.onSpeechFinish(utteranceId)
            }, 1000)
            return
        }

        // 设置监听器
        systemTTS?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) {
                handler.post {
                    ttsListener?.onSpeechStart(utteranceId)
                }
            }

            override fun onDone(id: String?) {
                handler.post {
                    ttsListener?.onSpeechFinish(utteranceId)
                }
            }

            override fun onError(id: String?) {
                handler.post {
                    ttsListener?.onError(utteranceId, context.getString(R.string.speech_tts_error))
                }
            }
        })

        // 播放
        val params = android.os.Bundle()
        systemTTS?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * 停止TTS
     */
    fun stopSpeaking() {
        systemTTS?.stop()
    }

    /**
     * 是否正在识别
     */
    fun isListening(): Boolean = isListening

    /**
     * 释放资源
     */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        systemTTS?.stop()
        systemTTS?.shutdown()
        systemTTS = null
        isListening = false
        Log.d(TAG, "Mock speech manager released")
    }
}





