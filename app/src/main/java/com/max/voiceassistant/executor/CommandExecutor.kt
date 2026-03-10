package com.max.voiceassistant.executor

import android.content.Context
import com.max.voiceassistant.R
import com.max.voiceassistant.data.VehicleStateRepository
import com.max.voiceassistant.model.*

/**
 * 命令执行器。
 *
 * 按 [Command.category] 将命令分发给媒体、系统、车辆、查询四个子执行器；
 * 未知类别时返回本地化提示文案。
 */
class CommandExecutor(
    private val context: Context,
    vehicleStateRepository: VehicleStateRepository
) {
    private val mediaExecutor = MediaControlExecutor(context)
    private val systemExecutor = SystemControlExecutor(context)
    private val vehicleExecutor = VehicleControlExecutor(context, vehicleStateRepository)
    private val queryExecutor = QueryExecutor(context)

    /**
     * 执行命令并返回结果。
     *
     * @param command 待执行命令（含类型与参数）
     * @return 成功/失败/需权限等结果及文案
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

