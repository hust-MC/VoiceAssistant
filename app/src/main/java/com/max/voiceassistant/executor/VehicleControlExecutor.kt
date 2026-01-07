package com.max.voiceassistant.executor

import com.max.voiceassistant.data.VehicleStateRepository
import com.max.voiceassistant.model.*

/**
 * 车辆控制执行器（Mock实现）
 */
class VehicleControlExecutor(
    private val repository: VehicleStateRepository
) {
    
    fun execute(command: Command): CommandResult {
        return when (command.type) {
            // 空调控制
            CommandType.AC_ON -> executeACOn()
            CommandType.AC_OFF -> executeACOff()
            CommandType.AC_TEMP_UP -> executeACTempUp()
            CommandType.AC_TEMP_DOWN -> executeACTempDown()
            CommandType.AC_TEMP_SET -> executeACTempSet(command.params)
            CommandType.AC_FAN_UP -> executeACFanUp()
            CommandType.AC_FAN_DOWN -> executeACFanDown()
            CommandType.AC_MODE_AUTO -> executeACMode(ACMode.AUTO)
            CommandType.AC_MODE_COOL -> executeACMode(ACMode.COOL)
            CommandType.AC_MODE_HEAT -> executeACMode(ACMode.HEAT)
            
            // 座椅控制
            CommandType.SEAT_FORWARD -> executeSeatForward()
            CommandType.SEAT_BACKWARD -> executeSeatBackward()
            CommandType.SEAT_HEAT_ON -> executeSeatHeat(true)
            CommandType.SEAT_HEAT_OFF -> executeSeatHeat(false)
            CommandType.SEAT_VENTILATION_ON -> executeSeatVentilation(true)
            CommandType.SEAT_VENTILATION_OFF -> executeSeatVentilation(false)
            CommandType.SEAT_RESET -> executeSeatReset()
            
            // 车窗控制
            CommandType.WINDOW_FRONT_OPEN -> executeWindowFront(WindowState.FULL_OPEN)
            CommandType.WINDOW_FRONT_CLOSE -> executeWindowFront(WindowState.CLOSED)
            CommandType.WINDOW_FRONT_HALF -> executeWindowFront(WindowState.HALF_OPEN)
            CommandType.WINDOW_REAR_OPEN -> executeWindowRear(WindowState.FULL_OPEN)
            CommandType.WINDOW_REAR_CLOSE -> executeWindowRear(WindowState.CLOSED)
            CommandType.SUNROOF_OPEN -> executeSunroof(true)
            CommandType.SUNROOF_CLOSE -> executeSunroof(false)
            
            // 灯光控制
            CommandType.LIGHT_HEADLIGHT_ON -> executeHeadlight(true)
            CommandType.LIGHT_HEADLIGHT_OFF -> executeHeadlight(false)
            CommandType.LIGHT_HEADLIGHT_AUTO -> executeHeadlightAuto()
            CommandType.LIGHT_AMBIENT_ON -> executeAmbientLight(true)
            CommandType.LIGHT_AMBIENT_OFF -> executeAmbientLight(false)
            CommandType.LIGHT_AMBIENT_COLOR -> executeAmbientColor(command.params)
            
            // 车门控制
            CommandType.DOOR_LOCK -> executeDoorLock(true)
            CommandType.DOOR_UNLOCK -> executeDoorLock(false)
            CommandType.TRUNK_OPEN -> executeTrunk(true)
            CommandType.TRUNK_CLOSE -> executeTrunk(false)
            
            // 引擎控制
            CommandType.ENGINE_START -> executeEngine(true)
            CommandType.ENGINE_STOP -> executeEngine(false)
            
            else -> CommandResult.Error("暂不支持此功能")
        }
    }
    
    // ========== 空调控制 ==========
    
    private fun executeACOn(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (currentAC.isOn) {
            return CommandResult.Success("空调已经打开了，当前温度${currentAC.temperature}度")
        }
        
        repository.updateACState(currentAC.copy(
            isOn = true,
            temperature = 24,
            fanSpeed = 3,
            mode = ACMode.AUTO
        ))
        return CommandResult.Success("空调已打开，当前温度24度")
    }
    
    private fun executeACOff(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Success("空调已经关闭了")
        }
        
        repository.updateACState(currentAC.copy(isOn = false))
        return CommandResult.Success("空调已关闭")
    }
    
    private fun executeACTempUp(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.temperature >= ACState.MAX_TEMPERATURE) {
            return CommandResult.Success("温度已经是最高了，${ACState.MAX_TEMPERATURE}度")
        }
        
        val newTemp = currentAC.temperature + 1
        repository.updateACState(currentAC.copy(temperature = newTemp))
        return CommandResult.Success("温度已调高至${newTemp}度")
    }
    
    private fun executeACTempDown(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.temperature <= ACState.MIN_TEMPERATURE) {
            return CommandResult.Success("温度已经是最低了，${ACState.MIN_TEMPERATURE}度")
        }
        
        val newTemp = currentAC.temperature - 1
        repository.updateACState(currentAC.copy(temperature = newTemp))
        return CommandResult.Success("温度已调低至${newTemp}度")
    }
    
    private fun executeACTempSet(params: Map<String, String>): CommandResult {
        val currentAC = repository.getCurrentState().ac
        
        val tempStr = params["temperature"] ?: return CommandResult.Error("请指定温度")
        val temp = tempStr.toIntOrNull() ?: return CommandResult.Error("无法识别温度")
        
        if (temp < ACState.MIN_TEMPERATURE || temp > ACState.MAX_TEMPERATURE) {
            return CommandResult.Error("温度只能在${ACState.MIN_TEMPERATURE}到${ACState.MAX_TEMPERATURE}度之间")
        }
        
        if (!currentAC.isOn) {
            // 自动打开空调
            repository.updateACState(currentAC.copy(isOn = true, temperature = temp))
            return CommandResult.Success("空调已打开，温度设置为${temp}度")
        }
        
        repository.updateACState(currentAC.copy(temperature = temp))
        return CommandResult.Success("温度已设置为${temp}度")
    }
    
    private fun executeACFanUp(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.fanSpeed >= ACState.MAX_FAN_SPEED) {
            return CommandResult.Success("风速已经是最大了，${ACState.MAX_FAN_SPEED}档")
        }
        
        val newSpeed = currentAC.fanSpeed + 1
        repository.updateACState(currentAC.copy(fanSpeed = newSpeed))
        return CommandResult.Success("风速已调高至${newSpeed}档")
    }
    
    private fun executeACFanDown(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.fanSpeed <= ACState.MIN_FAN_SPEED) {
            return CommandResult.Success("风速已经是最小了，${ACState.MIN_FAN_SPEED}档")
        }
        
        val newSpeed = currentAC.fanSpeed - 1
        repository.updateACState(currentAC.copy(fanSpeed = newSpeed))
        return CommandResult.Success("风速已调低至${newSpeed}档")
    }
    
    private fun executeACMode(mode: ACMode): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error("请先打开空调")
        }
        
        if (currentAC.mode == mode) {
            return CommandResult.Success("已经是${mode.displayName}模式了")
        }
        
        repository.updateACState(currentAC.copy(mode = mode))
        return CommandResult.Success("已切换到${mode.displayName}模式")
    }
    
    // ========== 座椅控制 ==========
    
    private fun executeSeatForward(): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        if (currentSeat.position >= SeatState.MAX_POSITION) {
            return CommandResult.Success("座椅已经是最前了")
        }
        
        val newPos = currentSeat.position + 1
        repository.updateSeatState(currentSeat.copy(position = newPos))
        return CommandResult.Success("座椅已前移")
    }
    
    private fun executeSeatBackward(): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        if (currentSeat.position <= SeatState.MIN_POSITION) {
            return CommandResult.Success("座椅已经是最后了")
        }
        
        val newPos = currentSeat.position - 1
        repository.updateSeatState(currentSeat.copy(position = newPos))
        return CommandResult.Success("座椅已后移")
    }
    
    private fun executeSeatHeat(enable: Boolean): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        if (currentSeat.heating == enable) {
            return CommandResult.Success("座椅加热已经${if (enable) "打开" else "关闭"}了")
        }
        
        repository.updateSeatState(currentSeat.copy(heating = enable))
        return CommandResult.Success("座椅加热已${if (enable) "打开" else "关闭"}")
    }
    
    private fun executeSeatVentilation(enable: Boolean): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        if (currentSeat.ventilation == enable) {
            return CommandResult.Success("座椅通风已经${if (enable) "打开" else "关闭"}了")
        }
        
        repository.updateSeatState(currentSeat.copy(ventilation = enable))
        return CommandResult.Success("座椅通风已${if (enable) "打开" else "关闭"}")
    }
    
    private fun executeSeatReset(): CommandResult {
        repository.updateSeatState(SeatState())
        return CommandResult.Success("座椅已复位")
    }
    
    // ========== 车窗控制 ==========
    
    private fun executeWindowFront(percent: Int): CommandResult {
        val currentWindow = repository.getCurrentState().window
        
        val status = when (percent) {
            WindowState.CLOSED -> "已关闭"
            WindowState.HALF_OPEN -> "已打开一半"
            WindowState.FULL_OPEN -> "已打开"
            else -> "已调整"
        }
        
        repository.updateWindowState(currentWindow.copy(
            frontLeft = percent,
            frontRight = percent
        ))
        return CommandResult.Success("前窗$status")
    }
    
    private fun executeWindowRear(percent: Int): CommandResult {
        val currentWindow = repository.getCurrentState().window
        
        val status = when (percent) {
            WindowState.CLOSED -> "已关闭"
            WindowState.FULL_OPEN -> "已打开"
            else -> "已调整"
        }
        
        repository.updateWindowState(currentWindow.copy(
            rearLeft = percent,
            rearRight = percent
        ))
        return CommandResult.Success("后窗$status")
    }
    
    private fun executeSunroof(open: Boolean): CommandResult {
        val currentWindow = repository.getCurrentState().window
        
        if (currentWindow.sunroof == open) {
            return CommandResult.Success("天窗已经${if (open) "打开" else "关闭"}了")
        }
        
        repository.updateWindowState(currentWindow.copy(sunroof = open))
        return CommandResult.Success("天窗已${if (open) "打开" else "关闭"}")
    }
    
    // ========== 灯光控制 ==========
    
    private fun executeHeadlight(on: Boolean): CommandResult {
        val currentLight = repository.getCurrentState().light
        
        if (currentLight.headlight == on) {
            return CommandResult.Success("大灯已经${if (on) "打开" else "关闭"}了")
        }
        
        repository.updateLightState(currentLight.copy(
            headlight = on,
            headlightMode = if (on) HeadlightMode.ON else HeadlightMode.OFF
        ))
        return CommandResult.Success("大灯已${if (on) "打开" else "关闭"}")
    }
    
    private fun executeHeadlightAuto(): CommandResult {
        val currentLight = repository.getCurrentState().light
        
        if (currentLight.headlightMode == HeadlightMode.AUTO) {
            return CommandResult.Success("已经是自动大灯模式了")
        }
        
        repository.updateLightState(currentLight.copy(headlightMode = HeadlightMode.AUTO))
        return CommandResult.Success("已切换到自动大灯")
    }
    
    private fun executeAmbientLight(on: Boolean): CommandResult {
        val currentLight = repository.getCurrentState().light
        
        if (currentLight.ambientLight == on) {
            return CommandResult.Success("氛围灯已经${if (on) "打开" else "关闭"}了")
        }
        
        repository.updateLightState(currentLight.copy(ambientLight = on))
        return CommandResult.Success("氛围灯已${if (on) "打开" else "关闭"}")
    }
    
    private fun executeAmbientColor(params: Map<String, String>): CommandResult {
        val color = params["color"] ?: "蓝色"
        val currentLight = repository.getCurrentState().light
        
        if (!currentLight.ambientLight) {
            // 自动打开氛围灯
            repository.updateLightState(currentLight.copy(
                ambientLight = true,
                ambientColor = color
            ))
            return CommandResult.Success("氛围灯已打开，颜色设置为$color")
        }
        
        repository.updateLightState(currentLight.copy(ambientColor = color))
        return CommandResult.Success("氛围灯已调成$color")
    }
    
    // ========== 车门控制 ==========
    
    private fun executeDoorLock(lock: Boolean): CommandResult {
        val currentDoor = repository.getCurrentState().door
        
        if (currentDoor.isLocked == lock) {
            return CommandResult.Success("车辆已经${if (lock) "锁定" else "解锁"}了")
        }
        
        repository.updateDoorState(currentDoor.copy(isLocked = lock))
        return CommandResult.Success("车辆已${if (lock) "锁定" else "解锁"}")
    }
    
    private fun executeTrunk(open: Boolean): CommandResult {
        val currentDoor = repository.getCurrentState().door
        
        if (currentDoor.trunkOpen == open) {
            return CommandResult.Success("后备箱已经${if (open) "打开" else "关闭"}了")
        }
        
        repository.updateDoorState(currentDoor.copy(trunkOpen = open))
        return CommandResult.Success("后备箱已${if (open) "打开" else "关闭"}")
    }
    
    // ========== 引擎控制 ==========
    
    private fun executeEngine(start: Boolean): CommandResult {
        val currentEngine = repository.getCurrentState().engine
        
        if (currentEngine.isRunning == start) {
            return CommandResult.Success("车辆已经${if (start) "启动" else "熄火"}了")
        }
        
        repository.updateEngineState(EngineState(isRunning = start))
        return CommandResult.Success("车辆已${if (start) "启动" else "熄火"}")
    }
}

