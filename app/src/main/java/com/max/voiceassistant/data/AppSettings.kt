package com.max.voiceassistant.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用设置管理
 * 使用 SharedPreferences 持久化存储
 */
class AppSettings(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "voice_assistant_settings"
        private const val KEY_USE_MOCK_MODE = "use_mock_mode"
        
        // 默认使用 Mock 模式
        private const val DEFAULT_MOCK_MODE = true
    }
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 是否使用模拟模式
     */
    var useMockMode: Boolean
        get() = prefs.getBoolean(KEY_USE_MOCK_MODE, DEFAULT_MOCK_MODE)
        set(value) = prefs.edit().putBoolean(KEY_USE_MOCK_MODE, value).apply()
}

