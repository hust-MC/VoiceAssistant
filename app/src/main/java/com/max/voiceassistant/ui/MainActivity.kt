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
            Toast.makeText(this, "需要录音权限才能使用语音功能", Toast.LENGTH_LONG).show()
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
            Toast.makeText(
                this, "当前为模拟模式\n点击麦克风后点击快捷命令进行测试", Toast.LENGTH_LONG
            ).show()
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
                dialogAdapter.submitList(dialogs)
                // 滚动到底部
                if (dialogs.isNotEmpty()) {
                    binding.dialogRecyclerView.scrollToPosition(dialogs.size - 1)
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
            processQuickCommand("播放音乐")
        }

        binding.chipAC.setOnClickListener {
            processQuickCommand("打开空调")
        }

        binding.chipTime.setOnClickListener {
            processQuickCommand("现在几点")
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
                AlertDialog.Builder(this).setTitle("需要录音权限")
                    .setMessage("语音助手需要录音权限才能识别您的语音指令")
                    .setPositiveButton("授权") { _, _ ->
                        requestPermissionLauncher.launch(
                            arrayOf(Manifest.permission.RECORD_AUDIO)
                        )
                    }.setNegativeButton("取消", null).show()
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
        val items = arrayOf(
            "清空对话历史", "关于"
        )

        AlertDialog.Builder(this).setTitle("设置").setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.clearDialog()
                        Toast.makeText(this, "对话历史已清空", Toast.LENGTH_SHORT).show()
                    }

                    1 -> {
                        showAboutDialog()
                    }
                }
            }.show()
    }

    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        val modeText = if (viewModel.isMockMode()) "模拟模式" else "真实模式（百度SDK）"

        AlertDialog.Builder(this).setTitle("关于").setMessage(
                """
                车载语音助手 Demo
                
                当前运行模式：$modeText
                
                功能：
                • 语音识别（ASR）
                • 语音合成（TTS）
                • 媒体控制
                • 车辆控制（Mock）
                • 系统控制
                • 信息查询
                
                提示：集成百度SDK后可使用真实语音识别
            """.trimIndent()
            ).setPositiveButton("确定", null).show()
    }

    // ========== UI更新方法 ==========

    private fun updateACUI(acState: ACState) {
        // 更新空调图标颜色
        val iconColor = if (acState.isOn) {
            getColor(R.color.accent_blue)
        } else {
            getColor(R.color.text_secondary)
        }
        binding.iconAC.setColorFilter(iconColor)

        // 更新空调状态文字
        binding.tvACStatus.text = if (acState.isOn) {
            "空调：开 ${acState.mode.displayName}"
        } else {
            "空调：关"
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
        binding.tvDoorStatus.text = if (doorState.isLocked) "已锁定" else "未锁定"
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
        binding.tvEngineStatus.text = if (engineState.isRunning) "运行中" else "熄火"
    }

    private fun updateRecognitionStateUI(state: RecognitionState) {
        when (state) {
            RecognitionState.IDLE -> {
                binding.tvRecognitionStatus.text = "点击麦克风开始说话"
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                binding.fabMicrophone.isEnabled = true
            }

            RecognitionState.LISTENING -> {
                binding.tvRecognitionStatus.text = "正在聆听..."
                binding.fabMicrophone.setImageResource(R.drawable.ic_mic)
                binding.fabMicrophone.isEnabled = true
            }

            RecognitionState.RECOGNIZING -> {
                binding.tvRecognitionStatus.text = "识别中..."
                binding.fabMicrophone.isEnabled = false
            }

            RecognitionState.PROCESSING -> {
                binding.tvRecognitionStatus.text = "处理中..."
                binding.fabMicrophone.isEnabled = false
            }

            RecognitionState.ERROR -> {
                binding.tvRecognitionStatus.text = "识别失败，点击重试"
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
