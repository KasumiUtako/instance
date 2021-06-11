package org.kslab

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
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


