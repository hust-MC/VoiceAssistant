package com.max.voiceassistant.data

import com.max.voiceassistant.model.DialogMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 对话历史Repository
 * 管理对话消息列表
 */
class DialogRepository {
    
    private val _dialogHistory = MutableStateFlow<List<DialogMessage>>(emptyList())
    val dialogHistory: StateFlow<List<DialogMessage>> = _dialogHistory.asStateFlow()
    
    /**
     * 添加用户消息
     */
    fun addUserMessage(text: String) {
        _dialogHistory.value = _dialogHistory.value + DialogMessage.User(text)
    }
    
    /**
     * 添加助手消息
     */
    fun addAssistantMessage(text: String) {
        _dialogHistory.value = _dialogHistory.value + DialogMessage.Assistant(text)
    }
    
    /**
     * 添加消息
     */
    fun addMessage(message: DialogMessage) {
        _dialogHistory.value = _dialogHistory.value + message
    }
    
    /**
     * 获取对话历史
     */
    fun getHistory(): List<DialogMessage> = _dialogHistory.value
    
    /**
     * 清空历史
     */
    fun clearHistory() {
        _dialogHistory.value = emptyList()
    }
    
    /**
     * 获取最后一条消息
     */
    fun getLastMessage(): DialogMessage? = _dialogHistory.value.lastOrNull()
}
