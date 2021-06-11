package org.kslab.console

import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import org.kslab.BiliBiliDynamic
import org.kslab.EchoPlugin

@OptIn(ConsoleExperimentalApi::class)
suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()

    BiliBiliDynamic.load()
    BiliBiliDynamic.enable()
    EchoPlugin.load()
    EchoPlugin.enable()

    MiraiConsole.job.join()
}