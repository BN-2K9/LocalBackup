package com.bn_2k9.localBackup.Core;

import com.bn_2k9.localBackup.LocalBackup;
import com.bn_2k9.localBackup.Utils.Logger;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;


public class Backup {

    public void InitBackupTimer() {
        // This Checks Every Half Hour if An Backup Is Needed.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(LocalBackup.getInstance(), () -> {

            LocalTime currentTime = LocalTime.now();

            if (currentTime.getHour() == LocalBackup.getInstance().getConfig().getInt("DailyBackupTime")) {
                SaveBackup();
            }


        }, 20 * 6,20 * 3600);

    }

    public void SaveBackup() {

        Bukkit.getServer().savePlayers();
        Logger.LogInfo("Saved Players!");

        for (Player player: Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Restarting For Backup!");
        }

        // This Disables LuckPerms So When We Take An Backup It Doesn't Error Out.
        if (Bukkit.getServer().getPluginManager().getPlugin("luckperms") != null) {
            Bukkit.getServer().getPluginManager().getPlugin("luckperms").onDisable();
        }

        // Unload All Worlds To Prevent Corruption.
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
                Logger.LogInfo("Max Backups Reached Removing Oldest File.");
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


                FileToDelete.replace(":", "x").replace(".", "z");
                File delete = new File(LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + FileToDelete);
                try {
                    FileUtils.deleteDirectory(delete);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        String FolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath().replace("LocalBackup", "").replace("plugins", "").replace(File.separator+File.separator, File.separator);
        File Folder = new File(FolderPath);
        String BackupFolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + LocalDateTime.now().toString().replace(":", "x").replace(".", "z") + "/";
        File OutputFolder = new File(BackupFolderPath);

        // Runs The Backup Async So it Doesn't Time Out The Main Thread.
        Bukkit.getScheduler().runTaskAsynchronously(LocalBackup.getInstance(), () -> {

            OutputFolder.mkdirs();
            try {
                Files.walkFileTree(Paths.get(Folder.getAbsolutePath()), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                        List<String> BlackListedNames = LocalBackup.getInstance().getConfig().getStringList("BlackListedNames");
                        BlackListedNames.add("LocalBackup");
                        BlackListedNames.add("session.lock");

                        String path = file.toFile().getAbsolutePath().replace(Folder.getAbsolutePath(), "");

                        Boolean contains = false;

                        for (String BlackListedName: BlackListedNames) {
                            if (path.contains(BlackListedName)) {
                                contains = true;
                            }
                        }

                        if (contains) {
                            Logger.LogInfo("Skipping Found Blacklisted File");
                        } else {
                            if (Files.exists(file)) {
                                if (!Files.isDirectory(file)) {
                                    File newfile = new File(OutputFolder + File.separator + path);
                                    FileUtils.copyFile(file.toFile(), newfile);
                                } else {
                                    File newfile = new File(OutputFolder + File.separator + path);
                                    FileUtils.copyDirectory(file.toFile(), newfile);
                                }
                            }

                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Logger.LogInfo("&aCopy Completed! Duration: " + (System.nanoTime()-start));

            Bukkit.getServer().spigot().restart();

        });
    }

    public static Backup getInstance() {
        return LocalBackup.getInstance().getBackup();
    }


}
