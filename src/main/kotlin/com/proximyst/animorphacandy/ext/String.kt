package com.proximyst.animorphacandy.ext

import org.bukkit.ChatColor

fun String.colour(char: Char = '&') = ChatColor.translateAlternateColorCodes(char, this)!!