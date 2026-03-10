package com.max.voiceassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.max.voiceassistant.R
import com.max.voiceassistant.data.AppSettings
import com.max.voiceassistant.model.ACMode
import com.max.voiceassistant.data.DialogRepository
import com.max.voiceassistant.data.VehicleStateRepository
import com.max.voiceassistant.databinding.ActivityMainBinding
import com.max.voiceassistant.model.*
import com.max.voiceassistant.ui.adapter.DialogAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 主界面 Activity。
 *
 * 负责：标题栏、对话列表、车辆状态卡片、语音输入区、快捷命令、设置与关于弹窗；
 * 监听 ViewModel 状态并更新 UI，处理录音权限与麦克风点击。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** 车辆状态仓库 */
    private val vehicleStateRepository by lazy { VehicleStateRepository() }
    /** 对话历史仓库 */
    private val dialogRepository by lazy { DialogRepository() }
    /** 应用设置（Mock/真实模式等） */
    private val appSettings by lazy { AppSettings(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            applicationContext, vehicleStateRepository, dialogRepository
        )
    }

    /** 对话列表适配器 */
    private lateinit var dialogAdapter: DialogAdapter

    /** 录音权限请求 Launcher */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // 权限已授予，开始录音
            startVoiceRecognition()
        } else {
            // 权限被拒绝
            Toast.makeText(this, getString(R.string.permission_need_record), Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        setupClickListeners()

        // 显示模式提示
        showModeInfo()
    }

    /**
     * 显示当前运行模式提示。
     * 若为模拟模式则 Toast 提示用户可点击麦克风后使用快捷命令测试。
     */
    private fun showModeInfo() {
        if (viewModel.isMockMode()) {
            Toast.makeText(this, getString(R.string.mode_info_mock), Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 初始化 UI：对话列表、布局与适配器。
     */
    private fun setupUI() {
        dialogAdapter = DialogAdapter()
        binding.dialogRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = dialogAdapter
        }
    }
    
    /**
     * 订阅 ViewModel 的各类 StateFlow，驱动 UI 更新。
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.acState.collectLatest { acState ->
                updateACUI(acState)
            }
        }
        
        // 观察车门状态
        lifecycleScope.launch {
            viewModel.doorState.collectLatest { doorState ->
                updateDoorUI(doorState)
            }
        }
        
        // 观察引擎状态
        lifecycleScope.launch {
            viewModel.engineState.collectLatest { engineState ->
                updateEngineUI(engineState)
            }
        }
        
        // 观察对话历史
        lifecycleScope.launch {
            viewModel.dialogHistory.collectLatest { dialogs ->
                dialogAdapter.submitList(dialogs.toList()) {
                    if (dialogs.isNotEmpty()) {
                        binding.dialogRecyclerView.smoothScrollToPosition(dialogs.size - 1)
                    }
                }
            }
        }
        
        // 观察识别状态
        lifecycleScope.launch {
            viewModel.recognitionState.collectLatest { state ->
                updateRecognitionStateUI(state)
            }
        }
        
        // 观察识别文本
        lifecycleScope.launch {
            viewModel.recognizedText.collectLatest { text ->
                updateRecognizedTextUI(text)
            }
        }

        // 观察音量（用于波形动画）
        lifecycleScope.launch {
            viewModel.volume.collectLatest { volume ->
                updateVolumeUI(volume)
            }
        }
        
        // 观察命令执行结果
        lifecycleScope.launch {
            viewModel.lastCommandResult.collectLatest { result ->
                updateFeedbackUI(result)
            }
        }

        // 观察TTS状态
        lifecycleScope.launch {
            viewModel.isSpeaking.collectLatest { isSpeaking ->
                updateSpeakingUI(isSpeaking)
            }
        }
    }
    
    /**
     * 绑定麦克风、快捷命令 Chip、设置按钮的点击事件。
     */
    private fun setupClickListeners() {
        binding.fabMicrophone.setOnClickListener {
            onMicrophoneClicked()
        }
        
        // 快捷命令 - 模拟模式下直接执行
        binding.chipPlayMusic.setOnClickListener {
            processQuickCommand(getString(R.string.main_quick_play_music))
        }
        
        binding.chipAC.setOnClickListener {
            processQuickCommand(getString(R.string.main_quick_open_ac))
        }
        
        binding.chipTime.setOnClickListener {
            processQuickCommand(getString(R.string.main_quick_what_time))
        }
        
        // 设置按钮
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    /**
     * 将快捷命令文本交给 ViewModel 执行。
     *
     * @param text 快捷命令文案（如「播放音乐」「打开空调」）
     */
    private fun processQuickCommand(text: String) {
        viewModel.processUserInput(text)
    }
    
    /**
     * 根据当前识别状态处理麦克风点击：空闲/错误时请求权限并开始，聆听时停止，处理中不响应。
     */
    private fun onMicrophoneClicked() {
        val currentState = viewModel.recognitionState.value
        when (currentState) {
            RecognitionState.IDLE -> checkPermissionAndStart()
            RecognitionState.LISTENING -> viewModel.stopListening()
            RecognitionState.RECOGNIZING, RecognitionState.PROCESSING -> { }
            RecognitionState.ERROR -> checkPermissionAndStart()
        }
    }

    /**
     * 检查录音权限：已有则直接开始；需说明则弹窗后请求；否则直接请求。
     */
    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                AlertDialog.Builder(this).setTitle(R.string.permission_record_title)
                    .setMessage(R.string.permission_record_message)
                    .setPositiveButton(R.string.permission_grant) { _, _ ->
                        requestPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECORD_AUDIO)
                        )
                    }.setNegativeButton(R.string.common_cancel, null).show()
            }

            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                )
            }
        }
    }
    
    /**
     * 调用 ViewModel 开始语音识别。
     */
    private fun startVoiceRecognition() {
        viewModel.startListening()
    }

    /**
     * 显示设置对话框：语音模式、清空历史、关于。
     */
    private fun showSettingsDialog() {
        val currentMode = if (appSettings.useMockMode) getString(R.string.settings_mode_mock) else getString(R.string.settings_mode_real)
        val items = arrayOf(
            getString(R.string.settings_voice_mode, currentMode),
            getString(R.string.settings_clear_history),
            getString(R.string.settings_about)
        )

        AlertDialog.Builder(this).setTitle(R.string.settings_title).setItems(items) { _, which ->
                when (which) {
                    0 -> showModeSelectionDialog()
                    1 -> {
                        viewModel.clearDialog()
                        Toast.makeText(this, getString(R.string.settings_history_cleared), Toast.LENGTH_SHORT).show()
                    }
                    2 -> showAboutDialog()
                }
            }.show()
    }
    
    /**
     * 显示语音模式选择（模拟 / 真实），切换后需重启生效。
     */
    private fun showModeSelectionDialog() {
        val modes = arrayOf(getString(R.string.settings_mode_mock_label), getString(R.string.settings_mode_real_label))
        val currentIndex = if (appSettings.useMockMode) 0 else 1
        
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_select_mode_title)
            .setSingleChoiceItems(modes, currentIndex) { dialog, which ->
                val newMockMode = (which == 0)
                if (newMockMode != appSettings.useMockMode) {
                    appSettings.useMockMode = newMockMode
                    val modeName = if (newMockMode) getString(R.string.settings_mode_mock) else getString(R.string.settings_mode_real)
                    Toast.makeText(this, getString(R.string.settings_switched_mode, modeName), Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }
    
    /**
     * 显示关于：应用名、当前模式、功能列表与提示。
     */
    private fun showAboutDialog() {
        val modeText = if (viewModel.isMockMode()) getString(R.string.settings_mode_mock) else getString(R.string.settings_mode_real_label)
        AlertDialog.Builder(this).setTitle(R.string.about_title).setMessage(
            getString(R.string.about_message, modeText)
        ).setPositiveButton(R.string.common_ok, null).show()
    }
    
    // ---------- UI 更新 ----------

    /**
     * 取空调模式的本地化名称。
     *
     * @param mode 空调模式
     * @return 对应 string 资源文案
     */
    private fun getAcModeName(mode: ACMode): String = getString(
        when (mode) {
            ACMode.AUTO -> R.string.ac_mode_auto
            ACMode.COOL -> R.string.ac_mode_cool
            ACMode.HEAT -> R.string.ac_mode_heat
        }
    )
    
    /**
     * 根据空调状态更新图标、颜色与文案。
     *
     * @param acState 当前空调状态
     */
    private fun updateACUI(acState: ACState) {
        if (acState.isOn) {
            when (acState.mode) {
                ACMode.HEAT -> {
                    binding.iconAC.setImageResource(R.drawable.ic_heat)
                    binding.iconAC.setColorFilter(getColor(R.color.accent_orange))
                }
                ACMode.COOL -> {
                    binding.iconAC.setImageResource(R.drawable.ic_ac)
                    binding.iconAC.setColorFilter(getColor(R.color.accent_blue))
                }
                ACMode.AUTO -> {
                    binding.iconAC.setImageResource(R.drawable.ic_ac)
                    binding.iconAC.setColorFilter(getColor(R.color.accent_green))
                }
            }
        } else {
            binding.iconAC.setImageResource(R.drawable.ic_ac)
            binding.iconAC.setColorFilter(getColor(R.color.text_secondary))
        }
        binding.tvACStatus.text = if (acState.isOn) {
            getString(R.string.main_ac_on, getAcModeName(acState.mode))
        } else {
            getString(R.string.main_ac_off)
        }
        binding.tvTemperature.text = "${acState.temperature}°C"
    }

    /**
     * 根据车门状态更新锁图标颜色与锁定/未锁定文案。
     *
     * @param doorState 车门状态
     */
    private fun updateDoorUI(doorState: DoorState) {
        val iconColor = if (doorState.isLocked) {
            getColor(R.color.accent_green)
        } else {
            getColor(R.color.accent_orange)
        }
        binding.iconDoor.setColorFilter(iconColor)
        binding.tvDoorStatus.text = if (doorState.isLocked) getString(R.string.main_door_locked) else getString(R.string.main_door_unlocked)
    }

    /**
     * 根据引擎状态更新图标颜色与运行中/熄火文案。
     *
     * @param engineState 引擎状态
     */
    private fun updateEngineUI(engineState: EngineState) {
        val iconColor = if (engineState.isRunning) {
            getColor(R.color.accent_green)
        } else {
            getColor(R.color.text_secondary)
        }
        binding.iconEngine.setColorFilter(iconColor)
        binding.tvEngineStatus.text = if (engineState.isRunning) getString(R.string.main_engine_running) else getString(R.string.main_engine_off)
    }

    /**
     * 根据识别状态更新提示文案与麦克风按钮可用性。
     *
     * @param state 当前识别状态
     */
    private fun updateRecognitionStateUI(state: RecognitionState) {
        when (state) {
            RecognitionState.IDLE -> {
                binding.tvRecognitionStatus.text = getString(R.string.main_recognition_tap_to_speak)
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                binding.fabMicrophone.isEnabled = true
            }

            RecognitionState.LISTENING -> {
                binding.tvRecognitionStatus.text = getString(R.string.main_recognition_listening)
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                binding.fabMicrophone.isEnabled = true
            }

            RecognitionState.RECOGNIZING -> {
                binding.tvRecognitionStatus.text = getString(R.string.main_recognition_recognizing)
                binding.fabMicrophone.isEnabled = false
            }

            RecognitionState.PROCESSING -> {
                binding.tvRecognitionStatus.text = getString(R.string.main_recognition_processing)
                binding.fabMicrophone.isEnabled = false
            }

            RecognitionState.ERROR -> {
                binding.tvRecognitionStatus.text = getString(R.string.main_recognition_error)
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                binding.fabMicrophone.isEnabled = true
            }
        }
    }

    /**
     * 显示或隐藏识别到的文字。
     *
     * @param text 识别结果，空则隐藏
     */
    private fun updateRecognizedTextUI(text: String) {
        if (text.isNotEmpty()) {
            binding.tvRecognizedText.visibility = View.VISIBLE
            binding.tvRecognizedText.text = text
        } else {
            binding.tvRecognizedText.visibility = View.GONE
        }
    }

    /**
     * 根据音量更新麦克风按钮透明度（可扩展为波形动画）。
     *
     * @param volume 音量 0–100
     */
    private fun updateVolumeUI(volume: Int) {
        if (volume > 0) {
            val alpha = 0.5f + (volume / 100f) * 0.5f
            binding.fabMicrophone.alpha = alpha.coerceIn(0.5f, 1f)
        } else {
            binding.fabMicrophone.alpha = 1f
        }
    }

    /**
     * 显示命令执行结果卡片：成功蓝、失败红、需权限橙；NeedPermission 时触发权限请求。
     *
     * @param result 执行结果，null 则隐藏卡片
     */
    private fun updateFeedbackUI(result: CommandResult?) {
        if (result != null) {
            binding.feedbackCard.visibility = View.VISIBLE
            when (result) {
                is CommandResult.Success -> {
                    binding.feedbackCard.setCardBackgroundColor(getColor(R.color.accent_blue))
                    binding.iconFeedback.setImageResource(R.drawable.ic_check_circle)
                    binding.tvFeedback.text = result.message
                }

                is CommandResult.Error -> {
                    binding.feedbackCard.setCardBackgroundColor(getColor(R.color.accent_red))
                    binding.iconFeedback.setImageResource(R.drawable.ic_check_circle)
                    binding.tvFeedback.text = result.message
                }

                is CommandResult.NeedPermission -> {
                    binding.feedbackCard.setCardBackgroundColor(getColor(R.color.accent_orange))
                    binding.iconFeedback.setImageResource(R.drawable.ic_check_circle)
                    binding.tvFeedback.text = result.message
                    requestPermissionLauncher.launch(arrayOf(result.permission))
                }
            }
        } else {
            binding.feedbackCard.visibility = View.GONE
        }
    }

    /**
     * TTS 播放状态 UI（可扩展：如显示小喇叭图标）。
     *
     * @param isSpeaking 是否正在播报
     */
    private fun updateSpeakingUI(isSpeaking: Boolean) {
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
