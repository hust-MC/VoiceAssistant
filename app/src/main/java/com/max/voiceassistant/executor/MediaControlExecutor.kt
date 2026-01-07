package com.max.voiceassistant.executor

import android.content.Context
import android.media.AudioManager
import com.max.voiceassistant.model.Command
import com.max.voiceassistant.model.CommandResult
import com.max.voiceassistant.model.CommandType

/**
 * 媒体控制执行器
 * 控制音乐播放和音量
 */
class MediaControlExecutor(private val context: Context) {
    
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    // 静音前的音量（用于恢复）
    private var volumeBeforeMute: Int = 0
    private var isMuted: Boolean = false
    
    fun execute(command: Command): CommandResult {
        return when (command.type) {
            CommandType.MEDIA_PLAY -> executePlay()
            CommandType.MEDIA_PAUSE -> executePause()
            CommandType.MEDIA_STOP -> executeStop()
            CommandType.MEDIA_NEXT -> executeNext()
            CommandType.MEDIA_PREVIOUS -> executePrevious()
            CommandType.VOLUME_UP -> executeVolumeUp()
            CommandType.VOLUME_DOWN -> executeVolumeDown()
            CommandType.VOLUME_MUTE -> executeMute()
            CommandType.VOLUME_UNMUTE -> executeUnmute()
            else -> CommandResult.Error("不支持的媒体命令")
        }
    }
    
    /**
     * 播放音乐
     * 注意：实际项目中需要使用MediaSession API控制真实的媒体播放器
     * 这里只是模拟实现
     */
    private fun executePlay(): CommandResult {
        // 模拟实现：发送媒体按键事件
        // 实际项目中可以使用MediaController或者与具体播放器App交互
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            CommandResult.Success("正在播放音乐")
        } catch (e: Exception) {
            CommandResult.Error("播放失败：${e.message}")
        }
    }
    
    private fun executePause(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            CommandResult.Success("音乐已暂停")
        } catch (e: Exception) {
            CommandResult.Error("暂停失败：${e.message}")
        }
    }
    
    private fun executeStop(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_STOP)
            CommandResult.Success("音乐已停止")
        } catch (e: Exception) {
            CommandResult.Error("停止失败：${e.message}")
        }
    }
    
    private fun executeNext(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            CommandResult.Success("已切换到下一首")
        } catch (e: Exception) {
            CommandResult.Error("切换失败：${e.message}")
        }
    }
    
    private fun executePrevious(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            CommandResult.Success("已切换到上一首")
        } catch (e: Exception) {
            CommandResult.Error("切换失败：${e.message}")
        }
    }
    
    private fun executeVolumeUp(): CommandResult {
        return try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            if (currentVolume >= maxVolume) {
                return CommandResult.Success("音量已经是最大了")
            }
            
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            
            val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val percent = (newVolume * 100 / maxVolume)
            CommandResult.Success("音量已调高，当前${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("调节音量失败：${e.message}")
        }
    }
    
    private fun executeVolumeDown(): CommandResult {
        return try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            if (currentVolume <= 0) {
                return CommandResult.Success("音量已经是最小了")
            }
            
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            
            val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = (newVolume * 100 / maxVolume)
            CommandResult.Success("音量已调低，当前${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("调节音量失败：${e.message}")
        }
    }
    
    private fun executeMute(): CommandResult {
        return try {
            if (isMuted) {
                return CommandResult.Success("已经是静音状态了")
            }
            
            volumeBeforeMute = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                0,
                AudioManager.FLAG_SHOW_UI
            )
            isMuted = true
            CommandResult.Success("已静音")
        } catch (e: Exception) {
            CommandResult.Error("静音失败：${e.message}")
        }
    }
    
    private fun executeUnmute(): CommandResult {
        return try {
            if (!isMuted) {
                return CommandResult.Success("当前不是静音状态")
            }
            
            // 恢复之前的音量，如果之前是0，则设置为中等音量
            val restoreVolume = if (volumeBeforeMute > 0) {
                volumeBeforeMute
            } else {
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
            }
            
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                restoreVolume,
                AudioManager.FLAG_SHOW_UI
            )
            isMuted = false
            
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = (restoreVolume * 100 / maxVolume)
            CommandResult.Success("已取消静音，当前音量${percent}%")
        } catch (e: Exception) {
            CommandResult.Error("取消静音失败：${e.message}")
        }
    }
    
    /**
     * 发送媒体按键事件
     */
    private fun sendMediaKeyEvent(keyCode: Int) {
        val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}

