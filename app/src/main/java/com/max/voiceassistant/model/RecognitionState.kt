package com.max.voiceassistant.model

/**
 * 语音识别状态
 */
enum class RecognitionState {
    IDLE,           // 空闲
    LISTENING,      // 正在录音
    RECOGNIZING,    // 正在识别
    PROCESSING,     // 正在处理
    ERROR           // 错误
}

