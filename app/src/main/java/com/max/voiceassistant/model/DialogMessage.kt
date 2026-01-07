package com.max.voiceassistant.model

/**
 * 对话消息
 */
sealed class DialogMessage {
    abstract val text: String
    abstract val timestamp: Long
    
    data class User(
        override val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DialogMessage()
    
    data class Assistant(
        override val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : DialogMessage()
}

