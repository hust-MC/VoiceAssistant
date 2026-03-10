package com.max.voiceassistant.model

/**
 * 整车状态聚合：空调、座椅、车窗、灯光、车门、引擎。
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
 * 空调状态。
 *
 * @param isOn 是否开启
 * @param temperature 设定温度（[MIN_TEMPERATURE]–[MAX_TEMPERATURE]）
 * @param fanSpeed 风速档位（[MIN_FAN_SPEED]–[MAX_FAN_SPEED]）
 * @param mode 制冷/制热/自动
 */
data class ACState(
    val isOn: Boolean = false,
    val temperature: Int = 24,
    val fanSpeed: Int = 3,
    val mode: ACMode = ACMode.AUTO
) {
    companion object {
        const val MIN_TEMPERATURE = 16
        const val MAX_TEMPERATURE = 32
        const val MIN_FAN_SPEED = 1
        const val MAX_FAN_SPEED = 5
    }
}

/** 空调模式（展示名用于 UI 本地化前兼容）。 */
enum class ACMode(val displayName: String) {
    AUTO("自动"),
    COOL("制冷"),
    HEAT("制热")
}

/**
 * 座椅状态：前后位置、加热、通风。
 */
data class SeatState(
    val position: Int = 3,
    val heating: Boolean = false,
    val ventilation: Boolean = false
) {
    companion object {
        const val MIN_POSITION = 1
        const val MAX_POSITION = 5
    }
}

/**
 * 车窗状态：四窗开合度 0–100，天窗是否开。
 */
data class WindowState(
    val frontLeft: Int = CLOSED,
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

/** 灯光状态：大灯开关/模式、氛围灯开关与颜色。 */
data class LightState(
    val headlight: Boolean = false,
    val headlightMode: HeadlightMode = HeadlightMode.OFF,
    val ambientLight: Boolean = false,
    val ambientColor: String = "蓝色"
)

/** 大灯模式。 */
enum class HeadlightMode(val displayName: String) {
    OFF("关闭"),
    ON("开启"),
    AUTO("自动")
}

/** 车门锁与后备箱开合状态。 */
data class DoorState(
    val isLocked: Boolean = true,
    val trunkOpen: Boolean = false
)

/** 引擎是否处于运行状态。 */
data class EngineState(
    val isRunning: Boolean = false
)
