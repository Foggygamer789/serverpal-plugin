package com.serverpal;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ServerPalPlugin extends JavaPlugin {

    private String backendUrl = "https://serverpal-backend.onrender.com/api/push";
    private String serverId = "my-server-1";
    private String key = "test-key";

    @Override
    public void onEnable() {
        // Create default config if it doesn't exist
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            try {
                String defaultConfig = 
                    "backend-url: \"" + backendUrl + "\"\n" +
                    "server-id: \"" + serverId + "\"\n" +
                    "key: \"" + key + "\"\n";
                Files.write(configFile.toPath(), defaultConfig.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                getLogger().warning("Could not create default config.yml");
            }
        }

        // Load values from file
        try {
            for (String line : Files.readAllLines(configFile.toPath())) {
                line = line.trim();
                if (line.startsWith("backend-url:")) {
                    backendUrl = line.substring("backend-url:".length()).trim().replaceAll("\"", "");
                } else if (line.startsWith("server-id:")) {
                    serverId = line.substring("server-id:".length()).trim().replaceAll("\"", "");
                } else if (line.startsWith("key:")) {
                    key = line.substring("key:".length()).trim().replaceAll("\"", "");
                }
            }
        } catch (Exception e) {
            getLogger().warning("Could not read config.yml, using defaults");
        }

        getLogger().info("ServerPal connector enabled. Pushing to " + backendUrl);

        new BukkitRunnable() {
            @Override
            public void run() {
                pushMetrics();
            }
        }.runTaskTimer(this, 100L, 100L); // every 5 seconds
    }

    private void pushMetrics() {
        try {
            double tps = Bukkit.getServer().getTPS()[0];
            int players = Bukkit.getOnlinePlayers().size();
            long totalMem = Runtime.getRuntime().totalMemory();
            long freeMem = Runtime.getRuntime().freeMemory();
            int memPercent = (int)(((totalMem - freeMem) * 100L) / totalMem);
            String json = String.format(
                "{\"serverId\":\"%s\",\"key\":\"%s\",\"tps\":%.1f,\"players\":%d,\"memory\":%d}",
                serverId, key, tps, players, memPercent
            );

            URL url = new URL(backendUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            // silently fail – don't spam console
        }
    }
}
