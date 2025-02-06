package com.bn_2k9.localBackup.Utils;

import org.bukkit.Bukkit;

public class Logger {

    public static void LogInfo(String s) {
        Bukkit.getConsoleSender().sendMessage(Color.colorPrefix("&aInfo: "+ s));
    }
}
