package com.max.voiceassistant.speech

import android.content.Context
import android.util.Log
import com.baidu.speech.EventListener
import com.baidu.speech.EventManager
import com.baidu.speech.EventManagerFactory
import com.baidu.speech.asr.SpeechConstant
import org.json.JSONObject

/**
 * 百度语音识别管理器
 * 
 * 封装百度语音识别SDK，提供简单的API接口
 * 基于 baidu_speech_ASR_V3 SDK
 */
class SpeechRecognizerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechRecognizer"
    }
    
    // 百度语音识别事件管理器
    private var asr: EventManager? = null
    
    // 是否已初始化
    private var isInitialized = false
    
    // 是否正在识别
    private var isListening = false
    
    // 回调监听器
    private var listener: RecognitionListener? = null
    
    /**
     * 识别结果回调接口
     */
    interface RecognitionListener {
        /** 准备就绪 */
        fun onReady()
        
        /** 开始录音 */
        fun onBegin()
        
        /** 音量变化 (0-100) */
        fun onVolumeChanged(volume: Int)
        
        /** 实时识别结果（部分结果） */
        fun onPartialResult(partialResult: String)
        
        /** 最终识别结果 */
        fun onResult(result: String)
        
        /** 识别结束 */
        fun onEnd()
        
        /** 识别错误 */
        fun onError(errorCode: Int, errorMessage: String)
    }
    
    /**
     * 百度SDK事件监听器
     */
    private val eventListener = EventListener { name, params, data, offset, length ->
        Log.d(TAG, "Event: $name, params: $params")
        
        when (name) {
            SpeechConstant.CALLBACK_EVENT_ASR_READY -> {
                // 引擎准备就绪
                listener?.onReady()
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_BEGIN -> {
                // 开始录音
                isListening = true
                listener?.onBegin()
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_VOLUME -> {
                // 音量变化
                try {
                    val json = JSONObject(params)
                    val volumePercent = json.optInt("volume-percent", 0)
                    listener?.onVolumeChanged(volumePercent)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse volume error", e)
                }
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL -> {
                // 识别结果回调（包含临时结果和最终结果）
                try {
                    if (params.isNullOrEmpty()) return@EventListener
                    
                    when {
                        params.contains("\"partial_result\"") -> {
                            // 临时识别结果
                            val json = JSONObject(params)
                            val resultsArray = json.optJSONArray("results_recognition")
                            if (resultsArray != null && resultsArray.length() > 0) {
                                val partialResult = resultsArray.optString(0, "")
                                if (partialResult.isNotEmpty()) {
                                    listener?.onPartialResult(partialResult)
                                }
                            }
                        }
                        params.contains("\"final_result\"") -> {
                            // 最终识别结果
                            val json = JSONObject(params)
                            val resultsArray = json.optJSONArray("results_recognition")
                            if (resultsArray != null && resultsArray.length() > 0) {
                                val result = resultsArray.optString(0, "")
                                if (result.isNotEmpty()) {
                                    listener?.onResult(result)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse result error", e)
                }
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_FINISH -> {
                // 识别结束
                isListening = false
                listener?.onEnd()
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_END -> {
                // 录音结束
                isListening = false
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_EXIT -> {
                // 识别引擎退出
                isListening = false
            }
            
            SpeechConstant.CALLBACK_EVENT_ASR_ERROR -> {
                // 识别错误
                isListening = false
                try {
                    val json = JSONObject(params)
                    val errorCode = json.optInt("error", -1)
                    val errorMessage = json.optString("desc", "未知错误")
                    listener?.onError(errorCode, errorMessage)
                } catch (e: Exception) {
                    listener?.onError(-1, "解析错误信息失败")
                }
            }
        }
    }
    
    /**
     * 初始化语音识别
     */
    fun init() {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return
        }
        
        try {
            // 创建事件管理器
            asr = EventManagerFactory.create(context, "asr")
            asr?.registerListener(eventListener)
            isInitialized = true
            Log.d(TAG, "Speech recognizer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
        }
    }
    
    /**
     * 设置识别监听器
     */
    fun setListener(listener: RecognitionListener) {
        this.listener = listener
    }
    
    /**
     * 开始语音识别
     */
    fun startListening() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized, call init() first")
            listener?.onError(-1, "语音识别未初始化")
            return
        }
        
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        // 构建识别参数
        val params = buildRecognitionParams()
        
        // 发送开始识别事件
        val json = JSONObject(params).toString()
        asr?.send(SpeechConstant.ASR_START, json, null, 0, 0)
        Log.d(TAG, "Start listening with params: $json")
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (!isListening) {
            return
        }
        
        asr?.send(SpeechConstant.ASR_STOP, null, null, 0, 0)
        Log.d(TAG, "Stop listening")
    }
    
    /**
     * 取消语音识别
     */
    fun cancel() {
        asr?.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0)
        isListening = false
        Log.d(TAG, "Cancel listening")
    }
    
    /**
     * 释放资源
     */
    fun release() {
        cancel()
        asr?.unregisterListener(eventListener)
        asr = null
        isInitialized = false
        isListening = false
        Log.d(TAG, "Released")
    }
    
    /**
     * 是否正在识别
     */
    fun isListening(): Boolean = isListening
    
    /**
     * 构建识别参数
     */
    private fun buildRecognitionParams(): Map<String, Any> {
        val params = mutableMapOf<String, Any>()
        
        // 百度API认证信息
        params[SpeechConstant.APP_ID] = SpeechConfig.APP_ID
        params[SpeechConstant.APP_KEY] = SpeechConfig.API_KEY
        params[SpeechConstant.SECRET] = SpeechConfig.SECRET_KEY
        
        // PID：识别模型（必须设置！）
        // 1537 = 普通话输入法模型（有标点）
        // 15372 = 普通话输入法模型（无标点）
        // 1737 = 英语
        // 1637 = 粤语
        params[SpeechConstant.PID] = SpeechConfig.ASR.LANGUAGE
        
        // VAD 模式：使用手动模式（touch），不依赖本地 VAD 模型
        // VAD_TOUCH = 手动停止模式，需要调用 stop() 结束识别
        // VAD_DNN = 自动端点检测，需要本地模型文件
        params[SpeechConstant.VAD] = SpeechConstant.VAD_TOUCH
        
        // 是否回调音量
        params[SpeechConstant.ACCEPT_AUDIO_VOLUME] = true
        
        // 是否回调音频数据
        params[SpeechConstant.ACCEPT_AUDIO_DATA] = false
        
        // VAD静音检测超时（毫秒）
        // 用户停止说话后多久结束识别
        params[SpeechConstant.VAD_ENDPOINT_TIMEOUT] = SpeechConfig.ASR.VAD_ENDPOINT_TIMEOUT
        
        return params
    }
}
