package com.max.voiceassistant.speech

/**
 * 百度语音 SDK 配置。
 *
 * 使用前需在百度 AI 开放平台创建应用，将 APP_ID、API_KEY、SECRET_KEY 填入下方；
 * [isConfigValid] 用于判断是否已配置，未配置时 [VoiceAssistantManager] 走 Mock 模式。
 */
object SpeechConfig {

    /**
     * 百度开放平台应用 ID。
     *
     * 公开仓库请勿提交真实凭证；请在本地替换为你自己的值。
     */
    const val APP_ID = "your_app_id"

    /**
     * 百度开放平台 API Key。
     *
     * 公开仓库请勿提交真实凭证；请在本地替换为你自己的值。
     */
    const val API_KEY = "your_api_key"

    /**
     * 百度开放平台 Secret Key。
     *
     * 公开仓库请勿提交真实凭证；请在本地替换为你自己的值。
     */
    const val SECRET_KEY = "your_secret_key"

    /** ASR 识别参数：采样率、语言、VAD 等。 */
    object ASR {
        const val SAMPLE_RATE = 16000
        const val LANGUAGE = 1537
        const val ACCEPT_AUDIO_DATA = true
        const val ACCEPT_AUDIO_VOLUME = true
        const val VAD_ENDPOINT_TIMEOUT = 2000
        const val MAX_RECORD_TIME = 60000
    }

    /** TTS 合成参数：发音人、语速、音调、音量、编码。 */
    object TTS {
        const val SPEAKER = 0
        const val SPEED = 5
        const val PITCH = 5
        const val VOLUME = 9
        const val AUDIO_ENCODE = 3
    }

    /**
     * 判断是否已填写有效的 API 凭证（非占位符）。
     *
     * @return true 表示可走真实模式
     */
    fun isConfigValid(): Boolean {
        return APP_ID != "your_app_id" &&
                API_KEY != "your_api_key" &&
                SECRET_KEY != "your_secret_key"
    }

    /** 配置状态描述，用于提示用户。 */
    fun getConfigStatus(): String {
        return if (isConfigValid()) {
            "百度语音 SDK 配置有效"
        } else {
            "请先配置百度语音 SDK 的 APP_ID、API_KEY、SECRET_KEY"
        }
    }
}



