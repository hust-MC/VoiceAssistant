package com.max.voiceassistant.speech

import android.content.Context
import android.util.Log

/**
 * 语音助手管理器
 * 
 * 统一管理语音识别和语音合成功能
 * 自动检测百度SDK是否可用，不可用时使用模拟模式
 */
class VoiceAssistantManager(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceAssistantManager"
        
        // 是否使用模拟模式（没有百度SDK时设为true）
        // TODO: 百度SDK认证通过后改为false
        const val USE_MOCK_MODE = true  // 暂时使用Mock模式，等百度平台配置生效后改回false
    }
    
    // 模拟模式管理器
    private var mockManager: MockSpeechManager? = null
    
    // 百度SDK管理器（真实模式）
    private var speechRecognizer: SpeechRecognizerManager? = null
    private var ttsManager: TTSManager? = null
    
    // 是否已初始化
    private var isInitialized = false
    
    // 回调
    private var recognitionCallback: RecognitionCallback? = null
    private var ttsCallback: TTSCallback? = null
    
    /**
     * 语音识别回调
     */
    interface RecognitionCallback {
        fun onReady()
        fun onBegin()
        fun onVolumeChanged(volume: Int)
        fun onPartialResult(text: String)
        fun onResult(text: String)
        fun onEnd()
        fun onError(errorCode: Int, errorMessage: String)
    }
    
    /**
     * TTS回调
     */
    interface TTSCallback {
        fun onSpeakStart()
        fun onSpeakFinish()
        fun onError(message: String)
    }
    
    /**
     * 初始化语音功能
     */
    fun init(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }
        
        return if (USE_MOCK_MODE || !SpeechConfig.isConfigValid()) {
            initMockMode()
        } else {
            initRealMode()
        }
    }
    
    /**
     * 初始化模拟模式
     */
    private fun initMockMode(): Boolean {
        Log.d(TAG, "Initializing in MOCK mode")
        
        mockManager = MockSpeechManager(context).apply {
            init()
            
            // 设置识别回调
            setRecognitionListener(object : SpeechRecognizerManager.RecognitionListener {
                override fun onReady() {
                    recognitionCallback?.onReady()
                }
                
                override fun onBegin() {
                    recognitionCallback?.onBegin()
                }
                
                override fun onVolumeChanged(volume: Int) {
                    recognitionCallback?.onVolumeChanged(volume)
                }
                
                override fun onPartialResult(partialResult: String) {
                    recognitionCallback?.onPartialResult(partialResult)
                }
                
                override fun onResult(result: String) {
                    recognitionCallback?.onResult(result)
                }
                
                override fun onEnd() {
                    recognitionCallback?.onEnd()
                }
                
                override fun onError(errorCode: Int, errorMessage: String) {
                    recognitionCallback?.onError(errorCode, errorMessage)
                }
            })
            
            // 设置TTS回调
            setTTSListener(object : TTSManager.TTSListener {
                override fun onSynthesizeStart(utteranceId: String) {}
                
                override fun onSpeechStart(utteranceId: String) {
                    ttsCallback?.onSpeakStart()
                }
                
                override fun onSpeechProgress(utteranceId: String, progress: Int) {}
                
                override fun onSpeechFinish(utteranceId: String) {
                    ttsCallback?.onSpeakFinish()
                }
                
                override fun onError(utteranceId: String, errorMessage: String) {
                    ttsCallback?.onError(errorMessage)
                }
            })
        }
        
        isInitialized = true
        Log.d(TAG, "Mock mode initialized")
        return true
    }
    
    /**
     * 初始化真实模式（百度SDK）
     */
    private fun initRealMode(): Boolean {
        Log.d(TAG, "Initializing in REAL mode (Baidu SDK)")
        
        try {
            // 初始化语音识别
            speechRecognizer = SpeechRecognizerManager(context).apply {
                init()
                setListener(object : SpeechRecognizerManager.RecognitionListener {
                    override fun onReady() {
                        recognitionCallback?.onReady()
                    }
                    
                    override fun onBegin() {
                        recognitionCallback?.onBegin()
                    }
                    
                    override fun onVolumeChanged(volume: Int) {
                        recognitionCallback?.onVolumeChanged(volume)
                    }
                    
                    override fun onPartialResult(partialResult: String) {
                        recognitionCallback?.onPartialResult(partialResult)
                    }
                    
                    override fun onResult(result: String) {
                        recognitionCallback?.onResult(result)
                    }
                    
                    override fun onEnd() {
                        recognitionCallback?.onEnd()
                    }
                    
                    override fun onError(errorCode: Int, errorMessage: String) {
                        recognitionCallback?.onError(errorCode, errorMessage)
                    }
                })
            }
            
            // 初始化TTS
            ttsManager = TTSManager(context).apply {
                if (!init()) {
                    Log.e(TAG, "TTS init failed")
                }
                setListener(object : TTSManager.TTSListener {
                    override fun onSynthesizeStart(utteranceId: String) {}
                    
                    override fun onSpeechStart(utteranceId: String) {
                        ttsCallback?.onSpeakStart()
                    }
                    
                    override fun onSpeechProgress(utteranceId: String, progress: Int) {}
                    
                    override fun onSpeechFinish(utteranceId: String) {
                        ttsCallback?.onSpeakFinish()
                    }
                    
                    override fun onError(utteranceId: String, errorMessage: String) {
                        ttsCallback?.onError(errorMessage)
                    }
                })
            }
            
            isInitialized = true
            Log.d(TAG, "Real mode initialized")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Real mode init failed, fallback to mock mode", e)
            return initMockMode()
        }
    }
    
    /**
     * 设置识别回调
     */
    fun setRecognitionCallback(callback: RecognitionCallback) {
        this.recognitionCallback = callback
    }
    
    /**
     * 设置TTS回调
     */
    fun setTTSCallback(callback: TTSCallback) {
        this.ttsCallback = callback
    }
    
    /**
     * 开始语音识别
     */
    fun startListening() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            recognitionCallback?.onError(-1, "语音功能未初始化")
            return
        }
        
        if (USE_MOCK_MODE || mockManager != null) {
            mockManager?.startListening()
        } else {
            speechRecognizer?.startListening()
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (USE_MOCK_MODE || mockManager != null) {
            mockManager?.stopListening()
        } else {
            speechRecognizer?.stopListening()
        }
    }
    
    /**
     * 取消语音识别
     */
    fun cancelListening() {
        if (USE_MOCK_MODE || mockManager != null) {
            mockManager?.cancelListening()
        } else {
            speechRecognizer?.cancel()
        }
    }
    
    /**
     * 语音合成（文字转语音）
     */
    fun speak(text: String) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized")
            return
        }
        
        if (text.isBlank()) {
            Log.w(TAG, "Text is empty")
            return
        }
        
        Log.d(TAG, "Speak: $text")
        
        if (USE_MOCK_MODE || mockManager != null) {
            mockManager?.speak(text)
        } else {
            ttsManager?.speak(text)
        }
    }
    
    /**
     * 停止TTS播放
     */
    fun stopSpeaking() {
        if (USE_MOCK_MODE || mockManager != null) {
            mockManager?.stopSpeaking()
        } else {
            ttsManager?.stop()
        }
    }
    
    /**
     * 是否正在识别
     */
    fun isListening(): Boolean {
        return if (USE_MOCK_MODE || mockManager != null) {
            mockManager?.isListening() ?: false
        } else {
            speechRecognizer?.isListening() ?: false
        }
    }
    
    /**
     * 模拟识别结果（仅Mock模式可用，用于测试）
     */
    fun simulateRecognition(text: String) {
        mockManager?.simulateRecognitionResult(text)
    }
    
    /**
     * 是否是模拟模式
     */
    fun isMockMode(): Boolean = USE_MOCK_MODE || mockManager != null
    
    /**
     * 释放资源
     */
    fun release() {
        mockManager?.release()
        mockManager = null
        
        speechRecognizer?.release()
        speechRecognizer = null
        
        ttsManager?.release()
        ttsManager = null
        
        isInitialized = false
        Log.d(TAG, "Released")
    }
}


