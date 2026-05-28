package com.bn_2k9.localBackup.Listeners;

import com.bn_2k9.localBackup.Core.Backup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    @EventHandler (priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event){
        if (Backup.backupInProgress) {
            event.getPlayer().kickPlayer("Backup in progress.");
        }
    }

}
