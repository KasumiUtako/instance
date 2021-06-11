package org.kslab

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getFriendOrGroup
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.kslab.bean.Dynamic
import org.kslab.bean.User
import java.net.URL

/**
 * 解析动态 发送动态
 */
suspend fun sendDynamic(bot: Bot, rawDynamic: JSONObject, user: User) {
    try {
        val desc = rawDynamic.getJSONObject("desc")
        val type = desc.getInteger("type")
        if (BiliBiliDynamicConfig.dynamic["videoMode"] == "true" && type != 8) {
            return
        }

        // 封装动态
        val dynamic = Dynamic()
        dynamic.did = desc.getBigInteger("dynamic_id").toString()
        dynamic.timestamp = desc.getBigInteger("timestamp").toLong()
        dynamic.type = type
        dynamic.contentJson = JSON.parseObject(rawDynamic.getString("card"))

        // 格式化动态信息
        dynamicFormat(dynamic)
        // 构建消息链
        val resMag = buildResMessage(dynamic, user)
        // 发送消息
        sendMessage(bot, user.uid, resMag)

    } catch (e: Exception) {
        BiliBiliDynamic.logger.error("发送 " + user.name + " 的动态失败!\n" + e.message)
        throw Exception("发送 " + user.name + " 的动态失败!\n" + e.message)
    }
}

/**
 * 动态格式化为消息 封装在dynamic中的content
 */
fun dynamicFormat(dynamic: Dynamic) {
    var content = ""
    when (dynamic.type) {
        // 转发动态
        1 -> {
            val card = dynamic.contentJson
            content = "转发动态 : \n" + card.getJSONObject("item").getString("content") + "\n\n"
            val origType = card.getJSONObject("item").getInteger("orig_type")
            val origin = JSON.parseObject(card.getString("origin"))
            val originUser = card.getJSONObject("origin_user").getJSONObject("info").getString("uname")
            when (origType) {
                //直播动态
                1 -> {
                    content += "原动态 $originUser : 直播"
                }
                //带图片的动态
                2 -> {
                    content += "原动态 $originUser : \n"
                    content += origin.getJSONObject("item").getString("description")
                    dynamic.pictures = mutableListOf()
                    for (pic in origin.getJSONObject("item").getJSONArray("pictures")) {
                        dynamic.pictures?.add((pic as JSONObject).getString("img_src"))
                    }
                }
                //带表情的文字动态
                4 -> {
                    content += "原动态 $originUser : \n"
                    content += origin.getJSONObject("item").getString("content")
                }
                //视频动态
                8 -> {
                    content += "来自 $originUser 的视频 : ${origin.getString("title")}"
                    dynamic.pictures = mutableListOf()
                    dynamic.pictures?.add(origin.getString("pic"))
                }
            }
        }
        //带图片的动态
        2 -> {
            val card = dynamic.contentJson
            content = card.getJSONObject("item").getString("description")
            dynamic.pictures = mutableListOf()
            for (pic in card.getJSONObject("item").getJSONArray("pictures")) {
                dynamic.pictures?.add((pic as JSONObject).getString("img_src"))
            }
        }
        //带表情的文字动态
        4 -> {
            val card = dynamic.contentJson
            content = card.getJSONObject("item").getString("content")
        }

        //视频更新动态
        8 -> {
            val card = dynamic.contentJson
            content += "视频: ${card.getString("title")}"
            val dt = card.getString("dynamic")
            val av = card.getString("aid")
            if (dt != "") {
                content += "\n\n" + dt + "\n"
            }
            dynamic.pictures = mutableListOf()
            dynamic.pictures?.add(card.getString("pic"))
            dynamic.info = "视频ID:av${av}"
            dynamic.link = "https://www.bilibili.com/video/av${av}"
        }

        //专栏
        64 -> {
            val card = dynamic.contentJson
            content += "专栏: ${card.getString("title")}\n"
            content += card.getString("summary")
            val cv = card.getString("id")
            val banner = card.getString("banner_url")
            if (banner != "") {
                dynamic.pictures = mutableListOf()
                dynamic.pictures?.add(banner)
            }
            dynamic.info = "专栏ID:${cv}"
            dynamic.link = "https://www.bilibili.com/read/cv${cv}"
        }

        //音频
        256 -> {
            val card = dynamic.contentJson
            val dt = card.getString("intro")
            val au = card.getString("id")
            if (dt != "") {
                content += dt + "\n"
            }
            content += "音频: ${card.getString("title")}"
            dynamic.pictures = mutableListOf()
            dynamic.pictures?.add(card.getString("cover"))
            dynamic.info = "音频ID:${au}"
            dynamic.link = "https://www.bilibili.com/audio/au${au}"
        }

        else -> {
            content = "不支持此类型动态 动态类型: ${dynamic.type}"
            dynamic.info = "动态ID:${dynamic.did}"
            dynamic.link = "https://t.bilibili.com/${dynamic.did}"
        }
    }
    if (dynamic.type == 1 || dynamic.type == 2 || dynamic.type == 4) {
        dynamic.info = "动态ID:${dynamic.did}"
        dynamic.link = "https://t.bilibili.com/${dynamic.did}"
    }

    dynamic.content = content
}

/**
 * 构建发送消息链
 */
suspend fun buildResMessage(dynamic: Dynamic, user: User): MessageChain {
    return buildTextMassageChain(dynamic, user)
}


/**
 * 构建文字消息
 */
@OptIn(ConsoleExperimentalApi::class)
suspend fun buildTextMassageChain(dynamic: Dynamic, user: User): MessageChain {
    var content = dynamic.content
    val sb = StringBuilder()

    if (dynamic.isDynamic) {
        // 删除b站表情
        content = content.replace("[", " [")
        while (content.indexOf('[') != -1) {
            content = content.removeRange(content.indexOf('['), content.indexOf(']') + 1)
        }

        sb.append("=====${user.name} 动态=====")
        sb.append("\n")
        sb.append(content)
        sb.append("\n")
        sb.append(dynamic.link)
        if (dynamic.pictures != null && dynamic.pictures?.size != 0) {
            sb.append("\n")
            sb.append("=======附件=======")
            sb.append("\n")
            return buildMessageChain {
                +sb.toString()
                for (img in dynamic.pictures!!) {
                    if (img.endsWith("webp")) {
                        +"\n [$img] \n"
                    } else {
                        //                    +Image(PluginMain.bot.getFriendOrGroup(PluginConfig.admin).uploadImage(URL(img).openConnection().getInputStream().toExternalResource()).imageId)
                        +Image(
                            URL(img).openConnection().getInputStream().uploadAsImage(
                                BiliBiliDynamic.bot.getFriendOrGroup(
                                    BiliBiliDynamicConfig.admin
                                )
                            ).imageId
                        )
                    }
                }
            }
        }

        return buildMessageChain {
            +sb.toString()
        }
    } else {
        sb.append("${user.name} 开播啦~")
        sb.append("\n")
        sb.append(content)
        sb.append("\n")
        sb.append(dynamic.link)
        return buildMessageChain {
            +sb.toString()
        }
    }
}

/**
 * 发送消息
 */
suspend fun sendMessage(bot: Bot, uid: String, resMsg: MessageChain) {
    BiliBiliDynamicData.followMemberGroup[uid]?.forEach { id ->
        if (BiliBiliDynamicData.groupList.contains(id)) {
            bot.getGroup(id)?.sendMessage(resMsg)
        } else if (BiliBiliDynamicData.friendList.contains(id)) {
            bot.getFriend(id)?.sendMessage(resMsg)
        }
        delay(500)
    }
}

