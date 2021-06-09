package org.kslab

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getFriendOrGroup
import net.mamoe.mirai.utils.info
import org.kslab.BiliBiliDynamicConfig.BPI
import top.colter.miraiplugin.utils.httpGet
import java.text.SimpleDateFormat


class Dynamic {
    //动态ID、直播号
    var did = ""

    //动态类型
    var type = 0

    //动态时间戳
    var timestamp: Long = 0

    //未解析的动态内容
    var contentJson: JSONObject = JSONObject()

    //解析后的动态内容
    var content = ""

    //是否为动态
    var isDynamic = true

    //图片集合
    var pictures: MutableList<String>? = null

    //b站表情
    var emoji: MutableMap<String, java.awt.Image>? = null

    //b站表情等
    var display: JSONObject = JSONObject()

    //动态信息 图片左下角的信息
    var info = ""

    //动态链接 发送时跟在后面
    var link = ""
}

@OptIn(ConsoleExperimentalApi::class)
suspend fun check(bot: Bot) {
    while (true) {
        try {
            BiliBiliDynamic.logger.info { "开始检测..." }

            val timestamp = System.currentTimeMillis()
            val time = SimpleDateFormat("HHmm").format(timestamp)

            val interval = BiliBiliDynamicConfig.dynamic["interval"]!!.toLong()
            val shortDelay = 1000L..3000L
            val middleDelay = interval * 1000..(interval + 5) * 1000
            val longDelay = (interval + 10) * 1000..(interval + 15) * 1000
            var delay = middleDelay

            val s = BiliBiliDynamicConfig.dynamic["lowSpeed"]!!.split("-")
            if (s[0] != s[1]) {
                if (time.toInt() in s[0].toLong()..s[1].toLong()) {
                    delay = longDelay
                }
            }

            BiliBiliDynamicData.userData.forEach { user ->
                //获取动态
                delay(delay.random())
                val rawDynamicList = httpGet(BPI["dynamic"] + user.uid).getJSONObject("data").getJSONArray("cards")

                //动态检测
                if (BiliBiliDynamicConfig.dynamic["enable"] == "true") {
//                    var r = false
                    // 判断是否为最新动态
                    for (i in rawDynamicList.size downTo 1) {
                        val rawDynamic = rawDynamicList[i - 1] as JSONObject
                        val dynamicId = rawDynamic.getJSONObject("desc").getBigInteger("dynamic_id").toString()
                        if (!BiliBiliDynamic.historyDynamic.contains(dynamicId)) {
                            user.dynamicId = dynamicId
                            BiliBiliDynamic.historyDynamic.add(dynamicId)
                            sendDynamic(bot, rawDynamic, user)
                        }
//                        if (dynamicId==user.dynamicId){
//                            r = true
//                        }
                    }
                }

                //直播检测
                if (user.liveRoom != "0" && BiliBiliDynamicConfig.live["enable"] == "true") {

                    var roomInfo = JSONObject()
                    var liveStatus = 0

                    delay(shortDelay.random())
                    try {
                        if (user.liveRoom != "0") {
                            roomInfo = httpGet(BPI["liveStatus"] + user.liveRoom).getJSONObject("data")
                                .getJSONObject("room_info")
                            liveStatus = roomInfo.getInteger("live_status")
                        }
                    } catch (e: Exception) {
                        BiliBiliDynamic.logger.error("检测动态失败")
                    }

                    if (liveStatus == 1 && (user.liveStatus == 0 || user.liveStatus == 2)) {
                        val dynamic = Dynamic()
                        dynamic.did = user.liveRoom
                        dynamic.timestamp = roomInfo.getBigInteger("live_start_time").toLong()
                        dynamic.content = "直播: ${roomInfo.getString("title")}"
                        dynamic.isDynamic = false
                        dynamic.pictures = mutableListOf()
                        dynamic.info = "直播ID:" + user.liveRoom
                        dynamic.link = "https://live.bilibili.com/" + user.liveRoom

                        val cover = roomInfo.getString("cover")
                        val keyframe = roomInfo.getString("keyframe")
                        if (cover != "") {
                            dynamic.pictures?.add(cover)
                        } else if (keyframe != "") {
                            dynamic.pictures?.add(keyframe)
                        }
                        sendMessage(bot, user.uid, buildResMessage(dynamic, user))
                    }
                    user.liveStatus = liveStatus
                }
            }

            BiliBiliDynamic.logger.info { "检测结束" }
            delay(20000L)

        } catch (e: Exception) {
            if (BiliBiliDynamicConfig.exception) {
                try {
                    bot.getFriendOrGroup(BiliBiliDynamicConfig.admin).sendMessage("检测动态失败，2分钟后重试\n" + e.message)
                } catch (e: Exception) {
                    BiliBiliDynamic.logger.error("发送失败")
                }
            }
            delay(120000L)
        }
    }
}


suspend fun initFollowInfo(uid: String, user: User): User {
    val rawDynamic = httpGet(BPI["dynamic"] + uid).getJSONObject("data").getJSONArray("cards")
    rawDynamic.forEach { item ->
        val desc = JSON.parseObject(item.toString()).getJSONObject("desc")
        BiliBiliDynamic.historyDynamic.add(desc.getBigInteger("dynamic_id").toString())
    }
    val res = rawDynamic.getJSONObject(0)
    val userProfile = res.getJSONObject("desc").getJSONObject("user_profile")
    val name = userProfile.getJSONObject("info").getString("uname")

    user.uid = uid
    user.name = name
    user.dynamicId = res.getJSONObject("desc").getBigInteger("dynamic_id").toString()

//    val face = userProfile.getJSONObject("info").getString("face")
//    val pendant = userProfile.getJSONObject("pendant").getString("image")

    delay(500)
    val liveRoom = httpGet(BPI["liveRoom"] + uid, "aaa").getJSONObject("data").getBigInteger("roomid").toString()
    user.liveRoom = liveRoom

    try {
        if (user.liveRoom != "0") {
            delay(500)
            user.liveStatus =
                httpGet(BPI["liveStatus"] + user.liveRoom).getJSONObject("data").getJSONObject("room_info")
                    .getInteger("live_status")
        }
    } catch (e: Exception) {
        user.liveStatus = 0
    }
    return user
}