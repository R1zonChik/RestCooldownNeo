package ru.refontstudio.restcooldownneo.logic;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.refontstudio.restcooldownneo.RestCooldownNeo;

import java.util.List;

public class PvPCommandBlocker implements Listener {

    private final RestCooldownNeo plugin;
    private final List<String> blockedCommands;
    private final String blockMessage;

    public PvPCommandBlocker(RestCooldownNeo plugin) {
        this.plugin = plugin;

        // Загружаем список команд для блокировки из конфига
        this.blockedCommands = plugin.getConfig().getStringList("pvp-blocked-commands");

        // Загружаем сообщение блокировки из конфига
        this.blockMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.pvp-command-blocked",
                        "&c• &fВы не можете использовать эту команду во время PVP"));

        // Регистрируем слушатель событий
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("PvP Command Blocker успешно загружен!");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();

        // Пропускаем проверку для операторов, если это настроено в конфиге
        if (player.isOp() && plugin.getConfig().getBoolean("allow-op-commands-in-pvp", false)) {
            return;
        }

        // Проверяем, находится ли игрок в режиме PVP
        if (isPlayerInPvP(player)) {
            String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();

            // Проверяем, является ли команда запрещенной
            for (String blockedCmd : blockedCommands) {
                if (command.equalsIgnoreCase(blockedCmd)) {
                    // Отменяем команду и отправляем сообщение
                    event.setCancelled(true);
                    player.sendMessage(blockMessage);
                    return;
                }
            }
        }
    }

    /**
     * Проверяет, находится ли игрок в режиме PVP
     * @param player Игрок для проверки
     * @return true, если игрок в режиме PVP
     */
    private boolean isPlayerInPvP(Player player) {
        // Проверяем наличие PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String pvpStatus = PlaceholderAPI.setPlaceholders(player, "%antirelog_in_pvp%");
            return "true".equalsIgnoreCase(pvpStatus);
        }
        return false;
    }
}