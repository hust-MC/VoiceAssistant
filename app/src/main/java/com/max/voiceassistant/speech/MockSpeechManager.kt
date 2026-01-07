package com.max.voiceassistant.speech

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * 模拟语音管理器
 * 
 * 在没有百度SDK的情况下，使用Android系统自带的语音功能进行模拟
 * 用于开发和测试阶段
 */
class MockSpeechManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MockSpeechManager"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    // 系统TTS（用于模拟语音合成）
    private var systemTTS: TextToSpeech? = null
    private var isTTSReady = false
    
    // 模拟识别状态
    private var isListening = false
    
    // 回调
    private var recognitionListener: SpeechRecognizerManager.RecognitionListener? = null
    private var ttsListener: TTSManager.TTSListener? = null
    
    /**
     * 初始化
     */
    fun init() {
        // 初始化系统TTS
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
    
    /**
     * 设置识别监听器
     */
    fun setRecognitionListener(listener: SpeechRecognizerManager.RecognitionListener) {
        this.recognitionListener = listener
    }
    
    /**
     * 设置TTS监听器
     */
    fun setTTSListener(listener: TTSManager.TTSListener) {
        this.ttsListener = listener
    }
    
    /**
     * 开始模拟语音识别
     * 
     * 模拟流程：
     * 1. 触发onReady
     * 2. 触发onBegin
     * 3. 模拟音量变化
     * 4. 2秒后触发onResult（返回模拟结果）
     * 5. 触发onEnd
     */
    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        isListening = true
        Log.d(TAG, "Mock: Start listening")
        
        // 模拟识别流程
        handler.post {
            recognitionListener?.onReady()
        }
        
        handler.postDelayed({
            if (isListening) {
                recognitionListener?.onBegin()
            }
        }, 200)
        
        // 模拟音量变化
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
                    ttsListener?.onError(utteranceId, "TTS播放错误")
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



