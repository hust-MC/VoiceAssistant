# 车辆控制Mock实现与UI状态更新详解

## 📋 目录
1. [整体架构设计](#整体架构设计)
2. [数据模型设计](#数据模型设计)
3. [状态管理机制](#状态管理机制)
4. [状态更新流程](#状态更新流程)
5. [UI监听与更新](#ui监听与更新)
6. [完整代码示例](#完整代码示例)

---

## 整体架构设计

### 架构图

```
┌─────────────────────────────────────────┐
│         UI Layer                        │
│  - VehicleControlPanelFragment          │
│  - 各种车辆状态显示组件                  │
└─────────────────────────────────────────┘
              ↓ (观察状态)
┌─────────────────────────────────────────┐
│      ViewModel Layer                    │
│  - VehicleControlViewModel              │
│    └── 暴露StateFlow供UI观察             │
└─────────────────────────────────────────┘
              ↓ (管理状态)
┌─────────────────────────────────────────┐
│    Repository Layer                     │
│  - VehicleStateRepository               │
│    └── 持有MutableStateFlow（状态源）    │
└─────────────────────────────────────────┘
              ↓ (更新状态)
┌─────────────────────────────────────────┐
│   Executor Layer                        │
│  - VehicleControlExecutor               │
│    └── 执行命令并更新状态                │
└─────────────────────────────────────────┘
```

---

## 数据模型设计

### 1. 车辆状态数据结构

```kotlin
// 完整的车辆状态数据类
data class VehicleState(
    // 空调状态
    val ac: ACState = ACState(),
    
    // 座椅状态
    val seat: SeatState = SeatState(),
    
    // 车窗状态
    val window: WindowState = WindowState(),
    
    // 灯光状态
    val light: LightState = LightState(),
    
    // 车门状态
    val door: DoorState = DoorState(),
    
    // 引擎状态
    val engine: EngineState = EngineState()
)

// 空调状态
data class ACState(
    val isOn: Boolean = false,
    val temperature: Int = 24,  // 16-32度
    val fanSpeed: Int = 3,      // 1-5档
    val mode: ACMode = ACMode.AUTO
) {
    // 提供copy方法用于不可变更新
    fun copy(
        isOn: Boolean = this.isOn,
        temperature: Int = this.temperature,
        fanSpeed: Int = this.fanSpeed,
        mode: ACMode = this.mode
    ) = ACState(isOn, temperature, fanSpeed, mode)
}

enum class ACMode {
    AUTO,   // 自动
    COOL,   // 制冷
    HEAT    // 制热
}

// 座椅状态
data class SeatState(
    val position: Int = 3,  // 1-5档，3是中间位置
    val heating: Boolean = false,
    val ventilation: Boolean = false
)

// 车窗状态
data class WindowState(
    val frontLeft: Int = 0,   // 0-100%，0是完全关闭
    val frontRight: Int = 0,
    val rearLeft: Int = 0,
    val rearRight: Int = 0,
    val sunroof: Boolean = false
)

// 灯光状态
data class LightState(
    val headlight: Boolean = false,
    val headlightMode: HeadlightMode = HeadlightMode.AUTO,
    val ambientLight: Boolean = false,
    val ambientColor: String = "白色"
)

enum class HeadlightMode {
    OFF,    // 关闭
    ON,     // 开启
    AUTO    // 自动
}

// 车门状态
data class DoorState(
    val isLocked: Boolean = true,
    val trunkOpen: Boolean = false
)

// 引擎状态
data class EngineState(
    val isRunning: Boolean = false
)
```

---

## 状态管理机制

### 1. Repository层 - 状态存储中心

```kotlin
/**
 * 车辆状态仓库
 * 职责：管理车辆状态数据的存储和更新
 */
class VehicleStateRepository {
    
    // 核心：MutableStateFlow作为状态源
    // 使用private val确保只能通过Repository的方法更新
    private val _vehicleState = MutableStateFlow(VehicleState())
    
    // 对外暴露只读的StateFlow，UI层只能观察，不能修改
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()
    
    // 获取当前状态（同步获取，不需要Flow）
    fun getCurrentState(): VehicleState {
        return _vehicleState.value
    }
    
    // 更新整个车辆状态
    fun updateState(newState: VehicleState) {
        _vehicleState.value = newState
    }
    
    // 部分更新：只更新空调状态
    fun updateACState(acState: ACState) {
        _vehicleState.value = _vehicleState.value.copy(
            ac = acState
        )
    }
    
    // 部分更新：只更新座椅状态
    fun updateSeatState(seatState: SeatState) {
        _vehicleState.value = _vehicleState.value.copy(
            seat = seatState
        )
    }
    
    // 部分更新：只更新车窗状态
    fun updateWindowState(windowState: WindowState) {
        _vehicleState.value = _vehicleState.value.copy(
            window = windowState
        )
    }
    
    // 部分更新：只更新灯光状态
    fun updateLightState(lightState: LightState) {
        _vehicleState.value = _vehicleState.value.copy(
            light = lightState
        )
    }
    
    // 部分更新：只更新车门状态
    fun updateDoorState(doorState: DoorState) {
        _vehicleState.value = _vehicleState.value.copy(
            door = doorState
        )
    }
    
    // 部分更新：只更新引擎状态
    fun updateEngineState(engineState: EngineState) {
        _vehicleState.value = _vehicleState.value.copy(
            engine = engineState
        )
    }
    
    // 重置所有状态到初始值
    fun resetState() {
        _vehicleState.value = VehicleState()
    }
}
```

**关键点说明**：
1. **MutableStateFlow**：可变的StateFlow，用于存储状态
2. **asStateFlow()**：转换为只读的StateFlow，防止外部直接修改
3. **不可变更新**：使用`copy()`方法创建新对象，而不是修改原对象
4. **单一数据源**：所有状态更新都通过Repository进行

---

### 2. Executor层 - 命令执行与状态更新

```kotlin
/**
 * 车辆控制执行器
 * 职责：解析命令，执行Mock操作，更新状态
 */
class VehicleControlExecutor(
    private val stateRepository: VehicleStateRepository,
    private val ttsManager: TTSManager
) {
    
    /**
     * 执行车辆控制命令
     */
    fun execute(command: VehicleCommand): CommandResult {
        return when (command.type) {
            VehicleCommandType.AC_ON -> executeACOn()
            VehicleCommandType.AC_OFF -> executeACOff()
            VehicleCommandType.AC_TEMP_UP -> executeACTempUp()
            VehicleCommandType.AC_TEMP_DOWN -> executeACTempDown()
            VehicleCommandType.AC_TEMP_SET -> executeACTempSet(command.params)
            VehicleCommandType.AC_FAN_UP -> executeACFanUp()
            VehicleCommandType.AC_FAN_DOWN -> executeACFanDown()
            VehicleCommandType.AC_MODE_AUTO -> executeACModeAuto()
            VehicleCommandType.AC_MODE_COOL -> executeACModeCool()
            VehicleCommandType.AC_MODE_HEAT -> executeACModeHeat()
            
            VehicleCommandType.SEAT_FORWARD -> executeSeatForward()
            VehicleCommandType.SEAT_BACKWARD -> executeSeatBackward()
            VehicleCommandType.SEAT_HEAT_ON -> executeSeatHeatOn()
            VehicleCommandType.SEAT_HEAT_OFF -> executeSeatHeatOff()
            VehicleCommandType.SEAT_VENTILATION_ON -> executeSeatVentilationOn()
            VehicleCommandType.SEAT_VENTILATION_OFF -> executeSeatVentilationOff()
            VehicleCommandType.SEAT_RESET -> executeSeatReset()
            
            // ... 其他命令
            
            else -> CommandResult.Error("未知命令")
        }
    }
    
    // ========== 空调控制示例 ==========
    
    /**
     * 打开空调
     */
    private fun executeACOn(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        // 检查状态
        if (currentAC.isOn) {
            return CommandResult.Success("空调已经打开了")
        }
        
        // 更新状态（Mock操作）
        val newAC = currentAC.copy(
            isOn = true,
            temperature = 24,  // 默认温度
            fanSpeed = 3,      // 默认风速
            mode = ACMode.AUTO // 默认模式
        )
        stateRepository.updateACState(newAC)
        
        // 返回反馈
        return CommandResult.Success("空调已打开，当前温度24度")
    }
    
    /**
     * 关闭空调
     */
    private fun executeACOff(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Success("空调已经关闭了")
        }
        
        // 更新状态
        val newAC = currentAC.copy(isOn = false)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("空调已关闭")
    }
    
    /**
     * 温度调高
     */
    private fun executeACTempUp(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        // 检查空调是否打开
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        // 计算新温度
        val newTemp = (currentAC.temperature + 1).coerceIn(16, 32)
        
        // 检查是否已达到最大值
        if (newTemp == currentAC.temperature && newTemp == 32) {
            return CommandResult.Success("温度已经是最高了，32度")
        }
        
        // 更新状态
        val newAC = currentAC.copy(temperature = newTemp)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("温度已调高至${newTemp}度")
    }
    
    /**
     * 温度调低
     */
    private fun executeACTempDown(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        val newTemp = (currentAC.temperature - 1).coerceIn(16, 32)
        
        if (newTemp == currentAC.temperature && newTemp == 16) {
            return CommandResult.Success("温度已经是最低了，16度")
        }
        
        val newAC = currentAC.copy(temperature = newTemp)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("温度已调低至${newTemp}度")
    }
    
    /**
     * 设置指定温度
     */
    private fun executeACTempSet(params: Map<String, String>): CommandResult {
        val tempStr = params["temperature"]
        val temp = tempStr?.toIntOrNull() ?: return CommandResult.Error("请指定温度")
        
        // 验证温度范围
        if (temp < 16 || temp > 32) {
            return CommandResult.Error("温度只能在16到32度之间")
        }
        
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        // 更新状态
        val newAC = currentAC.copy(temperature = temp)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("温度已设置为${temp}度")
    }
    
    /**
     * 风速调高
     */
    private fun executeACFanUp(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        val newSpeed = (currentAC.fanSpeed + 1).coerceIn(1, 5)
        
        if (newSpeed == currentAC.fanSpeed && newSpeed == 5) {
            return CommandResult.Success("风速已经是最大了，5档")
        }
        
        val newAC = currentAC.copy(fanSpeed = newSpeed)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("风速已调高至${newSpeed}档")
    }
    
    /**
     * 风速调低
     */
    private fun executeACFanDown(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        val newSpeed = (currentAC.fanSpeed - 1).coerceIn(1, 5)
        
        if (newSpeed == currentAC.fanSpeed && newSpeed == 1) {
            return CommandResult.Success("风速已经是最小了，1档")
        }
        
        val newAC = currentAC.copy(fanSpeed = newSpeed)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("风速已调低至${newSpeed}档")
    }
    
    /**
     * 切换到自动模式
     */
    private fun executeACModeAuto(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.mode == ACMode.AUTO) {
            return CommandResult.Success("已经是自动模式了")
        }
        
        val newAC = currentAC.copy(mode = ACMode.AUTO)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("已切换到自动模式")
    }
    
    /**
     * 切换到制冷模式
     */
    private fun executeACModeCool(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.mode == ACMode.COOL) {
            return CommandResult.Success("已经是制冷模式了")
        }
        
        val newAC = currentAC.copy(mode = ACMode.COOL)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("已切换到制冷模式")
    }
    
    /**
     * 切换到制热模式
     */
    private fun executeACModeHeat(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentAC = currentState.ac
        
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.mode == ACMode.HEAT) {
            return CommandResult.Success("已经是制热模式了")
        }
        
        val newAC = currentAC.copy(mode = ACMode.HEAT)
        stateRepository.updateACState(newAC)
        
        return CommandResult.Success("已切换到制热模式")
    }
    
    // ========== 座椅控制示例 ==========
    
    /**
     * 座椅前移
     */
    private fun executeSeatForward(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentSeat = currentState.seat
        
        val newPosition = (currentSeat.position + 1).coerceIn(1, 5)
        
        if (newPosition == currentSeat.position && newPosition == 5) {
            return CommandResult.Success("座椅已经是最前了")
        }
        
        val newSeat = currentSeat.copy(position = newPosition)
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅已前移")
    }
    
    /**
     * 座椅后移
     */
    private fun executeSeatBackward(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentSeat = currentState.seat
        
        val newPosition = (currentSeat.position - 1).coerceIn(1, 5)
        
        if (newPosition == currentSeat.position && newPosition == 1) {
            return CommandResult.Success("座椅已经是最后了")
        }
        
        val newSeat = currentSeat.copy(position = newPosition)
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅已后移")
    }
    
    /**
     * 打开座椅加热
     */
    private fun executeSeatHeatOn(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentSeat = currentState.seat
        
        if (currentSeat.heating) {
            return CommandResult.Success("座椅加热已经打开了")
        }
        
        val newSeat = currentSeat.copy(heating = true)
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅加热已打开")
    }
    
    /**
     * 关闭座椅加热
     */
    private fun executeSeatHeatOff(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentSeat = currentState.seat
        
        if (!currentSeat.heating) {
            return CommandResult.Success("座椅加热已经关闭了")
        }
        
        val newSeat = currentSeat.copy(heating = false)
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅加热已关闭")
    }
    
    /**
     * 打开座椅通风
     */
    private fun executeSeatVentilationOn(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentSeat = currentState.seat
        
        if (currentSeat.ventilation) {
            return CommandResult.Success("座椅通风已经打开了")
        }
        
        val newSeat = currentSeat.copy(ventilation = true)
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅通风已打开")
    }
    
    /**
     * 关闭座椅通风
     */
    private fun executeSeatVentilationOff(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentSeat = currentState.seat
        
        if (!currentSeat.ventilation) {
            return CommandResult.Success("座椅通风已经关闭了")
        }
        
        val newSeat = currentSeat.copy(ventilation = false)
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅通风已关闭")
    }
    
    /**
     * 座椅复位
     */
    private fun executeSeatReset(): CommandResult {
        val newSeat = SeatState(
            position = 3,  // 默认中间位置
            heating = false,
            ventilation = false
        )
        stateRepository.updateSeatState(newSeat)
        
        return CommandResult.Success("座椅已复位")
    }
    
    // ========== 车窗控制示例 ==========
    
    /**
     * 打开前窗
     */
    private fun executeWindowFrontOpen(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentWindow = currentState.window
        
        if (currentWindow.frontLeft == 100 && currentWindow.frontRight == 100) {
            return CommandResult.Success("前窗已经完全打开了")
        }
        
        val newWindow = currentWindow.copy(
            frontLeft = 100,
            frontRight = 100
        )
        stateRepository.updateWindowState(newWindow)
        
        return CommandResult.Success("前窗已打开")
    }
    
    /**
     * 关闭前窗
     */
    private fun executeWindowFrontClose(): CommandResult {
        val currentState = stateRepository.getCurrentState()
        val currentWindow = currentState.window
        
        if (currentWindow.frontLeft == 0 && currentWindow.frontRight == 0) {
            return CommandResult.Success("前窗已经完全关闭了")
        }
        
        val newWindow = currentWindow.copy(
            frontLeft = 0,
            frontRight = 0
        )
        stateRepository.updateWindowState(newWindow)
        
        return CommandResult.Success("前窗已关闭")
    }
    
    /**
     * 前窗打开一半
     */
    private fun executeWindowFrontHalf(): CommandResult {
        val newWindow = WindowState(
            frontLeft = 50,
            frontRight = 50,
            rearLeft = currentState.window.rearLeft,
            rearRight = currentState.window.rearRight,
            sunroof = currentState.window.sunroof
        )
        stateRepository.updateWindowState(newWindow)
        
        return CommandResult.Success("前窗已打开一半")
    }
    
    // ========== 其他控制命令... ==========
}

// 命令数据类
data class VehicleCommand(
    val type: VehicleCommandType,
    val params: Map<String, String> = emptyMap()
)

// 命令类型枚举
enum class VehicleCommandType {
    // 空调
    AC_ON, AC_OFF, AC_TEMP_UP, AC_TEMP_DOWN, AC_TEMP_SET,
    AC_FAN_UP, AC_FAN_DOWN,
    AC_MODE_AUTO, AC_MODE_COOL, AC_MODE_HEAT,
    
    // 座椅
    SEAT_FORWARD, SEAT_BACKWARD,
    SEAT_HEAT_ON, SEAT_HEAT_OFF,
    SEAT_VENTILATION_ON, SEAT_VENTILATION_OFF,
    SEAT_RESET,
    
    // 车窗
    WINDOW_FRONT_OPEN, WINDOW_FRONT_CLOSE, WINDOW_FRONT_HALF,
    WINDOW_REAR_OPEN, WINDOW_REAR_CLOSE,
    SUNROOF_OPEN, SUNROOF_CLOSE,
    
    // ... 其他命令
}

// 命令结果
sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}
```

**关键点说明**：
1. **状态不可变更新**：使用`copy()`方法创建新对象
2. **状态验证**：在执行操作前检查当前状态
3. **边界检查**：检查数值范围（温度16-32，风速1-5等）
4. **友好反馈**：返回详细的执行结果消息

---

## 状态更新流程

### 完整流程示例：用户说"打开空调"

```
1. 用户语音："打开空调"
   ↓
2. 语音识别：百度SDK识别为"打开空调"
   ↓
3. 意图识别：IntentParser解析为 VehicleCommand(AC_ON)
   ↓
4. 命令执行：VehicleControlExecutor.execute()
   ↓
5. 状态检查：检查当前空调状态（假设是关闭的）
   ↓
6. 状态更新：
   - 获取当前状态：stateRepository.getCurrentState()
   - 创建新状态：currentState.ac.copy(isOn = true, temperature = 24, ...)
   - 更新状态：stateRepository.updateACState(newAC)
   ↓
7. StateFlow自动通知：
   - Repository的_vehicleState.value发生变化
   - 所有观察者（ViewModel）自动收到通知
   ↓
8. ViewModel更新：
   - ViewModel观察vehicleState
   - 收到新状态后更新UI状态
   ↓
9. UI自动更新：
   - UI组件观察ViewModel的状态
   - 自动刷新显示（空调图标、温度显示等）
   ↓
10. TTS播报：
    - 返回CommandResult.Success("空调已打开，当前温度24度")
    - TTSManager播报反馈
```

---

## UI监听与更新

### 1. ViewModel层 - 状态暴露

```kotlin
/**
 * 车辆控制ViewModel
 * 职责：暴露状态给UI层，处理UI事件
 */
class VehicleControlViewModel(
    private val stateRepository: VehicleStateRepository,
    private val executor: VehicleControlExecutor
) : ViewModel() {
    
    // 暴露车辆状态给UI观察（只读）
    val vehicleState: StateFlow<VehicleState> = stateRepository.vehicleState
    
    // 暴露空调状态（方便UI直接观察）
    val acState: StateFlow<ACState> = stateRepository.vehicleState
        .map { it.ac }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ACState()
        )
    
    // 暴露座椅状态
    val seatState: StateFlow<SeatState> = stateRepository.vehicleState
        .map { it.seat }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SeatState()
        )
    
    // 暴露车窗状态
    val windowState: StateFlow<WindowState> = stateRepository.vehicleState
        .map { it.window }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WindowState()
        )
    
    // 暴露灯光状态
    val lightState: StateFlow<LightState> = stateRepository.vehicleState
        .map { it.light }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LightState()
        )
    
    // 暴露车门状态
    val doorState: StateFlow<DoorState> = stateRepository.vehicleState
        .map { it.door }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DoorState()
        )
    
    // 暴露引擎状态
    val engineState: StateFlow<EngineState> = stateRepository.vehicleState
        .map { it.engine }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EngineState()
        )
    
    /**
     * 执行车辆控制命令（可以从UI手动触发，或从语音指令触发）
     */
    fun executeCommand(command: VehicleCommand): Flow<CommandResult> = flow {
        val result = executor.execute(command)
        emit(result)
    }
}
```

---

### 2. UI层 - Compose实现示例

```kotlin
/**
 * 车辆控制面板Fragment/Composable
 */
@Composable
fun VehicleControlPanel(
    viewModel: VehicleControlViewModel = hiltViewModel()
) {
    // 观察空调状态
    val acState by viewModel.acState.collectAsState()
    
    // 观察座椅状态
    val seatState by viewModel.seatState.collectAsState()
    
    // 观察车窗状态
    val windowState by viewModel.windowState.collectAsState()
    
    // 观察灯光状态
    val lightState by viewModel.lightState.collectAsState()
    
    // 观察车门状态
    val doorState by viewModel.doorState.collectAsState()
    
    // 观察引擎状态
    val engineState by viewModel.engineState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 空调控制卡片
        ACControlCard(
            acState = acState,
            onCommand = { command ->
                viewModel.executeCommand(command).collect { result ->
                    // 处理结果（如TTS播报）
                }
            }
        )
        
        // 座椅控制卡片
        SeatControlCard(
            seatState = seatState,
            onCommand = { command ->
                viewModel.executeCommand(command).collect { }
            }
        )
        
        // 车窗控制卡片
        WindowControlCard(
            windowState = windowState,
            onCommand = { command ->
                viewModel.executeCommand(command).collect { }
            }
        )
        
        // 灯光控制卡片
        LightControlCard(
            lightState = lightState,
            onCommand = { command ->
                viewModel.executeCommand(command).collect { }
            }
        )
        
        // 车门控制卡片
        DoorControlCard(
            doorState = doorState,
            onCommand = { command ->
                viewModel.executeCommand(command).collect { }
            }
        )
        
        // 引擎控制卡片
        EngineControlCard(
            engineState = engineState,
            onCommand = { command ->
                viewModel.executeCommand(command).collect { }
            }
        )
    }
}

/**
 * 空调控制卡片组件
 */
@Composable
fun ACControlCard(
    acState: ACState,
    onCommand: (VehicleCommand) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Text(
                text = "空调",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 开关状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "状态")
                
                Switch(
                    checked = acState.isOn,
                    onCheckedChange = { checked ->
                        val command = if (checked) {
                            VehicleCommand(VehicleCommandType.AC_ON)
                        } else {
                            VehicleCommand(VehicleCommandType.AC_OFF)
                        }
                        onCommand(command)
                    }
                )
            }
            
            // 温度显示（仅在打开时显示）
            if (acState.isOn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "温度")
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_TEMP_DOWN))
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "降低")
                        }
                        
                        Text(
                            text = "${acState.temperature}°C",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_TEMP_UP))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "升高")
                        }
                    }
                }
                
                // 风速显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "风速")
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_FAN_DOWN))
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "减小")
                        }
                        
                        Text(
                            text = "${acState.fanSpeed}档",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        IconButton(
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_FAN_UP))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "增大")
                        }
                    }
                }
                
                // 模式选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "模式")
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = acState.mode == ACMode.AUTO,
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_MODE_AUTO))
                            },
                            label = { Text("自动") }
                        )
                        FilterChip(
                            selected = acState.mode == ACMode.COOL,
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_MODE_COOL))
                            },
                            label = { Text("制冷") }
                        )
                        FilterChip(
                            selected = acState.mode == ACMode.HEAT,
                            onClick = {
                                onCommand(VehicleCommand(VehicleCommandType.AC_MODE_HEAT))
                            },
                            label = { Text("制热") }
                        )
                    }
                }
            }
        }
    }
}
```

---

### 3. UI层 - 传统View实现示例

```kotlin
/**
 * 车辆控制面板Fragment（传统View方式）
 */
class VehicleControlPanelFragment : Fragment() {
    
    private lateinit var viewModel: VehicleControlViewModel
    private lateinit var binding: FragmentVehicleControlBinding
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVehicleControlBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[VehicleControlViewModel::class.java]
        
        // 观察空调状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.acState.collect { acState ->
                updateACUI(acState)
            }
        }
        
        // 观察座椅状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.seatState.collect { seatState ->
                updateSeatUI(seatState)
            }
        }
        
        // 观察车窗状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.windowState.collect { windowState ->
                updateWindowUI(windowState)
            }
        }
        
        // ... 其他状态观察
        
        // 设置按钮点击事件
        setupClickListeners()
    }
    
    /**
     * 更新空调UI
     */
    private fun updateACUI(acState: ACState) {
        // 更新开关状态
        binding.acSwitch.isChecked = acState.isOn
        
        // 更新温度显示
        binding.temperatureText.text = "${acState.temperature}°C"
        
        // 更新风速显示
        binding.fanSpeedText.text = "${acState.fanSpeed}档"
        
        // 更新模式显示
        when (acState.mode) {
            ACMode.AUTO -> binding.modeText.text = "自动"
            ACMode.COOL -> binding.modeText.text = "制冷"
            ACMode.HEAT -> binding.modeText.text = "制热"
        }
        
        // 根据开关状态显示/隐藏相关控件
        binding.temperatureLayout.visibility = if (acState.isOn) View.VISIBLE else View.GONE
        binding.fanSpeedLayout.visibility = if (acState.isOn) View.VISIBLE else View.GONE
        binding.modeLayout.visibility = if (acState.isOn) View.VISIBLE else View.GONE
    }
    
    /**
     * 更新座椅UI
     */
    private fun updateSeatUI(seatState: SeatState) {
        binding.seatPositionText.text = "位置${seatState.position}"
        binding.seatHeatingSwitch.isChecked = seatState.heating
        binding.seatVentilationSwitch.isChecked = seatState.ventilation
    }
    
    /**
     * 更新车窗UI
     */
    private fun updateWindowUI(windowState: WindowState) {
        binding.frontWindowProgress.progress = windowState.frontLeft
        binding.rearWindowProgress.progress = windowState.rearLeft
        binding.sunroofSwitch.isChecked = windowState.sunroof
    }
    
    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 空调开关
        binding.acSwitch.setOnCheckedChangeListener { _, isChecked ->
            val command = if (isChecked) {
                VehicleCommand(VehicleCommandType.AC_ON)
            } else {
                VehicleCommand(VehicleCommandType.AC_OFF)
            }
            executeCommand(command)
        }
        
        // 温度增加
        binding.temperatureUpBtn.setOnClickListener {
            executeCommand(VehicleCommand(VehicleCommandType.AC_TEMP_UP))
        }
        
        // 温度减少
        binding.temperatureDownBtn.setOnClickListener {
            executeCommand(VehicleCommand(VehicleCommandType.AC_TEMP_DOWN))
        }
        
        // ... 其他按钮
    }
    
    /**
     * 执行命令
     */
    private fun executeCommand(command: VehicleCommand) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.executeCommand(command).collect { result ->
                when (result) {
                    is CommandResult.Success -> {
                        // 可以显示Toast或进行TTS播报
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                    is CommandResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
```

---

## 完整代码示例

### 使用示例：完整的调用链

```kotlin
// 1. 初始化Repository（通常作为单例）
val stateRepository = VehicleStateRepository()

// 2. 初始化Executor
val ttsManager = BaiduTTSManager(context, appId, apiKey, secretKey)
val executor = VehicleControlExecutor(stateRepository, ttsManager)

// 3. 初始化ViewModel
val viewModel = VehicleControlViewModel(stateRepository, executor)

// 4. 用户说"打开空调"
val command = VehicleCommand(VehicleCommandType.AC_ON)

// 5. 执行命令
val result = executor.execute(command)

// 6. 状态自动更新
// Repository的_vehicleState.value发生变化

// 7. UI自动响应
// ViewModel的vehicleState StateFlow发出新值
// UI组件（Compose或View）自动刷新

// 8. TTS播报
when (result) {
    is CommandResult.Success -> ttsManager.speak(result.message)
    is CommandResult.Error -> ttsManager.speak(result.message)
}
```

---

## 关键设计要点总结

### 1. 状态管理
- ✅ 使用StateFlow作为状态容器
- ✅ Repository作为单一数据源
- ✅ 不可变更新（使用copy()）

### 2. 响应式更新
- ✅ UI自动响应状态变化
- ✅ 无需手动刷新UI
- ✅ 数据流向单一：Repository → ViewModel → UI

### 3. Mock实现
- ✅ 所有车辆控制都是Mock
- ✅ 真实的车载系统会用相同的数据结构
- ✅ 替换Repository实现即可对接真实系统

### 4. 可扩展性
- ✅ 新增功能只需扩展数据模型和Executor
- ✅ UI组件独立，易于维护
- ✅ 命令模式便于扩展新命令

这样的设计既适合教学演示，又便于将来对接真实的车载系统！

