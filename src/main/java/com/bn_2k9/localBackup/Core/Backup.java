package com.bn_2k9.localBackup.Core;

import com.bn_2k9.localBackup.LocalBackup;
import com.bn_2k9.localBackup.Utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Backup {

    public void InitBackupTimer() {
        // This Checks Every Half Hour if An Backup Is Needed.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(LocalBackup.getInstance(), () -> {

            LocalTime currentTime = LocalTime.now();

            String time = currentTime.getHour() + ":" + currentTime.getMinute();
            if (time.equals(LocalBackup.getInstance().getConfig().getString("DailyBackupTime"))) {
                SaveBackup();
            }


        }, 20 * 16,20 * 50);

    }

    public void SaveBackup() {

        // Make sure all the player data is saved.
        Bukkit.getServer().savePlayers();
        Logger.LogInfo("Saved Players!");

        // Kick everyone.
        for (Player player: Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Restarting For Backup!");
        }

        Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();

        for (int i = plugins.length - 1; i >= 0; i--) {
            Plugin plugin = plugins[i];

            if (plugin != LocalBackup.getInstance()) {
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                try {
                    ((URLClassLoader) plugin.getClass().getClassLoader()).close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Unload All Worlds To Prevent Corruption.
        for (World world :Bukkit.getServer().getWorlds()) {
            world.save();
            Bukkit.getServer().unloadWorld(world, true);
            Logger.LogInfo("Saved: " + world.getName());
        }

        long start = System.nanoTime();
        String BackupFolder = LocalBackup.getInstance().getDataFolder().getAbsolutePath() + "/Backups";

        if (!new File(BackupFolder).exists()) {
            boolean succes = new File(BackupFolder).mkdirs();
        } else {
            File[] files = new File(BackupFolder).listFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                if (files.length >= LocalBackup.getInstance().getConfig().getInt("MaxBackups")) {
                    files[0].delete();
                }
            }

        }

        String FolderPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath()
                .replace("LocalBackup", "")
                .replace("plugins", "")
                .replace(File.separator + File.separator, File.separator);

        File Folder = new File(FolderPath);

        List<String> blackListedNames = new ArrayList<>(
                LocalBackup.getInstance().getConfig().getStringList("BlackListedNames")
        );

        blackListedNames.add("LocalBackup");
        blackListedNames.add("session.lock");

        String BackupZipPath = LocalBackup.getInstance().getDataFolder().getAbsolutePath()
                + "/Backups/"
                + LocalDateTime.now().toString().replace(":", "x").replace(".", "q")
                + ".zip";

        File ZipFile = new File(BackupZipPath);

        AtomicLong processedFiles = new AtomicLong(0);
        AtomicLong oldPercentage = new AtomicLong(0);
        AtomicLong totalFiles = new AtomicLong(0);

        BukkitTask progressTask = Bukkit.getScheduler().runTaskTimerAsynchronously(LocalBackup.getInstance(), () -> {

            double percent = totalFiles.get() == 0 ? 100 : (processedFiles.get() * 100.0) / totalFiles.get();

            long currentPercent = (long) percent;

            if (oldPercentage.get() != currentPercent && totalFiles.get() != 0) {
                Logger.LogInfo("&eProgress: " + String.format("%.2f%% (%d/%d)", percent, processedFiles.get(), totalFiles.get()));
                oldPercentage.set(currentPercent);
            }

        }, 20, 20);

        Bukkit.getScheduler().runTaskAsynchronously(LocalBackup.getInstance(), () -> {

            try (Stream<Path> stream = Files.walk(Folder.toPath())) {
                totalFiles.set(stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String p = Folder.toPath().relativize(path).toString().replace("\\", "/");
                            for (String name : blackListedNames) {
                                if (p.contains(name)) return false;
                            }
                            return true;
                        })
                        .count());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                ZipFile.getParentFile().mkdirs();

                Path basePath = Folder.toPath();

                try (ZipOutputStream zos = new ZipOutputStream(
                        new BufferedOutputStream(new FileOutputStream(ZipFile))
                )) {

                    zos.setLevel(Deflater.BEST_COMPRESSION);

                    Files.walkFileTree(basePath, new SimpleFileVisitor<>() {

                        @Override
                        public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {

                            Path relative = basePath.relativize(file);
                            String path = relative.toString().replace("\\", "/");

                            for (String name : blackListedNames) {
                                if (path.endsWith(name) || path.contains("/" + name)) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            ZipEntry entry = new ZipEntry(path);
                            zos.putNextEntry(entry);

                            try (InputStream inputStream = Files.newInputStream(file)) {
                                inputStream.transferTo(zos);
                            }

                            zos.closeEntry();
                            processedFiles.incrementAndGet();

                            return FileVisitResult.CONTINUE;
                        }

                    });

                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            progressTask.cancel();
            Logger.LogInfo("&aBackup completed! Duration: " + (System.nanoTime() - start));
            Bukkit.getScheduler().runTask(LocalBackup.getInstance(), () -> Bukkit.getServer().spigot().restart());

        });

    }

    public static Backup getInstance() {
        return LocalBackup.getInstance().getBackup();
    }


}
