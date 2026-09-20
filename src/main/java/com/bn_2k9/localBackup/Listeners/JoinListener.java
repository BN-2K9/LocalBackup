package com.bn_2k9.localBackup.Listeners;

import com.bn_2k9.localBackup.Core.Backup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class JoinListener implements Listener {

    @EventHandler (priority = EventPriority.LOWEST)
    public void onJoin(AsyncPlayerPreLoginEvent event){
        if (Backup.backupInProgress) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Backup in progress.");
        }
    }

}
