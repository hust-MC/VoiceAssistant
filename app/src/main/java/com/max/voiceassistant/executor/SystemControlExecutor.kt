package com.max.voiceassistant.executor

import android.bluetooth.BluetoothAdapter
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import com.max.voiceassistant.model.Command
import com.max.voiceassistant.model.CommandResult
import com.max.voiceassistant.model.CommandType

/**
 * 系统控制执行器
 * 控制WiFi、蓝牙、亮度等系统设置
 */
class SystemControlExecutor(private val context: Context) {
    
    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    
    private val contentResolver: ContentResolver
        get() = context.contentResolver
    
    fun execute(command: Command): CommandResult {
        return when (command.type) {
            CommandType.BRIGHTNESS_UP -> executeBrightnessUp()
            CommandType.BRIGHTNESS_DOWN -> executeBrightnessDown()
            CommandType.WIFI_ON -> executeWifiOn()
            CommandType.WIFI_OFF -> executeWifiOff()
            CommandType.WIFI_STATUS -> executeWifiStatus()
            CommandType.BLUETOOTH_ON -> executeBluetoothOn()
            CommandType.BLUETOOTH_OFF -> executeBluetoothOff()
            CommandType.BLUETOOTH_STATUS -> executeBluetoothStatus()
            CommandType.OPEN_SETTINGS -> executeOpenSettings()
            else -> CommandResult.Error("不支持的系统命令")
        }
    }
    
    // ========== 亮度控制 ==========
    
    /**
     * 提高亮度
     * 注意：需要WRITE_SETTINGS权限，且需要在设置中授权
     */
    private fun executeBrightnessUp(): CommandResult {
        return try {
            if (!canWriteSettings()) {
                return CommandResult.Error("请先在设置中授权本应用修改系统设置")
            }
            
            val currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            
            if (currentBrightness >= 255) {
                return CommandResult.Success("亮度已经是最高了")
            }
            
            val newBrightness = (currentBrightness + 25).coerceAtMost(255)
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )
            
            val percent = (newBrightness * 100 / 255)
            CommandResult.Success("亮度已调高，当前${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("调节亮度失败：${e.message}")
        }
    }
    
    private fun executeBrightnessDown(): CommandResult {
        return try {
            if (!canWriteSettings()) {
                return CommandResult.Error("请先在设置中授权本应用修改系统设置")
            }
            
            val currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            
            if (currentBrightness <= 10) {
                return CommandResult.Success("亮度已经是最低了")
            }
            
            val newBrightness = (currentBrightness - 25).coerceAtLeast(10)
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )
            
            val percent = (newBrightness * 100 / 255)
            CommandResult.Success("亮度已调低，当前${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("调节亮度失败：${e.message}")
        }
    }
    
    private fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }
    
    // ========== WiFi控制 ==========
    
    /**
     * 打开WiFi
     * 注意：Android Q及以上版本，普通App无法直接控制WiFi开关
     * 需要使用设置面板或者引导用户手动操作
     */
    @Suppress("DEPRECATION")
    private fun executeWifiOn(): CommandResult {
        return try {
            val wifi = wifiManager ?: return CommandResult.Error("无法访问WiFi服务")
            
            if (wifi.isWifiEnabled) {
                return CommandResult.Success("WiFi已经是打开状态")
            }
            
            // Android Q以上需要引导用户到设置
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return CommandResult.Success("请在弹出的面板中打开WiFi")
            }
            
            // Android Q以下可以直接控制
            wifi.isWifiEnabled = true
            CommandResult.Success("WiFi已打开")
        } catch (e: Exception) {
            CommandResult.Error("打开WiFi失败：${e.message}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun executeWifiOff(): CommandResult {
        return try {
            val wifi = wifiManager ?: return CommandResult.Error("无法访问WiFi服务")
            
            if (!wifi.isWifiEnabled) {
                return CommandResult.Success("WiFi已经是关闭状态")
            }
            
            // Android Q以上需要引导用户到设置
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return CommandResult.Success("请在弹出的面板中关闭WiFi")
            }
            
            // Android Q以下可以直接控制
            wifi.isWifiEnabled = false
            CommandResult.Success("WiFi已关闭")
        } catch (e: Exception) {
            CommandResult.Error("关闭WiFi失败：${e.message}")
        }
    }
    
    private fun executeWifiStatus(): CommandResult {
        return try {
            val wifi = wifiManager ?: return CommandResult.Error("无法访问WiFi服务")
            
            if (wifi.isWifiEnabled) {
                val ssid = wifi.connectionInfo?.ssid?.replace("\"", "") ?: "未知网络"
                if (ssid == "<unknown ssid>") {
                    CommandResult.Success("WiFi已打开，但未连接网络")
                } else {
                    CommandResult.Success("WiFi已连接到：$ssid")
                }
            } else {
                CommandResult.Success("WiFi已关闭")
            }
        } catch (e: Exception) {
            CommandResult.Error("获取WiFi状态失败：${e.message}")
        }
    }
    
    // ========== 蓝牙控制 ==========
    
    private fun executeBluetoothOn(): CommandResult {
        return try {
            val bluetooth = bluetoothAdapter ?: return CommandResult.Error("设备不支持蓝牙")
            
            if (bluetooth.isEnabled) {
                return CommandResult.Success("蓝牙已经是打开状态")
            }
            
            // 引导用户打开蓝牙
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult.Success("请在弹出的对话框中确认打开蓝牙")
        } catch (e: Exception) {
            CommandResult.Error("打开蓝牙失败：${e.message}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun executeBluetoothOff(): CommandResult {
        return try {
            val bluetooth = bluetoothAdapter ?: return CommandResult.Error("设备不支持蓝牙")
            
            if (!bluetooth.isEnabled) {
                return CommandResult.Success("蓝牙已经是关闭状态")
            }
            
            // 尝试直接关闭（可能需要BLUETOOTH_ADMIN权限）
            bluetooth.disable()
            CommandResult.Success("蓝牙已关闭")
        } catch (e: Exception) {
            // 如果直接关闭失败，引导用户到设置
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult.Success("请在设置中关闭蓝牙")
        }
    }
    
    private fun executeBluetoothStatus(): CommandResult {
        return try {
            val bluetooth = bluetoothAdapter ?: return CommandResult.Error("设备不支持蓝牙")
            
            if (bluetooth.isEnabled) {
                CommandResult.Success("蓝牙已打开")
            } else {
                CommandResult.Success("蓝牙已关闭")
            }
        } catch (e: Exception) {
            CommandResult.Error("获取蓝牙状态失败：${e.message}")
        }
    }
    
    // ========== 其他 ==========
    
    private fun executeOpenSettings(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult.Success("已打开设置")
        } catch (e: Exception) {
            CommandResult.Error("打开设置失败：${e.message}")
        }
    }
}

