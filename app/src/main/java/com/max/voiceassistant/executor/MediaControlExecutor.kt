package com.max.voiceassistant.executor

import android.content.Context
import android.media.AudioManager
import com.max.voiceassistant.R
import com.max.voiceassistant.model.Command
import com.max.voiceassistant.model.CommandResult
import com.max.voiceassistant.model.CommandType

/**
 * 媒体控制执行器。
 *
 * 通过系统 [AudioManager] 与媒体按键事件控制播放/暂停/上下曲、音量与静音；
 * 实际播放依赖系统或前台媒体应用。
 */
class MediaControlExecutor(private val context: Context) {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    /** 静音前音量，用于取消静音时恢复 */
    private var volumeBeforeMute: Int = 0
    private var isMuted: Boolean = false

    /**
     * 执行媒体类命令并返回本地化结果。
     *
     * @param command 媒体命令（播放/暂停/音量等）
     * @return 成功或失败文案
     */
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
            else -> CommandResult.Error(context.getString(R.string.media_unsupported))
        }
    }

    /**
     * 发送 MEDIA_PLAY 按键事件，由系统或前台应用响应。
     *
     * @return 播放中或失败文案
     */
    private fun executePlay(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            CommandResult.Success(context.getString(R.string.media_playing))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_play_failed, e.message ?: ""))
        }
    }
    
    private fun executePause(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            CommandResult.Success(context.getString(R.string.media_paused))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_pause_failed, e.message ?: ""))
        }
    }
    
    private fun executeStop(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_STOP)
            CommandResult.Success(context.getString(R.string.media_stopped))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_stop_failed, e.message ?: ""))
        }
    }
    
    private fun executeNext(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            CommandResult.Success(context.getString(R.string.media_next))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_switch_failed, e.message ?: ""))
        }
    }
    
    private fun executePrevious(): CommandResult {
        return try {
            sendMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            CommandResult.Success(context.getString(R.string.media_previous))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_switch_failed, e.message ?: ""))
        }
    }
    
    private fun executeVolumeUp(): CommandResult {
        return try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            if (currentVolume >= maxVolume) {
                return CommandResult.Success(context.getString(R.string.media_volume_max))
            }
            
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            
            val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val percent = (newVolume * 100 / maxVolume)
            CommandResult.Success(context.getString(R.string.media_volume_up, percent))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_volume_failed, e.message ?: ""))
        }
    }
    
    private fun executeVolumeDown(): CommandResult {
        return try {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            if (currentVolume <= 0) {
                return CommandResult.Success(context.getString(R.string.media_volume_min))
            }
            
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            
            val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = (newVolume * 100 / maxVolume)
            CommandResult.Success(context.getString(R.string.media_volume_down, percent))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_volume_failed, e.message ?: ""))
        }
    }
    
    private fun executeMute(): CommandResult {
        return try {
            if (isMuted) {
                return CommandResult.Success(context.getString(R.string.media_already_mute))
            }
            
            volumeBeforeMute = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                0,
                AudioManager.FLAG_SHOW_UI
            )
            isMuted = true
            CommandResult.Success(context.getString(R.string.media_muted))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_mute_failed, e.message ?: ""))
        }
    }
    
    private fun executeUnmute(): CommandResult {
        return try {
            if (!isMuted) {
                return CommandResult.Success(context.getString(R.string.media_not_mute))
            }
            
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
            CommandResult.Success(context.getString(R.string.media_unmuted, percent))
        } catch (e: Exception) {
            CommandResult.Error(context.getString(R.string.media_unmute_failed, e.message ?: ""))
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

