package org.kslab

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.command.getGroupOrNull
import net.mamoe.mirai.console.command.isUser
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import org.kslab.bean.User


object EchoPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "org.kslab.echo",
        version = "1.0-SNAPSHOT",
    )
) {
    override fun onEnable() {
        logger.info { "回音插件启动" }
        EchoListenCommand.register()
        EchoBroadcastCommand.register()
    }

    override fun onDisable() {
        EchoListenCommand.unregister()
        EchoBroadcastCommand.unregister()
    }
}

object EchoBroadcastCommand : SimpleCommand(
    EchoPlugin,
    "open",
    "开启监听发布",
    description = "Open Event Broadcast"
) {
    @Handler
    suspend fun CommandSender.handle(qid: Long) {
        val group = getGroupOrNull()
        if (group !== null) {
            BiliBiliDynamicData.groupList[qid] = mutableListOf()
        } else {
            BiliBiliDynamicData.friendList[qid] = mutableListOf()
        }
        sendMessage("消息会分发到 $qid")
    }
}

object EchoListenCommand : SimpleCommand(
    EchoPlugin,
    "sub",
    "监听动态",
    description = "Listen bilibili up",
) {
    @Handler
    suspend fun CommandSender.handle(uid: String, qid: Long) {
        if (!BiliBiliDynamicData.friendList.contains(qid) && !BiliBiliDynamicData.groupList.contains(qid)) {
            sendMessage("要使用动态推送请先回复 /open qid")
            return
        }
        var name = ""
        val group = getGroupOrNull()
        try {
            BiliBiliDynamicData.userData.forEach { item ->
                if (item.uid == uid) {
                    name = item.name
                    return@forEach
                }
            }
            if (!BiliBiliDynamicData.followMemberGroup[uid]!!.contains(qid)) {
                BiliBiliDynamicData.followMemberGroup[uid]!!.add(qid)
                if (group !== null) {
                    BiliBiliDynamicData.groupList[qid]?.add("${uid}@$name")
                } else {
                    BiliBiliDynamicData.friendList[qid]?.add("${uid}@$name")
                }
                sendMessage("已记录 $uid 会发送到 $qid")
            }
        } catch (e: Exception) {
            sendMessage("添加并初始化信息中，请耐心等待...")
            try {
                val bUP = User()
                initFollowInfo(uid, bUP)
                BiliBiliDynamicData.userData.add(bUP)
                name = bUP.name
                if (isUser()) {
                    BiliBiliDynamicData.followMemberGroup[uid] = mutableListOf(qid)
                    if (group !== null) {
                        BiliBiliDynamicData.groupList[qid]?.add("${uid}@$name")
                    } else {
                        BiliBiliDynamicData.friendList[qid]?.add("${uid}@$name")
                    }
                    sendMessage("已记录 $uid 会发送到 $qid")
                } else {
                    sendMessage("此指令只能在聊天窗口使用")
                }
            } catch (e:Exception){
                sendMessage("${uid} 添加失败! 内部错误 或 uid错误")
            }
        }
    }
}
