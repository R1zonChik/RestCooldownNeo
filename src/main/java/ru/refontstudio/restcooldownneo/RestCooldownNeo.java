package ru.refontstudio.restcooldownneo;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import ru.refontstudio.restcooldownneo.bukkit.Metrics;
import ru.refontstudio.restcooldownneo.commands.CancelCommand;
import ru.refontstudio.restcooldownneo.logic.CooldownLogic;
import ru.refontstudio.restcooldownneo.logic.CooldownSkipper;
import ru.refontstudio.restcooldownneo.logic.DamageListener;
import ru.refontstudio.restcooldownneo.logic.PvPCommandBlocker;
import ru.refontstudio.restcooldownneo.utils.ColorUtils;

public final class RestCooldownNeo extends JavaPlugin {
    private static RestCooldownNeo instance;
    private CooldownLogic cooldownLogic;
    private Plugin duelPlugin; // Ссылка на дуэльный плагин

    @Override
    public void onEnable() {
        // Plugin startup logic
        int pluginId = 22647;
        new Metrics(this, pluginId);
        this.saveDefaultConfig();
        instance = this;
        this.createConfig();
        this.translateConfigColors(this.getConfig());

        // Добавляем новую секцию в конфиг, если её нет
        if (!this.getConfig().contains("pvp-blocked-commands")) {
            List<String> defaultBlockedCommands = Arrays.asList(
                    "spawn", "home", "homes", "tpa", "tpaccept", "tpahere",
                    "warp", "warps", "rtp", "wild", "wilderness", "back"
            );
            this.getConfig().set("pvp-blocked-commands", defaultBlockedCommands);
            this.getConfig().set("allow-op-commands-in-pvp", false);
            this.getConfig().set("messages.pvp-command-blocked", "&c• &fВы не можете использовать эту команду во время PVP");
            this.saveConfig();
        }

        // В методе onEnable класса RestCooldownNeo
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI обнаружен, активирую блокировку команд в PvP...");
            new PvPCommandBlocker(this);
            new CooldownSkipper(this);
        } else {
            getLogger().warning("PlaceholderAPI не найден! Блокировка команд в PvP не будет работать.");
        }

        // Получаем экземпляр дуэльного плагина
        if (getServer().getPluginManager().getPlugin("RestDuels") != null) {
            duelPlugin = getServer().getPluginManager().getPlugin("RestDuels");
            getLogger().info("Успешно подключен к RestDuels!");
        } else {
            getLogger().warning("RestDuels не найден! Проверки на дуэли не будут работать.");
        }

        this.cooldownLogic = new CooldownLogic(this);
        this.getServer().getPluginManager().registerEvents(this.cooldownLogic, this);
        this.getServer().getPluginManager().registerEvents(new DamageListener(this.cooldownLogic, this), this);

        CancelCommand cancelCommand = new CancelCommand(this, this.cooldownLogic);
        if (this.getCommand("neocommand_cancel") != null) {
            this.getCommand("neocommand_cancel").setExecutor(cancelCommand);
        }

        if (this.getCommand("neocommands") != null) {
            this.getCommand("neocommands").setExecutor(cancelCommand);
            this.getCommand("neocommands").setTabCompleter(cancelCommand);
        }

        // Регистрация блокировки команд в PvP при использовании PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI обнаружен, активирую блокировку команд в PvP...");
            new PvPCommandBlocker(this);
        } else {
            getLogger().warning("PlaceholderAPI не найден! Блокировка команд в PvP не будет работать.");
        }

        getLogger().info("RestCooldownNeo успешно запущен!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("RestCooldownNeo выключен!");
    }

    public static RestCooldownNeo getInstance() {
        return instance;
    }

    public void reloadPlugin() {
        this.reloadConfig();
        this.translateConfigColors(this.getConfig());
        getLogger().info("Конфигурация перезагружена!");
    }

    public CooldownLogic getCooldownLogic() {
        return this.cooldownLogic;
    }

    /**
     * Проверяет, находится ли игрок в дуэли
     * @param uuid UUID игрока
     * @return true, если игрок в дуэли
     */
    public boolean isPlayerInDuel(UUID uuid) {
        // Проверяем наличие плагина RestDuels
        if (duelPlugin != null) {
            try {
                // Используем рефлексию для вызова методов
                Object duelManager = duelPlugin.getClass().getMethod("getDuelManager").invoke(duelPlugin);

                // Проверяем, находится ли игрок в дуэли
                Boolean inDuel = (Boolean) duelManager.getClass()
                        .getMethod("isPlayerInDuel", UUID.class)
                        .invoke(duelManager, uuid);

                // Проверяем, заморожен ли игрок
                Boolean isFrozen = (Boolean) duelManager.getClass()
                        .getMethod("isPlayerFrozen", UUID.class)
                        .invoke(duelManager, uuid);

                return inDuel || isFrozen;
            } catch (Exception e) {
                getLogger().warning("Ошибка при проверке статуса дуэли: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    private void createConfig() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            this.getConfig().options().copyDefaults(true);
            this.saveDefaultConfig();
        }
    }

    public void translateConfigColors(FileConfiguration config) {
        Iterator var2 = config.getKeys(true).iterator();

        while(var2.hasNext()) {
            String key = (String)var2.next();
            if (config.isString(key)) {
                String message = config.getString(key);
                if (message != null) {
                    config.set(key, ColorUtils.translateHexColorCodes(message));
                }
            }
        }
    }
}