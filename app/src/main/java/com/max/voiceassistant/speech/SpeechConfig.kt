package com.max.voiceassistant.speech

/**
 * 百度语音 SDK 配置
 *
 * 使用前需要：
 * 1. 到百度 AI 开放平台注册：https://ai.baidu.com/
 * 2. 创建应用，获取 App ID、API Key、Secret Key
 * 3. 填入下方对应的常量中
 */
object SpeechConfig {

    // ==================== 百度 API 认证配置 ====================
    // TODO: 请替换为你自己的百度 API 凭证
    const val APP_ID = "your_app_id"
    const val API_KEY = "your_api_key"
    const val SECRET_KEY = "your_secret_key"

    // ==================== ASR 语音识别配置 ====================
    object ASR {
        // 采样率：16000 或 8000
        const val SAMPLE_RATE = 16000

        // 语言：普通话 (1537)、英语 (1737)、粤语 (1637)
        const val LANGUAGE = 1537

        // 是否返回中间结果（边说边识别）
        const val ACCEPT_AUDIO_DATA = true

        // 是否返回语音音量
        const val ACCEPT_AUDIO_VOLUME = true

        // 静音超时（毫秒），用户停止说话后多久结束识别
        const val VAD_ENDPOINT_TIMEOUT = 2000

        // 最长录音时间（毫秒）
        const val MAX_RECORD_TIME = 60000
    }

    // ==================== TTS 语音合成配置 ====================
    object TTS {
        // 发音人：0-普通女声, 1-普通男声, 3-情感男声, 4-情感女声
        // 度小宇=1, 度小美=0, 度逍遥=3, 度丫丫=4
        const val SPEAKER = 0

        // 语速：0-15，默认 5
        const val SPEED = 5

        // 音调：0-15，默认 5
        const val PITCH = 5

        // 音量：0-15，默认 5
        const val VOLUME = 9

        // 音频格式：3-mp3, 4-pcm-16k, 5-pcm-8k, 6-wav
        const val AUDIO_ENCODE = 3
    }

    // ==================== 检查配置是否有效 ====================
    /**
     * 检查 API 配置是否已填写
     * @return true 表示配置有效，false 表示需要填写配置
     */
    fun isConfigValid(): Boolean {
        return APP_ID != "your_app_id" &&
                API_KEY != "your_api_key" &&
                SECRET_KEY != "your_secret_key"
    }

    /**
     * 获取配置状态描述
     */
    fun getConfigStatus(): String {
        return if (isConfigValid()) {
            "百度语音 SDK 配置有效"
        } else {
            "请先配置百度语音 SDK 的 APP_ID、API_KEY、SECRET_KEY"
        }
    }
}

