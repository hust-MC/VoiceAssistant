package com.max.voiceassistant.model

/**
 * 命令类型
 */
enum class CommandType {
    // ========== 媒体控制 ==========
    MEDIA_PLAY,         // 播放
    MEDIA_PAUSE,        // 暂停
    MEDIA_STOP,         // 停止
    MEDIA_NEXT,         // 下一首
    MEDIA_PREVIOUS,     // 上一首
    VOLUME_UP,          // 音量增大
    VOLUME_DOWN,        // 音量减小
    VOLUME_MUTE,        // 静音
    VOLUME_UNMUTE,      // 取消静音
    
    // ========== 系统控制 ==========
    BRIGHTNESS_UP,      // 亮度增大
    BRIGHTNESS_DOWN,    // 亮度减小
    WIFI_ON,            // 打开WiFi
    WIFI_OFF,           // 关闭WiFi
    WIFI_STATUS,        // WiFi状态
    BLUETOOTH_ON,       // 打开蓝牙
    BLUETOOTH_OFF,      // 关闭蓝牙
    BLUETOOTH_STATUS,   // 蓝牙状态
    OPEN_SETTINGS,      // 打开设置
    
    // ========== 车辆控制（Mock）==========
    // 空调
    AC_ON,              // 打开空调
    AC_OFF,             // 关闭空调
    AC_TEMP_UP,         // 温度升高
    AC_TEMP_DOWN,       // 温度降低
    AC_TEMP_SET,        // 设置温度
    AC_FAN_UP,          // 风速增大
    AC_FAN_DOWN,        // 风速减小
    AC_MODE_AUTO,       // 自动模式
    AC_MODE_COOL,       // 制冷模式
    AC_MODE_HEAT,       // 制热模式
    
    // 座椅
    SEAT_FORWARD,       // 座椅前移
    SEAT_BACKWARD,      // 座椅后移
    SEAT_HEAT_ON,       // 座椅加热开
    SEAT_HEAT_OFF,      // 座椅加热关
    SEAT_VENTILATION_ON,  // 座椅通风开
    SEAT_VENTILATION_OFF, // 座椅通风关
    SEAT_RESET,         // 座椅复位
    
    // 车窗
    WINDOW_FRONT_OPEN,  // 前窗打开
    WINDOW_FRONT_CLOSE, // 前窗关闭
    WINDOW_FRONT_HALF,  // 前窗半开
    WINDOW_REAR_OPEN,   // 后窗打开
    WINDOW_REAR_CLOSE,  // 后窗关闭
    SUNROOF_OPEN,       // 天窗打开
    SUNROOF_CLOSE,      // 天窗关闭
    
    // 灯光
    LIGHT_HEADLIGHT_ON,     // 大灯开
    LIGHT_HEADLIGHT_OFF,    // 大灯关
    LIGHT_HEADLIGHT_AUTO,   // 大灯自动
    LIGHT_AMBIENT_ON,       // 氛围灯开
    LIGHT_AMBIENT_OFF,      // 氛围灯关
    LIGHT_AMBIENT_COLOR,    // 氛围灯颜色
    
    // 车门
    DOOR_LOCK,          // 锁车
    DOOR_UNLOCK,        // 解锁
    TRUNK_OPEN,         // 后备箱打开
    TRUNK_CLOSE,        // 后备箱关闭
    
    // 引擎
    ENGINE_START,       // 启动
    ENGINE_STOP,        // 熄火
    
    // ========== 信息查询 ==========
    QUERY_TIME,         // 查询时间
    QUERY_DATE,         // 查询日期
    QUERY_DAY_OF_WEEK,  // 查询星期
    QUERY_WEATHER,      // 查询天气
    QUERY_CALCULATE,    // 计算
    
    // 未知
    UNKNOWN
}

/**
 * 命令类别
 */
enum class CommandCategory {
    MEDIA,      // 媒体
    SYSTEM,     // 系统
    VEHICLE,    // 车辆
    QUERY,      // 查询
    UNKNOWN     // 未知
}

/**
 * 命令数据类
 */
data class Command(
    val type: CommandType,
    val category: CommandCategory,
    val params: Map<String, String> = emptyMap()
)

/**
 * 命令执行结果
 */
sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}
