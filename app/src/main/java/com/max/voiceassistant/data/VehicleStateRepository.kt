package com.max.voiceassistant.data

import com.max.voiceassistant.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 车辆状态仓库（Mock）。
 *
 * 持有一份 [VehicleState]，通过 [vehicleState] 对外暴露 [StateFlow]；
 * 提供各子状态的更新方法，供 [VehicleControlExecutor] 调用。
 */
class VehicleStateRepository {

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    /** 当前整车状态快照。 */
    fun getCurrentState(): VehicleState = _vehicleState.value

    /** 更新空调状态。 */
    fun updateACState(acState: ACState) {
        _vehicleState.value = _vehicleState.value.copy(ac = acState)
    }

    /** 更新座椅状态。 */
    fun updateSeatState(seatState: SeatState) {
        _vehicleState.value = _vehicleState.value.copy(seat = seatState)
    }

    /** 更新车窗状态。 */
    fun updateWindowState(windowState: WindowState) {
        _vehicleState.value = _vehicleState.value.copy(window = windowState)
    }

    /** 更新灯光状态。 */
    fun updateLightState(lightState: LightState) {
        _vehicleState.value = _vehicleState.value.copy(light = lightState)
    }

    /** 更新车门状态。 */
    fun updateDoorState(doorState: DoorState) {
        _vehicleState.value = _vehicleState.value.copy(door = doorState)
    }

    /** 更新引擎状态。 */
    fun updateEngineState(engineState: EngineState) {
        _vehicleState.value = _vehicleState.value.copy(engine = engineState)
    }

    /** 重置为初始整车状态。 */
    fun resetAll() {
        _vehicleState.value = VehicleState()
    }
}
