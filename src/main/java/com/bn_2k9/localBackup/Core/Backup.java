package com.bn_2k9.localBackup.Core;

import com.bn_2k9.localBackup.LocalBackup;
import com.bn_2k9.localBackup.Utils.Logger;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.FileSystemException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;

import static org.apache.commons.io.FileUtils.copyDirectory;

public class Backup {

    public void InitBackupTimer() {

        Bukkit.getScheduler().scheduleSyncRepeatingTask(LocalBackup.getInstance(), () -> {

            LocalTime currentTime = LocalTime.now();

            if (currentTime.getHour() == LocalBackup.getInstance().getConfig().getInt("DailyBackupTime")) {
                SaveBackup();
            }


        }, 20 * 6,20 * 600);

    }

    public void SaveBackup() {

        Bukkit.getServer().savePlayers();
        Logger.LogInfo("Saved Players!");

        for (Player player: Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Restarting For Backup!");
        }

        if (Bukkit.getServer().getPluginManager().getPlugin("luckperms") != null) {
            Bukkit.getServer().getPluginManager().getPlugin("luckperms").onDisable();
        }

        for (World world :Bukkit.getServer().getWorlds()) {
            world.save();
            Bukkit.getServer().unloadWorld(world, true);
            Logger.LogInfo("Saved: " + world.getName());
        }

        Long start = System.nanoTime();

        String BackupFolder = LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups";

        if (!new File(BackupFolder).exists()) {
            new File(BackupFolder).mkdirs();
        } else {
            String[] names = new File(BackupFolder).list();
            if (names.length >= LocalBackup.getInstance().getConfig().getInt("MaxBackups")) {
                String FileToDelete = null;
                for (String name : names) {
                    LocalDateTime currentDateTime = LocalDateTime.now();
                    ChronoLocalDateTime fileTime = LocalDateTime.parse(name.replace("x", ":").replace("z", "."));
                    if (FileToDelete == null) {
                        FileToDelete = name.replace("x", ":").replace("z", ".");
                    } else {
                        if (currentDateTime.isAfter(fileTime) && LocalDateTime.parse(FileToDelete).isAfter(fileTime)) {
                            FileToDelete = name.replace("x", ":").replace("z", ".");
                        }
                    }
                }

                Bukkit.getLogger().info("Removing Backup");

                FileToDelete.replace(":", "x");
                File delete = new File(LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + FileToDelete);
                try {
                    FileUtils.deleteDirectory(delete);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        String FolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath().replace("LocalBackup", "").replace("plugins", "").trim();
        Bukkit.getLogger().info(FolderPath);
        File Folder = new File(FolderPath);
        String BackupFolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + LocalDateTime.now().toString().replace(":", "x").replace(".", "z") + "/";
        File OutputFolder = new File(BackupFolderPath);
        OutputFolder.mkdirs();

        Bukkit.getScheduler().runTaskAsynchronously(LocalBackup.getInstance(), () -> {

            //try {
            //                FileUtils.copyDirectory(Folder, OutputFolder);
            //            } catch (IOException e) {
            //                throw new RuntimeException(e);
            //            }

            for (String f : Folder.list()) {
                File source = (new File(Folder, f));

                Bukkit.getLogger().info(source.getAbsolutePath());
                Bukkit.getLogger().info(source.getName());

                if (source.isDirectory()) {
                    if (!source.getAbsolutePath().contains("LocalBackup")) {
                        try {
                            copyDirectory(source, OutputFolder);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                } else {
                    if (!source.getAbsolutePath().contains("session.lock")) {
                        try {
                            FileUtils.copyFile(source, OutputFolder);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            }

            Bukkit.getServer().reload();

            Logger.LogInfo("&aCopy Completed! Duration: " + (System.nanoTime()-start));

        });
    }

    public static Backup getInstance() {
        return LocalBackup.getInstance().getBackup();
    }


}
