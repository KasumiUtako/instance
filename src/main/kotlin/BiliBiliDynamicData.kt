package org.kslab

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import org.kslab.bean.User

object BiliBiliDynamicData : AutoSavePluginData("BiliBiliDynamicRuntime") {
    val userData: MutableList<User> by value()

    var groupList: MutableMap<Long, MutableList<String>> by value()
    var friendList: MutableMap<Long, MutableList<String>> by value()

    var followMemberGroup: MutableMap<String, MutableList<Long>> by value()
}