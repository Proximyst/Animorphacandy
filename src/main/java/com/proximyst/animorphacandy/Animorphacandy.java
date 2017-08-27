package com.proximyst.animorphacandy;

import com.google.common.collect.Sets;
import com.proximyst.animorphacandy.event.ConsumeHandler;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class Animorphacandy extends JavaPlugin {
    @Getter private volatile Map<ItemStack, BiConsumer<Player, PlayerItemConsumeEvent>> actions = new HashMap<ItemStack, BiConsumer<Player, PlayerItemConsumeEvent>>() {
        @Override
        public BiConsumer<Player, PlayerItemConsumeEvent> get(Object key) {
            if (!(key instanceof ItemStack)) return null;
            BiConsumer<Player, PlayerItemConsumeEvent> get = super.get(key);
            if (get == null) {
                Optional<Entry<ItemStack, BiConsumer<Player, PlayerItemConsumeEvent>>> optional = entrySet().stream().filter(entry -> entry.getKey().isSimilar((ItemStack) key)).findFirst();
                if (optional.isPresent()) get = optional.get().getValue();
            }
            return get;
        }
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final ConfigurationSection actionsSection = getConfig().getConfigurationSection("actions");

        // TODO: Clean up this mess.
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            for (final String key : actionsSection.getKeys(false)) {
                final ConfigurationSection section = actionsSection.getConfigurationSection(key);
                if (section == null) continue;

                final Material type = Material.matchMaterial(key.toUpperCase());
                if (type == null) continue;

                final short damage = (short) Math.max(0, Math.min(Short.MAX_VALUE, Math.min(type.getMaxDurability(), section.getInt("durability", 0)))); // Make sure it's within short's bounds before we cast.

                // ItemMeta
                String name = section.getString("name");
                if (name == null) {
                    name = section.getString("displayName");
                }
                boolean unbreakable = section.getBoolean("unbreakable", false);

                final List<String> potionList = section.getStringList("potions");
                Map<Integer, Set<PotionEffect>> potions = new HashMap<>();

                if (!potionList.isEmpty()) {
                    Set<String> yes = Sets.newHashSet("yes", "y", "1", "ye", "yea", "yeah", "yess", "yy", "ya", "yah", "t", "true", "tru", "tr", "ambient", "a", "amb");
                    for (String potionSerialized : potionList) {
                        // CHANCE IN PERCENTAGE: EFFECT: DURATION TICKS[: AMPLIFIER (0 indexed)[: AMBIENT[: PARTICLES]]]
                        String[] split;

                        {
                            int colon = potionSerialized.indexOf(':');
                            if (colon <= -1) {
                                continue;
                            }
                            split = new String[]{potionSerialized.substring(0, colon), potionSerialized.substring(colon)};
                        }

                        int chance = 0;
                        try {
                            chance = Integer.parseInt(split[0].replace("%", ""));
                        } catch (NumberFormatException ignored) {
                            getLogger().warning("Unknown chance was supplied, \"" + split[0] + "\"! Only supply numbers with an optional %.");
                            continue;
                        }

                        split = split[1].split(":");

                        PotionEffectType potionType = PotionEffectType.getByName(split[0].replace(" ", "")); // If space(s) was supplied, remove it before getting type!
                        if (potionType == null) {
                            getLogger().warning("An effect type supplied was not found, \"" + split[0] + "\"!");
                            continue;
                        }

                        int duration = 20;
                        if (split.length >= 2) {
                            String formatted = split[1].replaceAll("[^0-9]", ""); // Async, slow is okay. Users won't notice, anyways.
                            if (!formatted.isEmpty())
                                try {
                                    duration = Integer.parseInt(formatted);
                                } catch (NumberFormatException ignored) {
                                    getLogger().warning("Unknown duration was supplied, \"" + split[1] + "\"!");
                                    duration = 20;
                                }
                        }

                        int amplifier = 0;
                        if (split.length >= 3) {
                            String formatted = split[2].replaceAll("[^0-9]", ""); // Again, async, so slow is okay. Users won't notice as it's one-time.
                            if (!formatted.isEmpty())
                                try {
                                    amplifier = Integer.parseInt(formatted);
                                } catch (NumberFormatException ignored) {
                                    getLogger().warning("Unknown amplifier was supplied, \"" + split[2] + "\"!");
                                }
                        }

                        boolean ambient = false;
                        if (split.length >= 4) {
                            final String ambientInput = split[3]; // final and own variable due to lambda.
                            ambient = yes.stream().anyMatch(it -> it.equalsIgnoreCase(ambientInput));
                        }

                        boolean particles = true;
                        if (split.length == 5) {
                            final String particlesInput = split[4]; // final and own variable due to lambda.
                            particles = yes.stream().anyMatch(it -> it.equalsIgnoreCase(particlesInput));
                        }

                        Set<PotionEffect> potionSet = potions.getOrDefault(chance, new HashSet<>());
                        potionSet.add(new PotionEffect(potionType, duration, amplifier, ambient, particles));
                        potions.put(chance, potionSet);
                    }
                }
                ItemStack item = new ItemStack(type, 1, damage);
                ItemMeta meta = item.getItemMeta();
                if (name != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                }
                //noinspection PointlessBooleanExpression -- Compiler optimises. It's for readability only.
                meta.setUnbreakable(unbreakable | false);
                item.setItemMeta(meta);
                final ThreadLocalRandom random = ThreadLocalRandom.current();
                actions.put(item, (player, event) -> {
                    int chance = random.nextInt(0, 100);
                    Set<PotionEffect> toAdd = new HashSet<>();
                    for (Map.Entry<Integer, Set<PotionEffect>> entry : potions.entrySet()) {
                        if (entry.getKey() < chance) continue;
                        Set<PotionEffect> effectsToCheck = entry.getValue();
                        if (effectsToCheck.size() <= 1) {
                            toAdd.addAll(effectsToCheck);
                            continue;
                        }
                        // apply new chance to each effect.
                        for (PotionEffect effect : effectsToCheck) {
                            int ownchance = random.nextInt(0, 100);
                            if (ownchance < entry.getKey()) continue;
                            toAdd.add(effect);
                        }
                    }
                    player.addPotionEffects(toAdd);
                });
            }
        });
        getServer().getPluginManager().registerEvents(new ConsumeHandler(this), this);
    }
}
