package com.max.voiceassistant.executor

import com.max.voiceassistant.model.Command
import com.max.voiceassistant.model.CommandResult
import com.max.voiceassistant.model.CommandType
import java.text.SimpleDateFormat
import java.util.*

/**
 * 信息查询执行器
 * 处理时间、日期、天气、计算等查询
 */
class QueryExecutor {
    
    fun execute(command: Command): CommandResult {
        return when (command.type) {
            CommandType.QUERY_TIME -> executeQueryTime()
            CommandType.QUERY_DATE -> executeQueryDate()
            CommandType.QUERY_DAY_OF_WEEK -> executeQueryDayOfWeek()
            CommandType.QUERY_WEATHER -> executeQueryWeather(command.params)
            CommandType.QUERY_CALCULATE -> executeCalculate(command.params)
            else -> CommandResult.Error("不支持的查询命令")
        }
    }
    
    // ========== 时间查询 ==========
    
    private fun executeQueryTime(): CommandResult {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timeDesc = when {
            hour < 6 -> "凌晨"
            hour < 9 -> "早上"
            hour < 12 -> "上午"
            hour == 12 -> "中午"
            hour < 14 -> "中午"
            hour < 18 -> "下午"
            hour < 20 -> "傍晚"
            else -> "晚上"
        }
        
        return CommandResult.Success("现在是${timeDesc}${hour}点${minute}分")
    }
    
    private fun executeQueryDate(): CommandResult {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        return CommandResult.Success("今天是${year}年${month}月${day}日")
    }
    
    private fun executeQueryDayOfWeek(): CommandResult {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val dayName = when (dayOfWeek) {
            Calendar.SUNDAY -> "星期日"
            Calendar.MONDAY -> "星期一"
            Calendar.TUESDAY -> "星期二"
            Calendar.WEDNESDAY -> "星期三"
            Calendar.THURSDAY -> "星期四"
            Calendar.FRIDAY -> "星期五"
            Calendar.SATURDAY -> "星期六"
            else -> "未知"
        }
        
        return CommandResult.Success("今天是$dayName")
    }
    
    // ========== 天气查询 ==========
    
    /**
     * 查询天气
     * 注意：实际项目中需要调用天气API
     * 这里使用模拟数据
     */
    private fun executeQueryWeather(params: Map<String, String>): CommandResult {
        val city = params["city"] ?: "北京"
        
        // 模拟天气数据
        val mockWeather = generateMockWeather(city)
        
        return CommandResult.Success(
            "${city}今天的天气：${mockWeather.condition}，" +
            "温度${mockWeather.tempLow}到${mockWeather.tempHigh}度，" +
            "${mockWeather.suggestion}"
        )
    }
    
    private fun generateMockWeather(city: String): WeatherInfo {
        // 根据城市和日期生成模拟天气（保证每次查询相同城市结果一致）
        val random = Random(city.hashCode().toLong() + getDayOfYear())
        
        val conditions = listOf("晴", "多云", "阴", "小雨", "阵雨")
        val condition = conditions[random.nextInt(conditions.size)]
        
        // 根据季节设定温度范围
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val (baseLow, baseHigh) = when (month) {
            in 1..2 -> Pair(-5, 5)      // 冬季
            3 -> Pair(5, 15)             // 初春
            in 4..5 -> Pair(15, 25)      // 春季
            in 6..8 -> Pair(25, 35)      // 夏季
            in 9..10 -> Pair(15, 25)     // 秋季
            in 11..12 -> Pair(0, 10)     // 初冬
            else -> Pair(15, 25)
        }
        
        val tempLow = baseLow + random.nextInt(5)
        val tempHigh = baseHigh + random.nextInt(5)
        
        val suggestion = when (condition) {
            "晴" -> "适合出行"
            "多云" -> "天气舒适"
            "阴" -> "可能有雨，建议带伞"
            "小雨", "阵雨" -> "出门记得带伞"
            else -> "注意天气变化"
        }
        
        return WeatherInfo(city, condition, tempLow, tempHigh, suggestion)
    }
    
    private fun getDayOfYear(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_YEAR)
    }
    
    // ========== 计算 ==========
    
    /**
     * 简单计算
     * 支持：加、减、乘、除
     */
    private fun executeCalculate(params: Map<String, String>): CommandResult {
        val expression = params["expression"] ?: return CommandResult.Error("请说出要计算的内容")
        
        return try {
            val result = parseAndCalculate(expression)
            if (result != null) {
                val formattedResult = if (result == result.toLong().toDouble()) {
                    result.toLong().toString()
                } else {
                    String.format("%.2f", result)
                }
                CommandResult.Success("计算结果是$formattedResult")
            } else {
                CommandResult.Error("无法计算，请说清楚，例如：123加456等于多少")
            }
        } catch (e: Exception) {
            CommandResult.Error("计算失败：${e.message}")
        }
    }
    
    /**
     * 解析并计算表达式
     * 支持中文和符号表达
     */
    private fun parseAndCalculate(expression: String): Double? {
        // 标准化表达式
        var normalized = expression
            .replace("加", "+")
            .replace("减", "-")
            .replace("乘", "*")
            .replace("乘以", "*")
            .replace("除", "/")
            .replace("除以", "/")
            .replace("等于", "")
            .replace("多少", "")
            .replace("是", "")
            .replace(" ", "")
        
        // 提取数字和运算符
        val pattern = """(-?\d+\.?\d*)([\+\-\*/])(-?\d+\.?\d*)""".toRegex()
        val match = pattern.find(normalized)
        
        if (match != null) {
            val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
            val operator = match.groupValues[2]
            val num2 = match.groupValues[3].toDoubleOrNull() ?: return null
            
            return when (operator) {
                "+" -> num1 + num2
                "-" -> num1 - num2
                "*" -> num1 * num2
                "/" -> if (num2 != 0.0) num1 / num2 else null
                else -> null
            }
        }
        
        // 尝试更宽松的匹配（比如"一百加二百"）
        return parseChineseNumbers(expression)
    }
    
    /**
     * 解析包含中文数字的表达式
     */
    private fun parseChineseNumbers(expression: String): Double? {
        // 简化实现，只处理阿拉伯数字
        // 实际项目中可以添加中文数字转换
        return null
    }
    
    /**
     * 天气信息数据类
     */
    private data class WeatherInfo(
        val city: String,
        val condition: String,
        val tempLow: Int,
        val tempHigh: Int,
        val suggestion: String
    )
}

