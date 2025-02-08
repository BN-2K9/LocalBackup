package com.bn_2k9.localBackup.Core;

import com.bn_2k9.localBackup.LocalBackup;
import com.bn_2k9.localBackup.Utils.Logger;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;

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

        String FolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath().replace("LocalBackup", "").replace("plugins", "").replace(File.separator+File.separator, File.separator);
        Bukkit.getLogger().info(FolderPath);
        File Folder = new File(FolderPath);
        String BackupFolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + LocalDateTime.now().toString().replace(":", "x").replace(".", "z") + "/";
        File OutputFolder = new File(BackupFolderPath);

        Bukkit.getScheduler().runTaskAsynchronously(LocalBackup.getInstance(), () -> {

            OutputFolder.mkdirs();
            try {
                Files.walkFileTree(Paths.get(Folder.getAbsolutePath()), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                        if (!Files.isDirectory(file)) {
                            String path = file.toFile().getAbsolutePath().replace(Folder.getAbsolutePath(), "");
                            Bukkit.getLogger().info(path);

                            if (path.contains("session.lock") || path.contains("LocalBackup")) {
                                Logger.LogInfo("Skipping Found Blacklisted File");
                            } else {
                                File newfile = new File(OutputFolder + File.separator + path);
                                FileUtils.copyFile(file.toFile(), newfile);
                            }
                        } else {
                            String path = file.toFile().getAbsolutePath().replace(Folder.getAbsolutePath(), "");
                            Bukkit.getLogger().info(path);

                            if (path.contains("session.lock") || path.contains("LocalBackup")) {
                                Logger.LogInfo("Skipping Found Blacklisted File");
                            } else {
                                File newfile = new File(OutputFolder + File.separator + path);
                                FileUtils.copyDirectory(file.toFile(), newfile);
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Bukkit.getServer().spigot().restart();

            Logger.LogInfo("&aCopy Completed! Duration: " + (System.nanoTime()-start));

        });
    }

    public static Backup getInstance() {
        return LocalBackup.getInstance().getBackup();
    }


}
