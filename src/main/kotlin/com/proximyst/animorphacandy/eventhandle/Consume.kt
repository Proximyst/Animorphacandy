package com.proximyst.animorphacandy.eventhandle

import com.proximyst.animorphacandy.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent

class HandleConsume : Listener {
    @EventHandler
    fun PlayerItemConsumeEvent.consume() {
        Main.instance.actions.filter { it.key.isSimilar(item) }.forEach {
            it.value(player, this)
        }
    }
}