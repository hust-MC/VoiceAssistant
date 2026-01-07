package com.max.voiceassistant.intent

import com.max.voiceassistant.model.Command
import com.max.voiceassistant.model.CommandCategory
import com.max.voiceassistant.model.CommandType

/**
 * 意图识别解析器
 * 将用户的语音识别文本解析为具体的命令
 */
class IntentParser {
    
    /**
     * 解析用户输入，返回对应的命令
     */
    fun parse(text: String): Command {
        val normalizedText = text.lowercase().trim()
        
        // 1. 先尝试常见表达映射（处理隐含意图）
        findExpressionMatch(normalizedText)?.let { return it }
        
        // 2. 再尝试规则匹配
        return when {
            isMediaCommand(normalizedText) -> parseMediaCommand(normalizedText)
            isVehicleCommand(normalizedText) -> parseVehicleCommand(normalizedText)
            isSystemCommand(normalizedText) -> parseSystemCommand(normalizedText)
            isQueryCommand(normalizedText) -> parseQueryCommand(normalizedText)
            else -> Command(CommandType.UNKNOWN, CommandCategory.UNKNOWN)
        }
    }
    
    // ========== 常见表达映射（处理隐含意图）==========
    
    /**
     * 常见表达映射表
     * 用于处理"太冷了"这种不直接说明意图的表达
     */
    private val expressionMap = listOf(
        // 温度相关的隐含表达
        ExpressionMapping(listOf("太冷了", "好冷", "冻死了", "有点冷", "冷死了", "很冷"), 
            Command(CommandType.AC_TEMP_UP, CommandCategory.VEHICLE)),
        ExpressionMapping(listOf("太热了", "好热", "热死了", "有点热", "很热", "闷"), 
            Command(CommandType.AC_TEMP_DOWN, CommandCategory.VEHICLE)),
        
        // 风速相关
        ExpressionMapping(listOf("风太大了", "风好大", "吹得难受"), 
            Command(CommandType.AC_FAN_DOWN, CommandCategory.VEHICLE)),
        ExpressionMapping(listOf("风太小了", "不够凉快", "没感觉"), 
            Command(CommandType.AC_FAN_UP, CommandCategory.VEHICLE)),
        
        // 灯光相关
        ExpressionMapping(listOf("太暗了", "看不清", "黑"), 
            Command(CommandType.BRIGHTNESS_UP, CommandCategory.SYSTEM)),
        ExpressionMapping(listOf("太亮了", "刺眼", "晃眼"), 
            Command(CommandType.BRIGHTNESS_DOWN, CommandCategory.SYSTEM)),
    )
    
    private fun findExpressionMatch(text: String): Command? {
        for (mapping in expressionMap) {
            if (mapping.expressions.any { text.contains(it) }) {
                return mapping.command
            }
        }
        return null
    }
    
    // ========== 判断命令大类 ==========
    
    private fun isMediaCommand(text: String): Boolean {
        val keywords = listOf(
            "播放", "暂停", "停止", "音乐", "歌", "歌曲",
            "下一首", "上一首", "下一曲", "上一曲", "切歌",
            "音量", "声音", "静音"
        )
        return keywords.any { text.contains(it) }
    }
    
    private fun isVehicleCommand(text: String): Boolean {
        val keywords = listOf(
            "空调", "温度", "风速", "风量", "制冷", "制热", "暖风", "冷风",
            "座椅", "加热", "通风",
            "车窗", "窗户", "天窗", "前窗", "后窗",
            "大灯", "氛围灯", "灯光",
            "锁车", "解锁", "车门", "后备箱",
            "启动", "熄火", "点火", "发动"
        )
        return keywords.any { text.contains(it) }
    }
    
    private fun isSystemCommand(text: String): Boolean {
        val keywords = listOf(
            "wifi", "无线网", "网络",
            "蓝牙",
            "亮度", "屏幕",
            "设置"
        )
        return keywords.any { text.contains(it) }
    }
    
    private fun isQueryCommand(text: String): Boolean {
        val keywords = listOf(
            "几点", "时间", "现在",
            "几号", "日期", "今天",
            "星期", "周几",
            "天气",
            "等于", "加", "减", "乘", "除", "多少"
        )
        return keywords.any { text.contains(it) }
    }
    
    // ========== 解析媒体命令 ==========
    
    private fun parseMediaCommand(text: String): Command {
        return when {
            // 播放
            containsAny(text, "播放", "放歌", "听歌", "放音乐", "听音乐") && 
            !containsAny(text, "暂停", "停止") ->
                Command(CommandType.MEDIA_PLAY, CommandCategory.MEDIA)
            
            // 暂停
            containsAny(text, "暂停", "停止播放", "暂停播放", "停止音乐") ->
                Command(CommandType.MEDIA_PAUSE, CommandCategory.MEDIA)
            
            // 停止
            text.contains("停止") && !text.contains("播放") ->
                Command(CommandType.MEDIA_STOP, CommandCategory.MEDIA)
            
            // 下一首
            containsAny(text, "下一首", "下一曲", "切歌", "换一首", "下首") ->
                Command(CommandType.MEDIA_NEXT, CommandCategory.MEDIA)
            
            // 上一首
            containsAny(text, "上一首", "上一曲", "上首") ->
                Command(CommandType.MEDIA_PREVIOUS, CommandCategory.MEDIA)
            
            // 音量增大
            containsAny(text, "音量", "声音") && 
            containsAny(text, "大", "高", "加", "增", "调大", "调高", "提高") ->
                Command(CommandType.VOLUME_UP, CommandCategory.MEDIA)
            
            // 音量减小
            containsAny(text, "音量", "声音") && 
            containsAny(text, "小", "低", "减", "降", "调小", "调低") ->
                Command(CommandType.VOLUME_DOWN, CommandCategory.MEDIA)
            
            // 静音
            text.contains("静音") && !text.contains("取消") ->
                Command(CommandType.VOLUME_MUTE, CommandCategory.MEDIA)
            
            // 取消静音
            containsAny(text, "取消静音", "关闭静音") || 
            (containsAny(text, "打开", "恢复") && containsAny(text, "声音", "音量")) ->
                Command(CommandType.VOLUME_UNMUTE, CommandCategory.MEDIA)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.MEDIA)
        }
    }
    
    // ========== 解析车辆命令 ==========
    
    private fun parseVehicleCommand(text: String): Command {
        return when {
            // 空调相关
            containsAny(text, "空调", "温度", "风速", "风量", "制冷", "制热", "暖风", "冷风") ->
                parseACCommand(text)
            
            // 座椅相关
            text.contains("座椅") ->
                parseSeatCommand(text)
            
            // 车窗相关
            containsAny(text, "车窗", "窗户", "天窗", "前窗", "后窗") ->
                parseWindowCommand(text)
            
            // 灯光相关
            containsAny(text, "大灯", "氛围灯", "灯光", "灯") ->
                parseLightCommand(text)
            
            // 车门相关
            containsAny(text, "锁车", "解锁", "车门", "后备箱") ->
                parseDoorCommand(text)
            
            // 引擎相关
            containsAny(text, "启动", "熄火", "点火", "发动") ->
                parseEngineCommand(text)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    /**
     * 解析空调命令
     */
    private fun parseACCommand(text: String): Command {
        return when {
            // 打开空调
            containsAny(text, "打开", "开启", "开") && text.contains("空调") ->
                Command(CommandType.AC_ON, CommandCategory.VEHICLE)
            
            // 关闭空调
            containsAny(text, "关闭", "关掉", "关") && text.contains("空调") ->
                Command(CommandType.AC_OFF, CommandCategory.VEHICLE)
            
            // 设置指定温度
            text.contains("温度") && containsAny(text, "调到", "设置", "设为", "调成") -> {
                val temp = extractNumber(text)
                Command(CommandType.AC_TEMP_SET, CommandCategory.VEHICLE,
                    mapOf("temperature" to (temp?.toString() ?: "24")))
            }
            
            // 直接说温度数字
            text.matches(Regex(".*\\d+度.*")) && !containsAny(text, "高", "低", "加", "减") -> {
                val temp = extractNumber(text)
                Command(CommandType.AC_TEMP_SET, CommandCategory.VEHICLE,
                    mapOf("temperature" to (temp?.toString() ?: "24")))
            }
            
            // 温度调高
            text.contains("温度") && containsAny(text, "高", "加", "升", "调高", "升高") ->
                Command(CommandType.AC_TEMP_UP, CommandCategory.VEHICLE)
            
            // 温度调低
            text.contains("温度") && containsAny(text, "低", "减", "降", "调低", "降低") ->
                Command(CommandType.AC_TEMP_DOWN, CommandCategory.VEHICLE)
            
            // 风速调大
            containsAny(text, "风速", "风量", "风") && 
            containsAny(text, "大", "高", "加", "强", "调大") ->
                Command(CommandType.AC_FAN_UP, CommandCategory.VEHICLE)
            
            // 风速调小
            containsAny(text, "风速", "风量", "风") && 
            containsAny(text, "小", "低", "减", "弱", "调小") ->
                Command(CommandType.AC_FAN_DOWN, CommandCategory.VEHICLE)
            
            // 自动模式
            text.contains("自动") && containsAny(text, "模式", "空调") ->
                Command(CommandType.AC_MODE_AUTO, CommandCategory.VEHICLE)
            
            // 制冷模式
            containsAny(text, "制冷", "冷风", "冷气") ->
                Command(CommandType.AC_MODE_COOL, CommandCategory.VEHICLE)
            
            // 制热模式
            containsAny(text, "制热", "暖风", "暖气") ->
                Command(CommandType.AC_MODE_HEAT, CommandCategory.VEHICLE)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    /**
     * 解析座椅命令
     */
    private fun parseSeatCommand(text: String): Command {
        return when {
            // 座椅前移
            text.contains("座椅") && containsAny(text, "前", "往前", "向前") ->
                Command(CommandType.SEAT_FORWARD, CommandCategory.VEHICLE)
            
            // 座椅后移
            text.contains("座椅") && containsAny(text, "后", "往后", "向后") ->
                Command(CommandType.SEAT_BACKWARD, CommandCategory.VEHICLE)
            
            // 座椅加热打开
            text.contains("座椅") && text.contains("加热") && 
            containsAny(text, "打开", "开启", "开") ->
                Command(CommandType.SEAT_HEAT_ON, CommandCategory.VEHICLE)
            
            // 座椅加热（默认打开）
            text.contains("座椅") && text.contains("加热") && 
            !containsAny(text, "关闭", "关掉", "关") ->
                Command(CommandType.SEAT_HEAT_ON, CommandCategory.VEHICLE)
            
            // 座椅加热关闭
            text.contains("座椅") && text.contains("加热") && 
            containsAny(text, "关闭", "关掉", "关") ->
                Command(CommandType.SEAT_HEAT_OFF, CommandCategory.VEHICLE)
            
            // 座椅通风打开
            text.contains("座椅") && text.contains("通风") && 
            containsAny(text, "打开", "开启", "开") ->
                Command(CommandType.SEAT_VENTILATION_ON, CommandCategory.VEHICLE)
            
            // 座椅通风（默认打开）
            text.contains("座椅") && text.contains("通风") && 
            !containsAny(text, "关闭", "关掉", "关") ->
                Command(CommandType.SEAT_VENTILATION_ON, CommandCategory.VEHICLE)
            
            // 座椅通风关闭
            text.contains("座椅") && text.contains("通风") && 
            containsAny(text, "关闭", "关掉", "关") ->
                Command(CommandType.SEAT_VENTILATION_OFF, CommandCategory.VEHICLE)
            
            // 座椅复位
            text.contains("座椅") && containsAny(text, "复位", "恢复", "回位") ->
                Command(CommandType.SEAT_RESET, CommandCategory.VEHICLE)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    /**
     * 解析车窗命令
     */
    private fun parseWindowCommand(text: String): Command {
        return when {
            // 前窗打开
            containsAny(text, "前窗", "前面窗") && 
            containsAny(text, "打开", "开", "降下", "放下") ->
                Command(CommandType.WINDOW_FRONT_OPEN, CommandCategory.VEHICLE)
            
            // 前窗关闭
            containsAny(text, "前窗", "前面窗") && 
            containsAny(text, "关闭", "关", "升起", "收起") ->
                Command(CommandType.WINDOW_FRONT_CLOSE, CommandCategory.VEHICLE)
            
            // 前窗一半
            containsAny(text, "前窗", "前面窗") && 
            containsAny(text, "一半", "50%", "半开") ->
                Command(CommandType.WINDOW_FRONT_HALF, CommandCategory.VEHICLE)
            
            // 后窗打开
            containsAny(text, "后窗", "后面窗") && 
            containsAny(text, "打开", "开", "降下", "放下") ->
                Command(CommandType.WINDOW_REAR_OPEN, CommandCategory.VEHICLE)
            
            // 后窗关闭
            containsAny(text, "后窗", "后面窗") && 
            containsAny(text, "关闭", "关", "升起", "收起") ->
                Command(CommandType.WINDOW_REAR_CLOSE, CommandCategory.VEHICLE)
            
            // 天窗打开
            text.contains("天窗") && containsAny(text, "打开", "开") ->
                Command(CommandType.SUNROOF_OPEN, CommandCategory.VEHICLE)
            
            // 天窗关闭
            text.contains("天窗") && containsAny(text, "关闭", "关") ->
                Command(CommandType.SUNROOF_CLOSE, CommandCategory.VEHICLE)
            
            // 通用车窗打开
            containsAny(text, "车窗", "窗户") && 
            containsAny(text, "打开", "开", "降下") ->
                Command(CommandType.WINDOW_FRONT_OPEN, CommandCategory.VEHICLE)
            
            // 通用车窗关闭
            containsAny(text, "车窗", "窗户") && 
            containsAny(text, "关闭", "关", "升起") ->
                Command(CommandType.WINDOW_FRONT_CLOSE, CommandCategory.VEHICLE)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    /**
     * 解析灯光命令
     */
    private fun parseLightCommand(text: String): Command {
        return when {
            // 大灯打开
            text.contains("大灯") && containsAny(text, "打开", "开") ->
                Command(CommandType.LIGHT_HEADLIGHT_ON, CommandCategory.VEHICLE)
            
            // 大灯关闭
            text.contains("大灯") && containsAny(text, "关闭", "关") ->
                Command(CommandType.LIGHT_HEADLIGHT_OFF, CommandCategory.VEHICLE)
            
            // 大灯自动
            text.contains("大灯") && text.contains("自动") ->
                Command(CommandType.LIGHT_HEADLIGHT_AUTO, CommandCategory.VEHICLE)
            
            // 氛围灯打开
            text.contains("氛围灯") && containsAny(text, "打开", "开") ->
                Command(CommandType.LIGHT_AMBIENT_ON, CommandCategory.VEHICLE)
            
            // 氛围灯关闭
            text.contains("氛围灯") && containsAny(text, "关闭", "关") ->
                Command(CommandType.LIGHT_AMBIENT_OFF, CommandCategory.VEHICLE)
            
            // 氛围灯颜色
            text.contains("氛围灯") && containsAny(text, "红", "蓝", "绿", "紫", "白", "黄", "橙") -> {
                val color = extractColor(text)
                Command(CommandType.LIGHT_AMBIENT_COLOR, CommandCategory.VEHICLE,
                    mapOf("color" to color))
            }
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    /**
     * 解析车门命令
     */
    private fun parseDoorCommand(text: String): Command {
        return when {
            // 锁车
            containsAny(text, "锁车", "锁定", "上锁") || 
            (text.contains("车门") && containsAny(text, "锁", "锁定")) ->
                Command(CommandType.DOOR_LOCK, CommandCategory.VEHICLE)
            
            // 解锁
            containsAny(text, "解锁", "开锁") || 
            (text.contains("车门") && containsAny(text, "解锁", "打开")) ->
                Command(CommandType.DOOR_UNLOCK, CommandCategory.VEHICLE)
            
            // 后备箱打开
            text.contains("后备箱") && containsAny(text, "打开", "开") ->
                Command(CommandType.TRUNK_OPEN, CommandCategory.VEHICLE)
            
            // 后备箱关闭
            text.contains("后备箱") && containsAny(text, "关闭", "关") ->
                Command(CommandType.TRUNK_CLOSE, CommandCategory.VEHICLE)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    /**
     * 解析引擎命令
     */
    private fun parseEngineCommand(text: String): Command {
        return when {
            // 启动
            containsAny(text, "启动", "点火", "发动") ->
                Command(CommandType.ENGINE_START, CommandCategory.VEHICLE)
            
            // 熄火
            containsAny(text, "熄火", "停车", "关闭发动机") ->
                Command(CommandType.ENGINE_STOP, CommandCategory.VEHICLE)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.VEHICLE)
        }
    }
    
    // ========== 解析系统命令 ==========
    
    private fun parseSystemCommand(text: String): Command {
        return when {
            // 亮度调高
            containsAny(text, "屏幕", "亮度") && 
            containsAny(text, "亮", "高", "加", "调亮", "调高") ->
                Command(CommandType.BRIGHTNESS_UP, CommandCategory.SYSTEM)
            
            // 亮度调低
            containsAny(text, "屏幕", "亮度") && 
            containsAny(text, "暗", "低", "减", "调暗", "调低") ->
                Command(CommandType.BRIGHTNESS_DOWN, CommandCategory.SYSTEM)
            
            // WiFi打开
            containsAny(text, "wifi", "无线网", "网络") && 
            containsAny(text, "打开", "开启", "连接") ->
                Command(CommandType.WIFI_ON, CommandCategory.SYSTEM)
            
            // WiFi关闭
            containsAny(text, "wifi", "无线网", "网络") && 
            containsAny(text, "关闭", "关掉", "断开") ->
                Command(CommandType.WIFI_OFF, CommandCategory.SYSTEM)
            
            // WiFi状态
            containsAny(text, "wifi", "无线网") && 
            containsAny(text, "状态", "开了吗", "连上了吗") ->
                Command(CommandType.WIFI_STATUS, CommandCategory.SYSTEM)
            
            // 蓝牙打开
            text.contains("蓝牙") && containsAny(text, "打开", "开启", "连接") ->
                Command(CommandType.BLUETOOTH_ON, CommandCategory.SYSTEM)
            
            // 蓝牙关闭
            text.contains("蓝牙") && containsAny(text, "关闭", "关掉", "断开") ->
                Command(CommandType.BLUETOOTH_OFF, CommandCategory.SYSTEM)
            
            // 蓝牙状态
            text.contains("蓝牙") && containsAny(text, "状态", "开了吗") ->
                Command(CommandType.BLUETOOTH_STATUS, CommandCategory.SYSTEM)
            
            // 打开设置
            text.contains("设置") && containsAny(text, "打开", "进入") ->
                Command(CommandType.OPEN_SETTINGS, CommandCategory.SYSTEM)
            
            // 单独说"设置"
            text == "设置" ->
                Command(CommandType.OPEN_SETTINGS, CommandCategory.SYSTEM)
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.SYSTEM)
        }
    }
    
    // ========== 解析查询命令 ==========
    
    private fun parseQueryCommand(text: String): Command {
        return when {
            // 查询时间
            containsAny(text, "几点", "时间", "现在几点") ->
                Command(CommandType.QUERY_TIME, CommandCategory.QUERY)
            
            // 查询日期
            containsAny(text, "几号", "日期", "今天几号") ->
                Command(CommandType.QUERY_DATE, CommandCategory.QUERY)
            
            // 查询星期
            containsAny(text, "星期几", "周几", "礼拜几") ->
                Command(CommandType.QUERY_DAY_OF_WEEK, CommandCategory.QUERY)
            
            // 查询天气
            text.contains("天气") -> {
                val city = extractCity(text)
                Command(CommandType.QUERY_WEATHER, CommandCategory.QUERY,
                    mapOf("city" to city))
            }
            
            // 计算
            containsAny(text, "等于", "多少") || 
            (text.any { it.isDigit() } && containsAny(text, "加", "减", "乘", "除", "+", "-", "*", "/")) -> {
                Command(CommandType.QUERY_CALCULATE, CommandCategory.QUERY,
                    mapOf("expression" to text))
            }
            
            else -> Command(CommandType.UNKNOWN, CommandCategory.QUERY)
        }
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 检查文本是否包含任意一个关键词
     */
    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    /**
     * 从文本中提取数字
     */
    private fun extractNumber(text: String): Int? {
        val regex = """(\d+)""".toRegex()
        return regex.find(text)?.value?.toIntOrNull()
    }
    
    /**
     * 从文本中提取颜色
     */
    private fun extractColor(text: String): String {
        val colorMap = mapOf(
            "红" to "红色",
            "橙" to "橙色",
            "黄" to "黄色",
            "绿" to "绿色",
            "青" to "青色",
            "蓝" to "蓝色",
            "紫" to "紫色",
            "白" to "白色"
        )
        
        colorMap.entries.forEach { (key, value) ->
            if (text.contains(key)) return value
        }
        return "蓝色" // 默认颜色
    }
    
    /**
     * 从文本中提取城市
     */
    private fun extractCity(text: String): String {
        val cities = listOf(
            "北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "重庆",
            "武汉", "西安", "天津", "苏州", "郑州", "长沙", "青岛", "宁波"
        )
        cities.forEach { city ->
            if (text.contains(city)) return city
        }
        return "北京" // 默认城市
    }
    
    /**
     * 表达式映射数据类
     */
    private data class ExpressionMapping(
        val expressions: List<String>,
        val command: Command
    )
}

