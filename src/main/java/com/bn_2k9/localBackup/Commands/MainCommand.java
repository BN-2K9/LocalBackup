package com.bn_2k9.localBackup.Commands;

import com.bn_2k9.localBackup.Core.Backup;
import com.bn_2k9.localBackup.LocalBackup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public class MainCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {

        if (!sender.hasPermission("LocalBackup.Admin")) {
            return true;
        }

        if (args.length == 0) {
            LocalBackup.displayHelp("Messages.Help", (Player) sender);
            return true;
        }

        if (args[0].equals("backup")) {
            Backup.getInstance().SaveBackup();
        } else {
            LocalBackup.displayHelp("Messages.Help", (Player) sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete (@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[]args){
        if (args.length == 1) {
            return List.of("backup");
        }
        return null;
    }

}
