package com.max.voiceassistant.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用设置管理。
 *
 * 使用 SharedPreferences 持久化；当前仅支持「是否使用模拟模式」。
 */
class AppSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "voice_assistant_settings"
        private const val KEY_USE_MOCK_MODE = "use_mock_mode"
        private const val DEFAULT_MOCK_MODE = true
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 是否使用模拟模式（未配置百度 SDK 或手动选择 Mock 时为 true）。 */
    var useMockMode: Boolean
        get() = prefs.getBoolean(KEY_USE_MOCK_MODE, DEFAULT_MOCK_MODE)
        set(value) = prefs.edit().putBoolean(KEY_USE_MOCK_MODE, value).apply()
}

