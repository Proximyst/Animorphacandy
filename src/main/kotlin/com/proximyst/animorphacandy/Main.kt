package com.proximyst.animorphacandy

import com.proximyst.animorphacandy.eventhandle.HandleConsume
import com.proximyst.animorphacandy.ext.colour
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ThreadLocalRandom

class Main : JavaPlugin() {
    val actions = mutableMapOf<ItemStack, (Player, PlayerItemConsumeEvent) -> Unit>()

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        saveDefaultConfig()
        val actionsSection = config.getConfigurationSection("actions")
        // TODO: Fix the mess of code, if requested at least. Should do it anyways for own sanity at night when I think of it..
        outer@ for (key in actionsSection.getKeys(false)) {
            if (!actionsSection.isConfigurationSection(key)) continue
            val section = actionsSection.getConfigurationSection(key)
            val material = Material.matchMaterial(key) ?: continue
            val damage = section.getInt("durability", 0)
            val name = section.getString("name") ?: section.getString("displayName")
            val potionsList = section.getStringList("potions")
            val potions = mutableMapOf<Int, PotionEffect>()
            // Following is a big block. Recommended to collapse it in IntelliJ
            inner@ for (pot in potionsList) {
                // EFFECT: DURATION TICKS[: AMPLIFIER (0 indexed)[: AMBIENT[: PARTICLES]]]
                var chance = 0
                var cont = false
                val pot = run {
                    val splitten = pot.split(": ", limit = 2)
                    try {
                        chance = Integer.parseInt(splitten[0].removeSuffix("%"))
                    } catch (ex: NumberFormatException) {
                        logger.warning("Unknown chance! ${splitten[0].removeSuffix("%")}")
                        cont = true
                    }
                    splitten[1]
                }
                if (cont) continue@inner
                val splitted = pot.split(": ")
                if (splitted.isEmpty()) {
                    continue@outer
                }
                val effect = PotionEffectType.getByName(splitted[0])
                if (effect == null) {
                    logger.info("Effect was not found: ${splitted[0]}")
                    continue@inner
                }
                val durationStr = splitted.getOrNull(1)
                val duration: Int
                if (durationStr is String) {
                    var temp: Int
                    try {
                        temp = Integer.parseInt(durationStr)
                    } catch (ex: NumberFormatException) {
                        logger.warning("Duration was unknown! Defaulting to 20.")
                        temp = 20
                    }
                    duration = temp
                } else if (durationStr == null) {
                    duration = 20
                } else {
                    // Bytecode injection? Shouldn't really ever happen.
                    logger.warning("Unknown data type! $durationStr")
                    continue@inner
                }
                val amplifierStr = splitted.getOrNull(2)
                val amplifier: Int
                if (amplifierStr is String) {
                    var temp: Int
                    try {
                        temp = Integer.parseInt(durationStr)
                    } catch (ex: NumberFormatException) {
                        logger.warning("Amplifier was unknown! Defaulting to 0.")
                        temp = 0
                    }
                    amplifier = temp
                } else if (amplifierStr == null) {
                    amplifier = 0
                } else {
                    // Bytecode injection? Shouldn't really ever happen.
                    logger.warning("Unknown data type! $amplifierStr")
                    continue@inner
                }
                val ambientStr = splitted.getOrNull(3)
                val ambient: Boolean
                if (ambientStr is String) {
                    ambient = arrayOf("true", "yes", "y", "t", "1", "ambient").any { it.equals(ambientStr, true) }
                } else if (ambientStr == null) {
                    ambient = false
                } else {
                    // Bytecode injection? Shouldn't really ever happen.
                    logger.warning("Unknown data type! $ambientStr")
                    continue@inner
                }
                val particlesStr = splitted.getOrNull(3)
                val particles: Boolean
                if (particlesStr is String) {
                    particles = arrayOf("true", "yes", "y", "t", "1", "ambient").any { it.equals(particlesStr, true) }
                } else if (particlesStr == null) {
                    particles = true
                } else {
                    // Bytecode injection? Shouldn't really ever happen.
                    logger.warning("Unknown data type! $particlesStr")
                    continue@inner
                }
                potions[chance] = PotionEffect(effect, duration, amplifier, ambient, particles)
            }
            val item = ItemStack(material, 1, damage.toShort()).apply {
                val meta = itemMeta
                if (name != null) meta.displayName = name.colour()
                itemMeta = meta
            }
            actions.put(item) { player, _ ->
                val random = ThreadLocalRandom.current().nextInt(0, 100)
                player.addPotionEffects(potions.filter { it.key <= random }.map { it.value })
            }
        }
        server.pluginManager.registerEvents(HandleConsume(), this)
    }

    override fun onDisable() {
        actions.clear()
        HandlerList.unregisterAll(this)
    }

    companion object {
        lateinit var instance: Main
            private set
    }
}