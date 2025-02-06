package com.bn_2k9.localBackup.Utils;

import com.bn_2k9.localBackup.LocalBackup;
import org.bukkit.ChatColor;

public class Color {

    public static String colorPrefix(String s) {
        return ChatColor.translateAlternateColorCodes('&', LocalBackup.getInstance().getConfig().getString("PluginPrefix") + s);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}
