package com.proximyst.animorphacandy.event;

import com.proximyst.animorphacandy.Animorphacandy;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class ConsumeHandler implements Listener {
    private final Animorphacandy main;

    @EventHandler
    public void playerConsumeItem(PlayerItemConsumeEvent event) {
        BiConsumer<Player, PlayerItemConsumeEvent> consumer = main.getActions().get(event.getItem());
        if (consumer == null) return;
        consumer.accept(event.getPlayer(), event);
    }
}
