package com.max.voiceassistant.data

import com.max.voiceassistant.model.DialogMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 对话历史仓库。
 *
 * 维护用户与助手的消息列表，通过 [dialogHistory] 对外暴露 [StateFlow]，供 UI 订阅。
 */
class DialogRepository {

    private val _dialogHistory = MutableStateFlow<List<DialogMessage>>(emptyList())
    val dialogHistory: StateFlow<List<DialogMessage>> = _dialogHistory.asStateFlow()

    /**
     * 追加一条用户消息。
     *
     * @param text 用户输入内容
     */
    fun addUserMessage(text: String) {
        _dialogHistory.value = _dialogHistory.value + DialogMessage.User(text)
    }

    /**
     * 追加一条助手回复。
     *
     * @param text 助手回复内容
     */
    fun addAssistantMessage(text: String) {
        _dialogHistory.value = _dialogHistory.value + DialogMessage.Assistant(text)
    }

    /**
     * 追加任意一条对话消息。
     *
     * @param message 用户或助手消息
     */
    fun addMessage(message: DialogMessage) {
        _dialogHistory.value = _dialogHistory.value + message
    }

    /** 当前对话列表快照。 */
    fun getHistory(): List<DialogMessage> = _dialogHistory.value

    /** 清空所有对话记录。 */
    fun clearHistory() {
        _dialogHistory.value = emptyList()
    }

    /** 最后一条消息，无则 null。 */
    fun getLastMessage(): DialogMessage? = _dialogHistory.value.lastOrNull()
}
