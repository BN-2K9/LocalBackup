package com.bn_2k9.localBackup.Core;

import com.bn_2k9.localBackup.LocalBackup;
import com.bn_2k9.localBackup.Utils.Logger;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Backup {

    public void InitBackupTimer() {
        // This Checks Every Half Hour if An Backup Is Needed.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(LocalBackup.getInstance(), () -> {

            LocalTime currentTime = LocalTime.now();

            Bukkit.getLogger().info(LocalBackup.getInstance().getConfig().getString("DailyBackupTime"));
            Bukkit.getLogger().info(currentTime.getHour() + ":" + currentTime.getMinute());

            String time = currentTime.getHour() + ":" + currentTime.getMinute();
            if (time.equals(LocalBackup.getInstance().getConfig().getString("DailyBackupTime"))) {
                SaveBackup();
            }


        }, 20 * 16,20 * 50);

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
            if (names.length >= LocalBackup.getInstance().getConfig().getInt("MaxBackups") - 1) {
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


                FileToDelete = FileToDelete.replace(":", "x").replace(".", "z");
                Bukkit.getLogger().info(FileToDelete);

                try {
                    FileUtils.touch(new File((LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + FileToDelete)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                File fileToDelete = FileUtils.getFile((LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups/" + FileToDelete));
                boolean success = FileUtils.deleteQuietly(fileToDelete);

            }

        }

        String FolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath()
                .replace("LocalBackup", "")
                .replace("plugins", "")
                .replace(File.separator + File.separator, File.separator);

        File Folder = new File(FolderPath);

        String BackupZipPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath()
                + "/Backups/"
                + LocalDateTime.now().toString().replace(":", "x").replace(".", "z")
                + ".zip";

        File ZipFile = new File(BackupZipPath);

        Bukkit.getScheduler().runTaskAsynchronously(LocalBackup.getInstance(), () -> {

            try {
                ZipFile.getParentFile().mkdirs();

                Path basePath = Folder.toPath();

                List<String> blackListedNames = new ArrayList<>(
                        LocalBackup.getInstance().getConfig().getStringList("BlackListedNames")
                );

                blackListedNames.add("LocalBackup");
                blackListedNames.add("session.lock");

                byte[] buffer = new byte[65536];

                try (ZipOutputStream zos = new ZipOutputStream(
                        new BufferedOutputStream(new FileOutputStream(ZipFile))
                )) {

                    zos.setLevel(Deflater.BEST_COMPRESSION);

                    Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                            Path relative = basePath.relativize(file);
                            String path = relative.toString().replace("\\", "/");

                            for (String name : blackListedNames) {
                                if (path.contains(name)) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            ZipEntry entry = new ZipEntry(path);
                            zos.putNextEntry(entry);

                            try (InputStream is = Files.newInputStream(file)) {
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    zos.write(buffer, 0, len);
                                }
                            }

                            zos.closeEntry();

                            return FileVisitResult.CONTINUE;
                        }

                    });

                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Logger.LogInfo("&aBackup completed! Duration: " + (System.nanoTime() - start));

            Bukkit.getServer().spigot().restart();
        });
    }

    public static Backup getInstance() {
        return LocalBackup.getInstance().getBackup();
    }


}
