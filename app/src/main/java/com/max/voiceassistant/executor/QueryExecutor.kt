package com.max.voiceassistant.executor

import android.content.Context
import com.max.voiceassistant.R
import com.max.voiceassistant.model.Command
import com.max.voiceassistant.model.CommandResult
import com.max.voiceassistant.model.CommandType
import java.util.*

/**
 * 信息查询执行器。
 *
 * 处理时间、日期、星期、天气（模拟）、简单四则运算等查询；
 * 文案均通过 [context] 做国际化。
 */
class QueryExecutor(private val context: Context) {
    private fun str(id: Int, vararg args: Any?) = context.getString(id, *args)

    /**
     * 根据命令类型执行对应查询并返回本地化结果。
     *
     * @param command 查询命令（类型 + 可选 params，如城市、表达式）
     * @return 成功带文案或错误提示
     */
    fun execute(command: Command): CommandResult {
        return when (command.type) {
            CommandType.QUERY_TIME -> executeQueryTime()
            CommandType.QUERY_DATE -> executeQueryDate()
            CommandType.QUERY_DAY_OF_WEEK -> executeQueryDayOfWeek()
            CommandType.QUERY_WEATHER -> executeQueryWeather(command.params)
            CommandType.QUERY_CALCULATE -> executeCalculate(command.params)
            else -> CommandResult.Error(str(R.string.query_unsupported))
        }
    }

    // ---------- 时间查询 ----------

    /** 返回当前时间（时段 + 时:分），文案本地化。 */
    private fun executeQueryTime(): CommandResult {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeDescResId = when {
            hour < 6 -> R.string.query_time_early_morning
            hour < 9 -> R.string.query_time_morning
            hour < 12 -> R.string.query_time_forenoon
            hour == 12 -> R.string.query_time_noon
            hour < 14 -> R.string.query_time_noon
            hour < 18 -> R.string.query_time_afternoon
            hour < 20 -> R.string.query_time_dusk
            else -> R.string.query_time_evening
        }
        val timeDesc = context.getString(timeDescResId)
        return CommandResult.Success(str(R.string.query_time_format, timeDesc, hour, minute))
    }

    /** 返回当前日期（年/月/日），文案本地化。 */
    private fun executeQueryDate(): CommandResult {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return CommandResult.Success(str(R.string.query_date_format, year, month, day))
    }

    /** 返回今天星期几，文案本地化。 */
    private fun executeQueryDayOfWeek(): CommandResult {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val dayNameResId = when (dayOfWeek) {
            Calendar.SUNDAY -> R.string.query_day_sunday
            Calendar.MONDAY -> R.string.query_day_monday
            Calendar.TUESDAY -> R.string.query_day_tuesday
            Calendar.WEDNESDAY -> R.string.query_day_wednesday
            Calendar.THURSDAY -> R.string.query_day_thursday
            Calendar.FRIDAY -> R.string.query_day_friday
            Calendar.SATURDAY -> R.string.query_day_saturday
            else -> R.string.query_day_unknown
        }
        val dayName = context.getString(dayNameResId)
        return CommandResult.Success(str(R.string.query_today_is, dayName))
    }

    // ---------- 天气查询 ----------

    /**
     * 查询天气（当前为模拟数据，可按城市+日期生成稳定结果）。
     *
     * @param params 可选 city，缺省为默认城市
     * @return 城市、天气、温度区间、建议的本地化文案
     */
    private fun executeQueryWeather(params: Map<String, String>): CommandResult {
        val city = params["city"] ?: context.getString(R.string.query_default_city)
        val mockWeather = generateMockWeather(city)
        return CommandResult.Success(
            str(R.string.query_weather_format, city, mockWeather.condition, mockWeather.tempLow, mockWeather.tempHigh, mockWeather.suggestion)
        )
    }

    /** 按城市+年内第几天生成可复现的模拟天气。 */
    private fun generateMockWeather(city: String): WeatherInfo {
        val random = Random(city.hashCode().toLong() + getDayOfYear())

        val conditionSuggestionPairs = listOf(
            R.string.query_weather_sunny to R.string.query_weather_suggestion_go,
            R.string.query_weather_cloudy to R.string.query_weather_suggestion_comfort,
            R.string.query_weather_overcast to R.string.query_weather_suggestion_umbrella_rain,
            R.string.query_weather_light_rain to R.string.query_weather_suggestion_umbrella,
            R.string.query_weather_shower to R.string.query_weather_suggestion_umbrella
        )
        val (conditionResId, suggestionResId) = conditionSuggestionPairs[random.nextInt(conditionSuggestionPairs.size)]
        val condition = context.getString(conditionResId)
        val suggestion = context.getString(suggestionResId)

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

        return WeatherInfo(city, condition, tempLow, tempHigh, suggestion)
    }

    private fun getDayOfYear(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_YEAR)
    }

    // ---------- 计算 ----------

    /**
     * 简单四则运算，支持中文运算符（加/减/乘/除等）。
     *
     * @param params 需包含 expression
     * @return 计算结果或错误提示（本地化）
     */
    private fun executeCalculate(params: Map<String, String>): CommandResult {
        val expression = params["expression"] ?: return CommandResult.Error(str(R.string.query_calc_say_expression))

        return try {
            val result = parseAndCalculate(expression)
            if (result != null) {
                val formattedResult = if (result == result.toLong().toDouble()) {
                    result.toLong().toString()
                } else {
                    String.format(Locale.getDefault(), "%.2f", result)
                }
                CommandResult.Success(str(R.string.query_calc_result, formattedResult))
            } else {
                CommandResult.Error(str(R.string.query_calc_unclear))
            }
        } catch (e: Exception) {
            CommandResult.Error(str(R.string.query_calc_failed, e.message ?: ""))
        }
    }

    /**
     * 解析并计算表达式
     * 支持中文和符号表达
     */
    private fun parseAndCalculate(expression: String): Double? {
        // 标准化表达式
        val normalized = expression
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
        val pattern = """(-?\d+\.?\d*)([+\-*/])(-?\d+\.?\d*)""".toRegex()
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
        return parseChineseNumbers()
    }

    /**
     * 解析包含中文数字的表达式
     */
    private fun parseChineseNumbers(): Double? {
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

