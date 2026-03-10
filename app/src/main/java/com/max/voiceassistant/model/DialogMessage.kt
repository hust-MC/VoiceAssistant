package com.max.voiceassistant.model

/**
 * 单条对话消息（用户或助手）。
 *
 * @param text 消息正文
 * @param timestamp 时间戳，用于 DiffUtil 与排序
 */
sealed class DialogMessage {
    abstract val text: String
    abstract val timestamp: Long

    /** 用户发送的消息。 */
    data class User(
        override val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DialogMessage()

    /** 助手回复的消息。 */
    data class Assistant(
        override val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DialogMessage()
}

