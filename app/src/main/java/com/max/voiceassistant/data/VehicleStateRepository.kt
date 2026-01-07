package com.max.voiceassistant.data

import com.max.voiceassistant.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 车辆状态Repository
 * 管理车辆的各种状态（Mock实现）
 */
class VehicleStateRepository {
    
    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()
    
    /**
     * 获取当前状态
     */
    fun getCurrentState(): VehicleState = _vehicleState.value
    
    // ========== 空调控制 ==========
    
    fun updateACState(acState: ACState) {
        _vehicleState.value = _vehicleState.value.copy(ac = acState)
    }
    
    // ========== 座椅控制 ==========
    
    fun updateSeatState(seatState: SeatState) {
        _vehicleState.value = _vehicleState.value.copy(seat = seatState)
    }
    
    // ========== 车窗控制 ==========
    
    fun updateWindowState(windowState: WindowState) {
        _vehicleState.value = _vehicleState.value.copy(window = windowState)
    }
    
    // ========== 灯光控制 ==========
    
    fun updateLightState(lightState: LightState) {
        _vehicleState.value = _vehicleState.value.copy(light = lightState)
    }
    
    // ========== 车门控制 ==========
    
    fun updateDoorState(doorState: DoorState) {
        _vehicleState.value = _vehicleState.value.copy(door = doorState)
    }
    
    // ========== 引擎控制 ==========
    
    fun updateEngineState(engineState: EngineState) {
        _vehicleState.value = _vehicleState.value.copy(engine = engineState)
    }
    
    /**
     * 重置所有状态
     */
    fun resetAll() {
        _vehicleState.value = VehicleState()
    }
}
