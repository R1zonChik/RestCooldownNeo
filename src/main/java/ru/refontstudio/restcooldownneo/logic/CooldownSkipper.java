package ru.refontstudio.restcooldownneo.logic;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import ru.refontstudio.restcooldownneo.RestCooldownNeo;

public class CooldownSkipper implements Listener {

    private final RestCooldownNeo plugin;

    public CooldownSkipper(RestCooldownNeo plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("CooldownSkipper успешно загружен!");
    }

    /**
     * Проверяет, находится ли игрок в режиме PVP
     * @param player Игрок для проверки
     * @return true, если игрок в режиме PVP
     */
    public static boolean isPlayerInPvP(Player player) {
        // Проверяем наличие PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String pvpStatus = PlaceholderAPI.setPlaceholders(player, "%antirelog_in_pvp%");
            return "true".equalsIgnoreCase(pvpStatus);
        }
        return false;
    }
}