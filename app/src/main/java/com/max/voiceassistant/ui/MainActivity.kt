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
 * 主界面Activity
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    // Repository实例
    private val vehicleStateRepository by lazy { VehicleStateRepository() }
    private val dialogRepository by lazy { DialogRepository() }
    
    // 设置管理
    private val appSettings by lazy { AppSettings(applicationContext) }
    
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            applicationContext, vehicleStateRepository, dialogRepository
        )
    }
    
    private lateinit var dialogAdapter: DialogAdapter

    // 权限请求
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
     * 显示当前运行模式
     */
    private fun showModeInfo() {
        if (viewModel.isMockMode()) {
            Toast.makeText(this, getString(R.string.mode_info_mock), Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 初始化UI
     */
    private fun setupUI() {
        // 设置对话列表
        dialogAdapter = DialogAdapter()
        binding.dialogRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true // 新消息显示在底部
            }
            adapter = dialogAdapter
        }
    }
    
    /**
     * 设置状态观察
     */
    private fun setupObservers() {
        // 观察空调状态
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
                // 使用带回调的 submitList，确保列表更新完成后再滚动
                dialogAdapter.submitList(dialogs.toList()) {
                // 滚动到底部
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
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 麦克风按钮
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
     * 处理快捷命令
     */
    private fun processQuickCommand(text: String) {
        viewModel.processUserInput(text)
    }
    
    /**
     * 麦克风按钮点击
     */
    private fun onMicrophoneClicked() {
        val currentState = viewModel.recognitionState.value
        
        when (currentState) {
            RecognitionState.IDLE -> {
                // 检查权限后开始录音
                checkPermissionAndStart()
            }

            RecognitionState.LISTENING -> {
                // 停止录音
                viewModel.stopListening()
            }

            RecognitionState.RECOGNIZING, RecognitionState.PROCESSING -> {
                // 处理中，不操作
            }

            RecognitionState.ERROR -> {
                // 重试
                checkPermissionAndStart()
            }
        }
    }

    /**
     * 检查权限并开始录音
     */
    private fun checkPermissionAndStart() {
        when {
            // 已有权限
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }

            // 需要解释为什么需要权限
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                AlertDialog.Builder(this).setTitle(R.string.permission_record_title)
                    .setMessage(R.string.permission_record_message)
                    .setPositiveButton(R.string.permission_grant) { _, _ ->
                        requestPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECORD_AUDIO)
                        )
                    }.setNegativeButton(R.string.common_cancel, null).show()
            }

            // 直接请求权限
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                )
            }
        }
    }
    
    /**
     * 开始语音识别
     */
    private fun startVoiceRecognition() {
        viewModel.startListening()
    }

    /**
     * 显示设置对话框
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
     * 显示模式选择对话框
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
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        val modeText = if (viewModel.isMockMode()) getString(R.string.settings_mode_mock) else getString(R.string.settings_mode_real_label)
        AlertDialog.Builder(this).setTitle(R.string.about_title).setMessage(
            getString(R.string.about_message, modeText)
        ).setPositiveButton(R.string.common_ok, null).show()
    }
    
    // ========== UI更新方法 ==========

    private fun getAcModeName(mode: ACMode): String = getString(
        when (mode) {
            ACMode.AUTO -> R.string.ac_mode_auto
            ACMode.COOL -> R.string.ac_mode_cool
            ACMode.HEAT -> R.string.ac_mode_heat
        }
    )
    
    private fun updateACUI(acState: ACState) {
        // 根据空调模式更新图标和颜色
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
        
        // 更新空调状态文字
        binding.tvACStatus.text = if (acState.isOn) {
            getString(R.string.main_ac_on, getAcModeName(acState.mode))
        } else {
            getString(R.string.main_ac_off)
        }
        
        // 更新温度显示
        binding.tvTemperature.text = "${acState.temperature}°C"
    }
    
    private fun updateDoorUI(doorState: DoorState) {
        // 更新车门图标颜色
        val iconColor = if (doorState.isLocked) {
            getColor(R.color.accent_green)
        } else {
            getColor(R.color.accent_orange)
        }
        binding.iconDoor.setColorFilter(iconColor)
        
        // 更新车门状态文字
        binding.tvDoorStatus.text = if (doorState.isLocked) getString(R.string.main_door_locked) else getString(R.string.main_door_unlocked)
    }
    
    private fun updateEngineUI(engineState: EngineState) {
        // 更新引擎图标颜色
        val iconColor = if (engineState.isRunning) {
            getColor(R.color.accent_green)
        } else {
            getColor(R.color.text_secondary)
        }
        binding.iconEngine.setColorFilter(iconColor)
        
        // 更新引擎状态文字
        binding.tvEngineStatus.text = if (engineState.isRunning) getString(R.string.main_engine_running) else getString(R.string.main_engine_off)
    }
    
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
    
    private fun updateRecognizedTextUI(text: String) {
        if (text.isNotEmpty()) {
            binding.tvRecognizedText.visibility = View.VISIBLE
            binding.tvRecognizedText.text = text
        } else {
            binding.tvRecognizedText.visibility = View.GONE
        }
    }

    private fun updateVolumeUI(volume: Int) {
        // 音量更新（可用于未来扩展波形动画）
        // 目前通过麦克风按钮的视觉效果来表示音量
        if (volume > 0) {
            // 可以根据音量调整麦克风按钮的视觉反馈
            val alpha = 0.5f + (volume / 100f) * 0.5f
            binding.fabMicrophone.alpha = alpha.coerceIn(0.5f, 1f)
        } else {
            binding.fabMicrophone.alpha = 1f
        }
    }
    
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
                    // 请求权限
                    requestPermissionLauncher.launch(arrayOf(result.permission))
                }
            }
        } else {
            binding.feedbackCard.visibility = View.GONE
        }
    }

    private fun updateSpeakingUI(isSpeaking: Boolean) {
        // 可以添加TTS播放状态的视觉提示
        // 例如：显示一个小喇叭图标表示正在播报
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel会自动释放语音资源
    }
}
