package com.bn_2k9.localBackup;

import com.bn_2k9.localBackup.Core.Backup;
import com.bn_2k9.localBackup.Core.Command;
import com.bn_2k9.localBackup.Utils.Logger;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocalBackup extends JavaPlugin {

    @Getter
    private static LocalBackup instance;
    @Getter
    private Backup backup;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        instance = this;

        InitClasses();

        Backup.getInstance().InitBackupTimer();

        Logger.LogInfo("Plugin Started!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public void InitClasses() {
        backup = new Backup();
        getCommand("localBackup").setExecutor(new Command());
    }

    public static void displayHelp(String Path, Player e) {
        for (String s: LocalBackup.getInstance().getConfig().getStringList(Path)) {
            e.sendMessage(ChatColor.translateAlternateColorCodes('&', LocalBackup.getInstance().getConfig().getString("PluginPrefix") + s));
        }
    }
}
