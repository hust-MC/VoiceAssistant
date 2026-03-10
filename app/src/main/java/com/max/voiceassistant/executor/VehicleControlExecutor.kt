package com.max.voiceassistant.executor

import android.content.Context
import com.max.voiceassistant.R
import com.max.voiceassistant.data.VehicleStateRepository
import com.max.voiceassistant.model.*

/**
 * 车辆控制执行器（Mock实现）
 */
class VehicleControlExecutor(
    private val context: Context,
    private val repository: VehicleStateRepository
) {
    private fun str(id: Int, vararg args: Any?) = context.getString(id, *args)
    
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
            
            else -> CommandResult.Error(str(R.string.vehicle_unsupported))
        }
    }
    
    // ========== 空调控制 ==========
    
    private fun executeACOn(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (currentAC.isOn) {
            return CommandResult.Success(str(R.string.vehicle_ac_already_on, currentAC.temperature))
        }
        
        repository.updateACState(currentAC.copy(
            isOn = true,
            temperature = 24,
            fanSpeed = 3,
            mode = ACMode.AUTO
        ))
        return CommandResult.Success(str(R.string.vehicle_ac_turned_on))
    }
    
    private fun executeACOff(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Success(str(R.string.vehicle_ac_already_off))
        }
        
        repository.updateACState(currentAC.copy(isOn = false))
        return CommandResult.Success(str(R.string.vehicle_ac_off))
    }
    
    private fun executeACTempUp(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error(str(R.string.vehicle_ac_turn_on_first))
        }
        
        if (currentAC.temperature >= ACState.MAX_TEMPERATURE) {
            return CommandResult.Success(str(R.string.vehicle_temp_max, ACState.MAX_TEMPERATURE))
        }
        
        val newTemp = currentAC.temperature + 1
        repository.updateACState(currentAC.copy(temperature = newTemp))
        return CommandResult.Success(str(R.string.vehicle_temp_up, newTemp))
    }
    
    private fun executeACTempDown(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error(str(R.string.vehicle_ac_turn_on_first))
        }
        
        if (currentAC.temperature <= ACState.MIN_TEMPERATURE) {
            return CommandResult.Success(str(R.string.vehicle_temp_min, ACState.MIN_TEMPERATURE))
        }
        
        val newTemp = currentAC.temperature - 1
        repository.updateACState(currentAC.copy(temperature = newTemp))
        return CommandResult.Success(str(R.string.vehicle_temp_down, newTemp))
    }
    
    private fun executeACTempSet(params: Map<String, String>): CommandResult {
        val currentAC = repository.getCurrentState().ac
        
        val tempStr = params["temperature"] ?: return CommandResult.Error(str(R.string.vehicle_temp_specify))
        val temp = tempStr.toIntOrNull() ?: return CommandResult.Error(str(R.string.vehicle_temp_invalid))
        
        if (temp < ACState.MIN_TEMPERATURE || temp > ACState.MAX_TEMPERATURE) {
            return CommandResult.Error(str(R.string.vehicle_temp_range, ACState.MIN_TEMPERATURE, ACState.MAX_TEMPERATURE))
        }
        
        if (!currentAC.isOn) {
            repository.updateACState(currentAC.copy(isOn = true, temperature = temp))
            return CommandResult.Success(str(R.string.vehicle_ac_on_set_temp, temp))
        }
        
        repository.updateACState(currentAC.copy(temperature = temp))
        return CommandResult.Success(str(R.string.vehicle_temp_set, temp))
    }
    
    private fun executeACFanUp(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error(str(R.string.vehicle_ac_turn_on_first))
        }
        
        if (currentAC.fanSpeed >= ACState.MAX_FAN_SPEED) {
            return CommandResult.Success(str(R.string.vehicle_fan_max, ACState.MAX_FAN_SPEED))
        }
        
        val newSpeed = currentAC.fanSpeed + 1
        repository.updateACState(currentAC.copy(fanSpeed = newSpeed))
        return CommandResult.Success(str(R.string.vehicle_fan_up, newSpeed))
    }
    
    private fun executeACFanDown(): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error(str(R.string.vehicle_ac_turn_on_first))
        }
        
        if (currentAC.fanSpeed <= ACState.MIN_FAN_SPEED) {
            return CommandResult.Success(str(R.string.vehicle_fan_min, ACState.MIN_FAN_SPEED))
        }
        
        val newSpeed = currentAC.fanSpeed - 1
        repository.updateACState(currentAC.copy(fanSpeed = newSpeed))
        return CommandResult.Success(str(R.string.vehicle_fan_down, newSpeed))
    }
    
    private fun executeACMode(mode: ACMode): CommandResult {
        val currentAC = repository.getCurrentState().ac
        if (!currentAC.isOn) {
            return CommandResult.Error(str(R.string.vehicle_ac_turn_on_first))
        }
        val modeName = context.getString(when (mode) { ACMode.AUTO -> R.string.ac_mode_auto; ACMode.COOL -> R.string.ac_mode_cool; ACMode.HEAT -> R.string.ac_mode_heat })
        if (currentAC.mode == mode) {
            return CommandResult.Success(str(R.string.vehicle_ac_mode_already, modeName))
        }
        repository.updateACState(currentAC.copy(mode = mode))
        return CommandResult.Success(str(R.string.vehicle_ac_mode_switched, modeName))
    }
    
    // ========== 座椅控制 ==========
    
    private fun executeSeatForward(): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        if (currentSeat.position >= SeatState.MAX_POSITION) {
            return CommandResult.Success(str(R.string.vehicle_seat_forward_max))
        }
        
        val newPos = currentSeat.position + 1
        repository.updateSeatState(currentSeat.copy(position = newPos))
        return CommandResult.Success(str(R.string.vehicle_seat_forward))
    }
    
    private fun executeSeatBackward(): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        if (currentSeat.position <= SeatState.MIN_POSITION) {
            return CommandResult.Success(str(R.string.vehicle_seat_backward_max))
        }
        
        val newPos = currentSeat.position - 1
        repository.updateSeatState(currentSeat.copy(position = newPos))
        return CommandResult.Success(str(R.string.vehicle_seat_backward))
    }
    
    private fun executeSeatHeat(enable: Boolean): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        val onOff = if (enable) context.getString(R.string.vehicle_on) else context.getString(R.string.vehicle_off)
        if (currentSeat.heating == enable) {
            return CommandResult.Success(str(R.string.vehicle_seat_heat_already, onOff))
        }
        repository.updateSeatState(currentSeat.copy(heating = enable))
        return CommandResult.Success(str(R.string.vehicle_seat_heat, onOff))
    }
    
    private fun executeSeatVentilation(enable: Boolean): CommandResult {
        val currentSeat = repository.getCurrentState().seat
        
        val onOff = if (enable) context.getString(R.string.vehicle_on) else context.getString(R.string.vehicle_off)
        if (currentSeat.ventilation == enable) {
            return CommandResult.Success(str(R.string.vehicle_seat_vent_already, onOff))
        }
        repository.updateSeatState(currentSeat.copy(ventilation = enable))
        return CommandResult.Success(str(R.string.vehicle_seat_vent, onOff))
    }
    
    private fun executeSeatReset(): CommandResult {
        repository.updateSeatState(SeatState())
        return CommandResult.Success(str(R.string.vehicle_seat_reset))
    }
    
    // ========== 车窗控制 ==========
    
    private fun executeWindowFront(percent: Int): CommandResult {
        val currentWindow = repository.getCurrentState().window
        
        val status = when (percent) {
            WindowState.CLOSED -> context.getString(R.string.vehicle_window_closed)
            WindowState.HALF_OPEN -> context.getString(R.string.vehicle_window_half)
            WindowState.FULL_OPEN -> context.getString(R.string.vehicle_window_open)
            else -> context.getString(R.string.vehicle_window_adjusted)
        }
        repository.updateWindowState(currentWindow.copy(
            frontLeft = percent,
            frontRight = percent
        ))
        return CommandResult.Success(str(R.string.vehicle_front_window, status))
    }
    
    private fun executeWindowRear(percent: Int): CommandResult {
        val currentWindow = repository.getCurrentState().window
        
        val status = when (percent) {
            WindowState.CLOSED -> context.getString(R.string.vehicle_window_closed)
            WindowState.FULL_OPEN -> context.getString(R.string.vehicle_window_open)
            else -> context.getString(R.string.vehicle_window_adjusted)
        }
        repository.updateWindowState(currentWindow.copy(
            rearLeft = percent,
            rearRight = percent
        ))
        return CommandResult.Success(str(R.string.vehicle_rear_window, status))
    }
    
    private fun executeSunroof(open: Boolean): CommandResult {
        val currentWindow = repository.getCurrentState().window
        
        val onOff = if (open) context.getString(R.string.vehicle_on) else context.getString(R.string.vehicle_off)
        if (currentWindow.sunroof == open) {
            return CommandResult.Success(str(R.string.vehicle_sunroof_already, onOff))
        }
        repository.updateWindowState(currentWindow.copy(sunroof = open))
        return CommandResult.Success(str(R.string.vehicle_sunroof, onOff))
    }
    
    // ========== 灯光控制 ==========
    
    private fun executeHeadlight(on: Boolean): CommandResult {
        val currentLight = repository.getCurrentState().light
        
        val onOff = if (on) context.getString(R.string.vehicle_on) else context.getString(R.string.vehicle_off)
        if (currentLight.headlight == on) {
            return CommandResult.Success(str(R.string.vehicle_headlight_already, onOff))
        }
        repository.updateLightState(currentLight.copy(
            headlight = on,
            headlightMode = if (on) HeadlightMode.ON else HeadlightMode.OFF
        ))
        return CommandResult.Success(str(R.string.vehicle_headlight, onOff))
    }
    
    private fun executeHeadlightAuto(): CommandResult {
        val currentLight = repository.getCurrentState().light
        
        if (currentLight.headlightMode == HeadlightMode.AUTO) {
            return CommandResult.Success(str(R.string.vehicle_headlight_auto_already))
        }
        repository.updateLightState(currentLight.copy(headlightMode = HeadlightMode.AUTO))
        return CommandResult.Success(str(R.string.vehicle_headlight_auto))
    }
    
    private fun executeAmbientLight(on: Boolean): CommandResult {
        val currentLight = repository.getCurrentState().light
        
        val onOff = if (on) context.getString(R.string.vehicle_on) else context.getString(R.string.vehicle_off)
        if (currentLight.ambientLight == on) {
            return CommandResult.Success(str(R.string.vehicle_ambient_already, onOff))
        }
        repository.updateLightState(currentLight.copy(ambientLight = on))
        return CommandResult.Success(str(R.string.vehicle_ambient, onOff))
    }
    
    private fun executeAmbientColor(params: Map<String, String>): CommandResult {
        val color = params["color"] ?: context.getString(R.string.default_color_blue)
        val currentLight = repository.getCurrentState().light
        
        if (!currentLight.ambientLight) {
            repository.updateLightState(currentLight.copy(
                ambientLight = true,
                ambientColor = color
            ))
            return CommandResult.Success(str(R.string.vehicle_ambient_color_set, color))
        }
        repository.updateLightState(currentLight.copy(ambientColor = color))
        return CommandResult.Success(str(R.string.vehicle_ambient_color, color))
    }
    
    // ========== 车门控制 ==========
    
    private fun executeDoorLock(lock: Boolean): CommandResult {
        val currentDoor = repository.getCurrentState().door
        
        val lockStr = if (lock) context.getString(R.string.vehicle_locked) else context.getString(R.string.vehicle_unlocked)
        if (currentDoor.isLocked == lock) {
            return CommandResult.Success(str(R.string.vehicle_door_already, lockStr))
        }
        repository.updateDoorState(currentDoor.copy(isLocked = lock))
        return CommandResult.Success(str(R.string.vehicle_door, lockStr))
    }
    
    private fun executeTrunk(open: Boolean): CommandResult {
        val currentDoor = repository.getCurrentState().door
        
        val onOff = if (open) context.getString(R.string.vehicle_on) else context.getString(R.string.vehicle_off)
        if (currentDoor.trunkOpen == open) {
            return CommandResult.Success(str(R.string.vehicle_trunk_already, onOff))
        }
        repository.updateDoorState(currentDoor.copy(trunkOpen = open))
        return CommandResult.Success(str(R.string.vehicle_trunk, onOff))
    }
    
    // ========== 引擎控制 ==========
    
    private fun executeEngine(start: Boolean): CommandResult {
        val currentEngine = repository.getCurrentState().engine
        
        val stateStr = if (start) context.getString(R.string.vehicle_started) else context.getString(R.string.vehicle_stopped)
        if (currentEngine.isRunning == start) {
            return CommandResult.Success(str(R.string.vehicle_engine_already, stateStr))
        }
        repository.updateEngineState(EngineState(isRunning = start))
        return CommandResult.Success(str(R.string.vehicle_engine, stateStr))
    }
}

