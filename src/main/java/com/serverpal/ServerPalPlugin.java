package com.serverpal;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ServerPalPlugin extends JavaPlugin {

    private String backendUrl = "https://serverpal-backend.onrender.com/api/push";
    private String serverId = null;
    private String key = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        backendUrl = getConfig().getString("backend-url", backendUrl);
        serverId = getConfig().getString("server-id", "test-server");
        key = getConfig().getString("key", "test-key");

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
