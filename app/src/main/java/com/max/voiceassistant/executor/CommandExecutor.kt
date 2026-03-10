package com.max.voiceassistant.executor

import android.content.Context
import com.max.voiceassistant.R
import com.max.voiceassistant.data.VehicleStateRepository
import com.max.voiceassistant.model.*

/**
 * 命令执行器
 * 负责分发和执行各类命令
 */
class CommandExecutor(
    private val context: Context,
    vehicleStateRepository: VehicleStateRepository
) {
    // 各模块执行器
    private val mediaExecutor = MediaControlExecutor(context)
    private val systemExecutor = SystemControlExecutor(context)
    private val vehicleExecutor = VehicleControlExecutor(context, vehicleStateRepository)
    private val queryExecutor = QueryExecutor(context)

    /**
     * 执行命令
     */
    fun execute(command: Command): CommandResult {
        return when (command.category) {
            CommandCategory.MEDIA -> mediaExecutor.execute(command)
            CommandCategory.SYSTEM -> systemExecutor.execute(command)
            CommandCategory.VEHICLE -> vehicleExecutor.execute(command)
            CommandCategory.QUERY -> queryExecutor.execute(command)
            CommandCategory.UNKNOWN -> CommandResult.Error(context.getString(R.string.cmd_unknown))
        }
    }
}

