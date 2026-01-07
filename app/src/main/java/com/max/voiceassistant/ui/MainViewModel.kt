package com.max.voiceassistant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.max.voiceassistant.data.DialogRepository
import com.max.voiceassistant.data.VehicleStateRepository
import com.max.voiceassistant.executor.CommandExecutor
import com.max.voiceassistant.intent.IntentParser
import com.max.voiceassistant.model.*
import com.max.voiceassistant.speech.VoiceAssistantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 主界面ViewModel
 */
class MainViewModel(
    private val context: Context,
    private val vehicleStateRepository: VehicleStateRepository,
    private val dialogRepository: DialogRepository
) : ViewModel() {
    
    // 意图识别器
    private val intentParser = IntentParser()
    
    // 命令执行器
    private val commandExecutor = CommandExecutor(context, vehicleStateRepository)
    
    // 语音助手管理器
    private val voiceManager = VoiceAssistantManager(context)
    
    // ========== 车辆状态 ==========
    
    val vehicleState: StateFlow<VehicleState> = vehicleStateRepository.vehicleState
    
    // 为UI提供便捷的状态访问
    val acState: StateFlow<ACState> = vehicleState
        .map { it.ac }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = vehicleState.value.ac
        )
    
    val doorState: StateFlow<DoorState> = vehicleState
        .map { it.door }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = vehicleState.value.door
        )
    
    val engineState: StateFlow<EngineState> = vehicleState
        .map { it.engine }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = vehicleState.value.engine
        )
    
    // ========== 对话历史 ==========
    
    val dialogHistory: StateFlow<List<DialogMessage>> = dialogRepository.dialogHistory
    
    // ========== 语音识别状态 ==========
    
    private val _recognitionState = MutableStateFlow(RecognitionState.IDLE)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    
    // 音量（用于显示波形动画）
    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()
    
    // ========== 命令执行结果 ==========
    
    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()
    
    // ========== TTS状态 ==========
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    init {
        // 初始化语音管理器
        initVoiceManager()
    }
    
    /**
     * 初始化语音管理器
     */
    private fun initVoiceManager() {
        voiceManager.init()
        
        // 设置识别回调
        voiceManager.setRecognitionCallback(object : VoiceAssistantManager.RecognitionCallback {
            override fun onReady() {
                _recognitionState.value = RecognitionState.LISTENING
            }
            
            override fun onBegin() {
                _recognitionState.value = RecognitionState.LISTENING
                _recognizedText.value = ""
            }
            
            override fun onVolumeChanged(volume: Int) {
                _volume.value = volume
            }
            
            override fun onPartialResult(text: String) {
                _recognizedText.value = text
            }
            
            override fun onResult(text: String) {
                _recognizedText.value = text
                _recognitionState.value = RecognitionState.RECOGNIZING
                // 处理识别结果
                processUserInput(text)
            }
            
            override fun onEnd() {
                if (_recognitionState.value != RecognitionState.PROCESSING) {
                    _recognitionState.value = RecognitionState.IDLE
                }
                _volume.value = 0
            }
            
            override fun onError(errorCode: Int, errorMessage: String) {
                _recognitionState.value = RecognitionState.ERROR
                _lastCommandResult.value = CommandResult.Error("识别失败: $errorMessage")
                
                // 3秒后恢复
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _recognitionState.value = RecognitionState.IDLE
                    _lastCommandResult.value = null
                }
            }
        })
        
        // 设置TTS回调
        voiceManager.setTTSCallback(object : VoiceAssistantManager.TTSCallback {
            override fun onSpeakStart() {
                _isSpeaking.value = true
            }
            
            override fun onSpeakFinish() {
                _isSpeaking.value = false
            }
            
            override fun onError(message: String) {
                _isSpeaking.value = false
            }
        })
    }
    
    /**
     * 开始语音识别
     */
    fun startListening() {
        // 如果正在播放TTS，先停止
        if (_isSpeaking.value) {
            voiceManager.stopSpeaking()
        }
        
        _recognitionState.value = RecognitionState.LISTENING
        _recognizedText.value = ""
        voiceManager.startListening()
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        voiceManager.stopListening()
    }
    
    /**
     * 取消语音识别
     */
    fun cancelListening() {
        voiceManager.cancelListening()
        _recognitionState.value = RecognitionState.IDLE
        _recognizedText.value = ""
        _volume.value = 0
    }
    
    /**
     * 处理用户输入（语音识别结果或手动输入）
     */
    fun processUserInput(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            // 1. 添加用户消息
            dialogRepository.addUserMessage(text)
            
            // 2. 更新状态为处理中
            _recognitionState.value = RecognitionState.PROCESSING
            
            // 3. 解析意图
            val command = intentParser.parse(text)
            
            // 4. 执行命令
            val result = commandExecutor.execute(command)
            
            // 5. 获取响应文本
            val responseText = when (result) {
                is CommandResult.Success -> result.message
                is CommandResult.Error -> result.message
            }
            
            // 6. 添加助手回复
            dialogRepository.addAssistantMessage(responseText)
            
            // 7. 更新命令执行结果
            _lastCommandResult.value = result
            
            // 8. TTS播报结果
            voiceManager.speak(responseText)
            
            // 9. 恢复空闲状态
            _recognitionState.value = RecognitionState.IDLE
            
            // 10. 3秒后清除反馈
            kotlinx.coroutines.delay(3000)
            _lastCommandResult.value = null
        }
    }
    
    /**
     * 更新识别状态（供外部调用）
     */
    fun updateRecognitionState(state: RecognitionState) {
        _recognitionState.value = state
    }
    
    /**
     * 处理语音识别结果（供外部调用）
     */
    fun onRecognitionResult(text: String) {
        _recognizedText.value = text
        processUserInput(text)
    }
    
    /**
     * 停止TTS播放
     */
    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }
    
    /**
     * 清空对话历史
     */
    fun clearDialog() {
        dialogRepository.clearHistory()
    }
    
    /**
     * 是否正在识别
     */
    fun isListening(): Boolean = voiceManager.isListening()
    
    /**
     * 是否是模拟模式
     */
    fun isMockMode(): Boolean = voiceManager.isMockMode()
    
    /**
     * 获取VehicleStateRepository（供外部直接调用）
     */
    fun getVehicleStateRepository(): VehicleStateRepository = vehicleStateRepository
    
    /**
     * 释放资源
     */
    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
    
    /**
     * ViewModel Factory
     */
    class Factory(
        private val context: Context,
        private val vehicleStateRepository: VehicleStateRepository,
        private val dialogRepository: DialogRepository
    ) : ViewModelProvider.Factory {
        
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(context, vehicleStateRepository, dialogRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
