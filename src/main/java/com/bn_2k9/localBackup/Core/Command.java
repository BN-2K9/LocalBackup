package com.bn_2k9.localBackup.Core;

import com.bn_2k9.localBackup.LocalBackup;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Command implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {

        if (!sender.hasPermission("LocalBackup.Admin")) {
            return true;
        }

        if (args.length == 0) {
            LocalBackup.displayHelp("Messages.Help", (Player) sender);
            return true;
        }

        switch (args[0]) {
            case "backup" -> Backup.getInstance().SaveBackup();
            default -> LocalBackup.displayHelp("Messages.Help", (Player) sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete (CommandSender sender, org.bukkit.command.Command command, String alias, String[]args){
        if (args.length == 1) { //prank <subcommand> <args>
            ArrayList<String> subcommandsArguments = new ArrayList<>();

            subcommandsArguments.add("backup");

            return subcommandsArguments;
        }
        return null;
    }

}
