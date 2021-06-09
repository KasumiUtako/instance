package org.kslab

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info


object BiliBiliDynamic : KotlinPlugin(
    JvmPluginDescription(
        id = "org.kslab.dynamic",
        version = "1.0-SNAPSHOT",
    )
) {

    val historyDynamic: MutableList<String> = mutableListOf()

    lateinit var bot: Bot

    override fun onEnable() {
        BiliBiliDynamicConfig.reload()
        BiliBiliDynamicData.reload()

        BiliBiliDynamic.launch {

            while (true) {
                try {
                    bot = Bot.instances[0]
                    bot
                    break
                } catch (e: Exception) {
                    delay(1000)
                }
            }
            delay(1000)
            logger.info("工作Bot：" + bot.id)

            check(bot)

            logger.info { "bilibili dynamic listen" }
        }
    }
}


object BiliBiliDynamicConfig : AutoSavePluginConfig("BiliBiliDynamic") {
    val admin: Long by value()
    val exception: Boolean by value(true)

    //---------------直播检测----------------//
    var live: MutableMap<String, String> by value(
        mutableMapOf(
            //直播检测总开关
            "enable" to "true",
        )
    )

    //---------------动态检测----------------//
    var dynamic: MutableMap<String, String> by value(
        mutableMapOf(
            //动态检测总开关
            "enable" to "true",
            //访问间隔 单位:秒  范围:[1,∞]
            //这个间隔是每次访问b站api时就会触发
            "interval" to "10",
            //慢速模式开启时间段 不开启则填000-000
            //例：200..800就是凌晨2点到8点
            "lowSpeed" to "200-800",
            //视频模式  此模式仅会推送视频
            "videoMode" to "false",
            //是否保存动态图片
            "saveDynamicImage" to "true"
        )
    )

    var BPI: Map<String, String> by value(
        mapOf(
            // 动态API
            "dynamic" to "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/space_history?visitor_uid=1111111111&offset_dynamic_id=0&need_top=0&host_uid=",
            // 粉丝数API
            "followNum" to "https://api.bilibili.com/x/relation/stat?vmid=",
            // 直播状态API
            "liveStatus" to "https://api.live.bilibili.com/xlive/web-room/v1/index/getInfoByRoom?room_id=",
            // 直播id API
            "liveRoom" to "https://api.live.bilibili.com/room/v1/Room/getRoomInfoOld?mid=",
            // 大航海数 需要参数 用户id:ruid 直播间id:roomid  eg: ruid=487550002&roomid=21811136
            "guard" to "https://api.live.bilibili.com/xlive/app-room/v2/guardTab/topList?page=1&page_size=1&"
        )
    )
}


@Serializable
data class User(
    // 名用户名
    var name: String = "",
    // 用户ID
    var uid: String = "",
    // 动态ID
    var dynamicId: String = "",
    // 直播间号
    var liveRoom: String = "",
    // 直播间状态
    var liveStatus: Int = 0
)

object BiliBiliDynamicData : AutoSavePluginData("BiliBiliDynamicRuntime") {
    val userData: MutableList<User> by value()

    var groupList: MutableMap<Long, MutableList<String>> by value()
    var friendList: MutableMap<Long, MutableList<String>> by value()

    var followMemberGroup: MutableMap<String, MutableList<Long>> by value()
}
