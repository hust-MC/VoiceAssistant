package com.max.voiceassistant.speech

import android.content.Context
import android.util.Log
import com.baidu.tts.client.SpeechSynthesizer
import com.baidu.tts.client.SpeechSynthesizerListener
import com.baidu.tts.client.SynthesizerResponse
import com.baidu.tts.client.TtsEntity
import com.baidu.tts.client.TtsMode

/**
 * 百度语音合成（TTS）管理器
 * 
 * 封装百度语音合成SDK 6.2.7+，提供简单的文字转语音功能
 */
class TTSManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    // 百度TTS合成器
    private var synthesizer: SpeechSynthesizer? = null
    
    // 是否已初始化
    private var isInitialized = false
    
    // 是否正在播放
    private var isSpeaking = false
    
    // 回调监听器
    private var listener: TTSListener? = null
    
    /**
     * TTS回调接口
     */
    interface TTSListener {
        fun onSynthesizeStart(utteranceId: String)
        fun onSpeechStart(utteranceId: String)
        fun onSpeechProgress(utteranceId: String, progress: Int)
        fun onSpeechFinish(utteranceId: String)
        fun onError(utteranceId: String, errorMessage: String)
    }
    
    /**
     * 简单的监听器适配器
     */
    abstract class SimpleTTSListener : TTSListener {
        override fun onSynthesizeStart(utteranceId: String) {}
        override fun onSpeechStart(utteranceId: String) {}
        override fun onSpeechProgress(utteranceId: String, progress: Int) {}
        override fun onSpeechFinish(utteranceId: String) {}
        override fun onError(utteranceId: String, errorMessage: String) {}
    }
    
    /**
     * 百度TTS监听器（适配SDK 6.2.7+）
     */
    private val synthesizerListener = object : SpeechSynthesizerListener {
        
        override fun onSynthesizeResponse(response: SynthesizerResponse?) {
            if (response == null) {
                Log.w(TAG, "Response is null")
                return
            }
            
            val utteranceId = response.utteranceId ?: ""
            val synthesizeType = response.synthesizeType
            val error = response.synthesizerError
            
            // 如果有错误
            if (error != null) {
                val errorMsg = error.description ?: "未知错误 (code: ${error.code})"
                Log.e(TAG, "TTS Error: $errorMsg")
                isSpeaking = false
                listener?.onError(utteranceId, errorMsg)
                return
            }
            
            // 根据响应类型分发事件
            when (synthesizeType) {
                SynthesizerResponse.SynthesizeType.SYNTHESIZE_START -> {
                    Log.d(TAG, "onSynthesizeStart: $utteranceId")
                    listener?.onSynthesizeStart(utteranceId)
                }
                
                SynthesizerResponse.SynthesizeType.PLAY_START -> {
                    Log.d(TAG, "onSpeechStart: $utteranceId")
                    isSpeaking = true
                    listener?.onSpeechStart(utteranceId)
                }
                
                SynthesizerResponse.SynthesizeType.PLAY_PROGRESS -> {
                    val data = response.synthesizerData
                    val progress = ((data?.audioPercent ?: 0f) * 100).toInt()
                    listener?.onSpeechProgress(utteranceId, progress)
                }
                
                SynthesizerResponse.SynthesizeType.PLAY_FINISH -> {
                    Log.d(TAG, "onSpeechFinish: $utteranceId")
                    isSpeaking = false
                    listener?.onSpeechFinish(utteranceId)
                }
                
                SynthesizerResponse.SynthesizeType.SYNTHESIZE_FINISH -> {
                    Log.d(TAG, "onSynthesizeFinish: $utteranceId")
                }
                
                SynthesizerResponse.SynthesizeType.SYNTHESIZE_ERROR -> {
                    val errorMsg = error?.description ?: "合成错误"
                    Log.e(TAG, "Synthesize Error: $errorMsg")
                    isSpeaking = false
                    listener?.onError(utteranceId, errorMsg)
                }
                
                else -> {
                    Log.d(TAG, "Other response type: $synthesizeType")
                }
            }
        }
    }
    
    /**
     * 初始化TTS
     */
    fun init(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }
        
        try {
            // 创建合成器实例（SDK 6.2.7+ 使用构造函数）
            synthesizer = SpeechSynthesizer(context)
            
            // 设置API认证信息
            synthesizer?.setParam(SpeechSynthesizer.PARAM_APP_ID, SpeechConfig.APP_ID)
            synthesizer?.setParam(SpeechSynthesizer.PARAM_API_KEY, SpeechConfig.API_KEY)
            synthesizer?.setParam(SpeechSynthesizer.PARAM_SECRET_KEY, SpeechConfig.SECRET_KEY)
            
            // 设置监听器
            synthesizer?.setSpeechSynthesizerListener(synthesizerListener)
            
            // 设置合成参数
            setupParams()
            
            // 初始化引擎（加载在线TTS）
            val result = synthesizer?.loadOnlineTts()
            if (result == null || result.detailCode == 0) {
                isInitialized = true
                Log.d(TAG, "TTS initialized successfully")
                return true
            } else {
                val errorMsg = result.detailMessage ?: "未知错误"
                Log.e(TAG, "TTS init failed: $errorMsg (code: ${result.detailCode})")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS init error", e)
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 设置合成参数
     */
    private fun setupParams() {
        // 发音人（使用 PARAM_ONLINE_SPEAKER）
        synthesizer?.setParam(SpeechSynthesizer.PARAM_ONLINE_SPEAKER, SpeechConfig.TTS.SPEAKER.toString())
        
        // 语速
        synthesizer?.setParam(SpeechSynthesizer.PARAM_SPEED, SpeechConfig.TTS.SPEED.toString())
        
        // 音调
        synthesizer?.setParam(SpeechSynthesizer.PARAM_PITCH, SpeechConfig.TTS.PITCH.toString())
        
        // 音量
        synthesizer?.setParam(SpeechSynthesizer.PARAM_VOLUME, SpeechConfig.TTS.VOLUME.toString())
    }
    
    /**
     * 设置监听器
     */
    fun setListener(listener: TTSListener?) {
        this.listener = listener
    }
    
    /**
     * 播放文本
     */
    fun speak(text: String): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized")
            return false
        }
        
        if (text.isBlank()) {
            Log.w(TAG, "Text is empty")
            return false
        }
        
        Log.d(TAG, "Speaking: $text")
        
        try {
            val ttsEntity = TtsEntity(text, TtsMode.ONLINE)
            val result = synthesizer?.speak(ttsEntity)
            if (result == null || result.detailCode == 0) {
                return true
            } else {
                Log.e(TAG, "Speak failed: ${result.detailMessage} (code: ${result.detailCode})")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Speak error", e)
            return false
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause(): Boolean {
        val result = synthesizer?.pause()
        return result == 0
    }
    
    /**
     * 恢复播放
     */
    fun resume(): Boolean {
        val result = synthesizer?.resume()
        return result == 0
    }
    
    /**
     * 停止播放
     */
    fun stop(): Boolean {
        isSpeaking = false
        val result = synthesizer?.stop()
        return result == 0
    }
    
    /**
     * 是否正在播放
     */
    fun isSpeaking(): Boolean = isSpeaking
    
    /**
     * 设置发音人
     */
    fun setSpeaker(speaker: String) {
        synthesizer?.setParam(SpeechSynthesizer.PARAM_ONLINE_SPEAKER, speaker)
    }
    
    /**
     * 设置语速
     */
    fun setSpeed(speed: String) {
        synthesizer?.setParam(SpeechSynthesizer.PARAM_SPEED, speed)
    }
    
    /**
     * 设置音量
     */
    fun setVolume(volume: String) {
        synthesizer?.setParam(SpeechSynthesizer.PARAM_VOLUME, volume)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stop()
        synthesizer?.release()
        synthesizer = null
        isInitialized = false
        Log.d(TAG, "TTS released")
    }
}
