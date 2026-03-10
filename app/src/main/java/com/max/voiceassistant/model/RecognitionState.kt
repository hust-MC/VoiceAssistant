package com.max.voiceassistant.model

/**
 * 语音识别流程状态。
 *
 * 用于 UI 展示与麦克风按钮行为：空闲可开始、聆听可停止、识别/处理中禁用、错误可重试。
 */
enum class RecognitionState {
    /** 空闲，可开始录音 */
    IDLE,
    /** 正在录音 */
    LISTENING,
    /** 正在识别 */
    RECOGNIZING,
    /** 正在处理命令 */
    PROCESSING,
    /** 识别失败，可重试 */
    ERROR
}

