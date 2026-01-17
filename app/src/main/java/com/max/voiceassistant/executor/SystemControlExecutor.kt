package com.max.voiceassistant.executor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
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
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }
    
    /**
     * 检查是否有蓝牙连接权限 (Android 12+)
     */
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12以下不需要运行时权限
        }
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
                return requestWriteSettingsPermission()
            }
            
            // 先关闭自动亮度
            disableAutoBrightness()
            
            val currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            
            if (currentBrightness >= 255) {
                return CommandResult.Success("亮度已经是最高了")
            }
            
            val newBrightness = (currentBrightness + 25).coerceAtMost(255)
            setBrightness(newBrightness)
            
            val percent = (newBrightness * 100 / 255)
            CommandResult.Success("亮度已调高，当前${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("调节亮度失败：${e.message}")
        }
    }
    
    private fun executeBrightnessDown(): CommandResult {
        return try {
            if (!canWriteSettings()) {
                return requestWriteSettingsPermission()
            }
            
            // 先关闭自动亮度
            disableAutoBrightness()
            
            val currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            
            if (currentBrightness <= 10) {
                return CommandResult.Success("亮度已经是最低了")
            }
            
            val newBrightness = (currentBrightness - 25).coerceAtLeast(10)
            setBrightness(newBrightness)
            
            val percent = (newBrightness * 100 / 255)
            CommandResult.Success("亮度已调低，当前${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("调节亮度失败：${e.message}")
        }
    }
    
    /**
     * 关闭自动亮度模式
     */
    private fun disableAutoBrightness() {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    /**
     * 设置屏幕亮度
     * @param brightness 亮度值 (0-255)
     */
    private fun setBrightness(brightness: Int) {
        // 1. 保存到系统设置
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness
        )
        
        // 2. 发送广播通知系统亮度已更改（部分设备需要）
        val intent = Intent("android.intent.action.SCREEN_BRIGHTNESS_CHANGED")
        context.sendBroadcast(intent)
    }
    
    private fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }
    
    /**
     * 请求修改系统设置权限
     * 跳转到专门的授权页面
     */
    private fun requestWriteSettingsPermission(): CommandResult {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult.Success("请在弹出的页面中授权，然后再试一次")
        } catch (e: Exception) {
            CommandResult.Error("无法打开授权页面：${e.message}")
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            // Android 12+ 需要检查 BLUETOOTH_CONNECT 权限
            if (!hasBluetoothPermission()) {
                return CommandResult.NeedPermission(
                    "需要蓝牙权限才能控制蓝牙",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Manifest.permission.BLUETOOTH_CONNECT
                    } else {
                        Manifest.permission.BLUETOOTH
                    }
                )
            }
            
            val bluetooth = bluetoothAdapter ?: return CommandResult.Error("设备不支持蓝牙")
            
            if (bluetooth.isEnabled) {
                return CommandResult.Success("蓝牙已经是打开状态")
            }
            
            // 引导用户打开蓝牙
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult.Success("请在弹出的对话框中确认打开蓝牙")
        } catch (e: SecurityException) {
            CommandResult.NeedPermission(
                "需要蓝牙权限",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH
                }
            )
        } catch (e: Exception) {
            CommandResult.Error("打开蓝牙失败：${e.message}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun executeBluetoothOff(): CommandResult {
        return try {
            if (!hasBluetoothPermission()) {
                return CommandResult.NeedPermission(
                    "需要蓝牙权限才能控制蓝牙",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Manifest.permission.BLUETOOTH_CONNECT
                    } else {
                        Manifest.permission.BLUETOOTH
                    }
                )
            }
            
            val bluetooth = bluetoothAdapter ?: return CommandResult.Error("设备不支持蓝牙")
            
            if (!bluetooth.isEnabled) {
                return CommandResult.Success("蓝牙已经是关闭状态")
            }
            
            // Android 13+ 普通App无法直接关闭蓝牙，引导到设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return CommandResult.Success("请在设置中关闭蓝牙")
            }
            
            // 尝试直接关闭（可能需要BLUETOOTH_ADMIN权限）
            bluetooth.disable()
            CommandResult.Success("蓝牙已关闭")
        } catch (e: SecurityException) {
            // 如果直接关闭失败，引导用户到设置
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult.Success("请在设置中关闭蓝牙")
        } catch (e: Exception) {
            CommandResult.Error("关闭蓝牙失败：${e.message}")
        }
    }
    
    private fun executeBluetoothStatus(): CommandResult {
        return try {
            if (!hasBluetoothPermission()) {
                return CommandResult.NeedPermission(
                    "需要蓝牙权限才能查询状态",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Manifest.permission.BLUETOOTH_CONNECT
                    } else {
                        Manifest.permission.BLUETOOTH
                    }
                )
            }
            
            val bluetooth = bluetoothAdapter ?: return CommandResult.Error("设备不支持蓝牙")
            
            if (bluetooth.isEnabled) {
                CommandResult.Success("蓝牙已打开")
            } else {
                CommandResult.Success("蓝牙已关闭")
            }
        } catch (e: SecurityException) {
            CommandResult.NeedPermission(
                "需要蓝牙权限",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Manifest.permission.BLUETOOTH_CONNECT
                } else {
                    Manifest.permission.BLUETOOTH
                }
            )
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

