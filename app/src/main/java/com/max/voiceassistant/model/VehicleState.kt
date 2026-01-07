package com.max.voiceassistant.model

/**
 * 车辆状态
 */
data class VehicleState(
    val ac: ACState = ACState(),
    val seat: SeatState = SeatState(),
    val window: WindowState = WindowState(),
    val light: LightState = LightState(),
    val door: DoorState = DoorState(),
    val engine: EngineState = EngineState()
)

/**
 * 空调状态
 */
data class ACState(
    val isOn: Boolean = false,
    val temperature: Int = 24,     // 16-32度
    val fanSpeed: Int = 3,          // 1-5档
    val mode: ACMode = ACMode.AUTO
) {
    companion object {
        const val MIN_TEMPERATURE = 16
        const val MAX_TEMPERATURE = 32
        const val MIN_FAN_SPEED = 1
        const val MAX_FAN_SPEED = 5
    }
}

/**
 * 空调模式
 */
enum class ACMode(val displayName: String) {
    AUTO("自动"),
    COOL("制冷"),
    HEAT("制热")
}

/**
 * 座椅状态
 */
data class SeatState(
    val position: Int = 3,          // 1-5档
    val heating: Boolean = false,   // 加热
    val ventilation: Boolean = false // 通风
) {
    companion object {
        const val MIN_POSITION = 1
        const val MAX_POSITION = 5
    }
}

/**
 * 车窗状态
 */
data class WindowState(
    val frontLeft: Int = CLOSED,    // 0-100%
    val frontRight: Int = CLOSED,
    val rearLeft: Int = CLOSED,
    val rearRight: Int = CLOSED,
    val sunroof: Boolean = false
) {
    companion object {
        const val CLOSED = 0
        const val HALF_OPEN = 50
        const val FULL_OPEN = 100
    }
}

/**
 * 灯光状态
 */
data class LightState(
    val headlight: Boolean = false,
    val headlightMode: HeadlightMode = HeadlightMode.OFF,
    val ambientLight: Boolean = false,
    val ambientColor: String = "蓝色"
)

/**
 * 大灯模式
 */
enum class HeadlightMode(val displayName: String) {
    OFF("关闭"),
    ON("开启"),
    AUTO("自动")
}

/**
 * 车门状态
 */
data class DoorState(
    val isLocked: Boolean = true,
    val trunkOpen: Boolean = false
)

/**
 * 引擎状态
 */
data class EngineState(
    val isRunning: Boolean = false
)
