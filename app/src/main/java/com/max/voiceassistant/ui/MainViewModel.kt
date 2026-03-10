package com.max.voiceassistant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.max.voiceassistant.R
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
 * 主界面 ViewModel。
 *
 * 职责：维护车辆状态、对话历史、识别状态、命令执行结果与 TTS 状态；
 * 协调意图解析与命令执行，对接 [VoiceAssistantManager] 的识别与播报。
 */
class MainViewModel(
    private val context: Context,
    private val vehicleStateRepository: VehicleStateRepository,
    private val dialogRepository: DialogRepository
) : ViewModel() {

    private val intentParser = IntentParser()
    private val commandExecutor = CommandExecutor(context, vehicleStateRepository)
    private val voiceManager = VoiceAssistantManager(context)

    val vehicleState: StateFlow<VehicleState> = vehicleStateRepository.vehicleState
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

    val dialogHistory: StateFlow<List<DialogMessage>> = dialogRepository.dialogHistory

    private val _recognitionState = MutableStateFlow(RecognitionState.IDLE)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()
    /** 当前音量 0–100，供 UI 做波形等展示 */
    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initVoiceManager()
    }

    /**
     * 初始化语音管理器并注册识别、TTS 回调，更新状态与结果。
     */
    private fun initVoiceManager() {
        voiceManager.init()
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
                _lastCommandResult.value = CommandResult.Error(context.getString(R.string.cmd_recognition_failed, errorMessage))
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _recognitionState.value = RecognitionState.IDLE
                    _lastCommandResult.value = null
                }
            }
        })
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
     * 开始语音识别；若正在 TTS 播报则先停止。
     */
    fun startListening() {
        if (_isSpeaking.value) {
            voiceManager.stopSpeaking()
        }
        
        _recognitionState.value = RecognitionState.LISTENING
        _recognizedText.value = ""
        voiceManager.startListening()
    }

    /**
     * 停止语音识别（结束本次录音）。
     */
    fun stopListening() {
        voiceManager.stopListening()
    }

    /**
     * 取消本次语音识别并重置状态。
     */
    fun cancelListening() {
        voiceManager.cancelListening()
        _recognitionState.value = RecognitionState.IDLE
        _recognizedText.value = ""
        _volume.value = 0
    }

    /**
     * 处理用户输入：写入对话、解析意图、执行命令、写回复、TTS 播报、更新结果与状态，3 秒后清除反馈。
     *
     * @param text 用户输入（识别结果或快捷命令文案）
     */
    fun processUserInput(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            dialogRepository.addUserMessage(text)
            _recognitionState.value = RecognitionState.PROCESSING
            val command = intentParser.parse(text)
            val result = commandExecutor.execute(command)
            val responseText = when (result) {
                is CommandResult.Success -> result.message
                is CommandResult.Error -> result.message
                is CommandResult.NeedPermission -> result.message
            }
            dialogRepository.addAssistantMessage(responseText)
            _lastCommandResult.value = result
            voiceManager.speak(responseText)
            _recognitionState.value = RecognitionState.IDLE
            kotlinx.coroutines.delay(3000)
            _lastCommandResult.value = null
        }
    }

    /**
     * 设置识别状态（供外部同步用）。
     *
     * @param state 目标状态
     */
    fun updateRecognitionState(state: RecognitionState) {
        _recognitionState.value = state
    }

    /**
     * 外部注入识别结果并触发处理流程。
     *
     * @param text 识别到的文字
     */
    fun onRecognitionResult(text: String) {
        _recognizedText.value = text
        processUserInput(text)
    }

    /** 停止 TTS 播报。 */
    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    /** 清空对话历史。 */
    fun clearDialog() {
        dialogRepository.clearHistory()
    }

    /** 当前是否正在语音识别。 */
    fun isListening(): Boolean = voiceManager.isListening()

    /** 当前是否为模拟模式（Mock）。 */
    fun isMockMode(): Boolean = voiceManager.isMockMode()

    /** 获取车辆状态仓库，供需要直接读写车辆状态的调用方使用。 */
    fun getVehicleStateRepository(): VehicleStateRepository = vehicleStateRepository

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }

    /**
     * 用于创建 [MainViewModel] 的 Factory，注入 Context 与 Repository。
     *
     * @param context 应用上下文
     * @param vehicleStateRepository 车辆状态仓库
     * @param dialogRepository 对话历史仓库
     */
    class Factory(
        private val context: Context,
        private val vehicleStateRepository: VehicleStateRepository,
        private val dialogRepository: DialogRepository
    ) : ViewModelProvider.Factory {
        
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                MainViewModel(context, vehicleStateRepository, dialogRepository) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
