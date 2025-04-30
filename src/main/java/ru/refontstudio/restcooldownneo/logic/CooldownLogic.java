package ru.refontstudio.restcooldownneo.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.refontstudio.restcooldownneo.JsonMessageBuilder;
import ru.refontstudio.restcooldownneo.RestCooldownNeo;
import ru.refontstudio.restcooldownneo.placeholders.NeoPlaceholders;

public class CooldownLogic implements Listener {
    private final RestCooldownNeo plugin;
    private final Map<String, CommandGroup> commandGroups = new HashMap<>();
    private final Map<Player, BossBar> playerBossBars = new HashMap<>();
    private final Map<Player, BukkitRunnable> playerCooldownRunnables = new HashMap<>();
    private final Map<Player, Integer> playerCooldowns = new HashMap<>();
    private final Map<Player, Set<String>> playerCommandCooldowns = new HashMap<>();

    public CooldownLogic(RestCooldownNeo plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection commandGroupsSection = this.plugin.getConfig().getConfigurationSection("command-groups");
        if (commandGroupsSection != null) {
            Iterator var2 = commandGroupsSection.getKeys(false).iterator();

            while(var2.hasNext()) {
                String groupName = (String)var2.next();
                ConfigurationSection groupSection = commandGroupsSection.getConfigurationSection(groupName);
                if (groupSection != null) {
                    CommandGroup commandGroup = new CommandGroup(groupSection, this.plugin, this);
                    this.commandGroups.put(groupName, commandGroup);
                }
            }
        }
    }

    public boolean cancelAllCommands(Player player) {
        // Если игрок НЕ находится в кулдауне (т.е. нет активной телепортации)
        if (!this.isPlayerInCooldown(player)) {
            // Не отправляем сообщение, просто возвращаем false
            return false;
        }

        // Если есть активный runnable, отменяем его
        if (this.playerCooldownRunnables.containsKey(player)) {
            ((BukkitRunnable)this.playerCooldownRunnables.get(player)).cancel();
            this.playerCooldownRunnables.remove(player);
        }

        // Удаляем информацию о кулдауне
        this.playerCooldowns.remove(player);
        NeoPlaceholders.setCooldown(player, 0);

        // Удаляем BossBar если он есть
        if (this.playerBossBars.containsKey(player)) {
            ((BossBar)this.playerBossBars.get(player)).removeAll();
            this.playerBossBars.remove(player);
        }

        // Отправляем сообщение об отмене телепорта (только один раз)
        String message = this.plugin.getConfig().getString("messages.teleport-is-cancel", "&c• &fВы отменили телепорт");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Проверка на PVP с использованием PlaceholderAPI
        if (isPlayerInPvP(player)) {
            // Если игрок в режиме PVP, пропускаем обработку команд телепортации
            return;
        }

        // Проверка на дуэль и разрешение только команды /hub (для не-операторов)
        if (plugin.isPlayerInDuel(player.getUniqueId()) && !player.isOp()) {
            String message = event.getMessage();
            if (message.length() > 1) {
                String cmdLabel = message.substring(1).split(" ")[0]; // Получаем команду без "/"
                if (!cmdLabel.equalsIgnoreCase("hub")) {
                    // Если команда не hub - тихо блокируем
                    event.setCancelled(true);
                    return;
                }
            }
        }

        String fullCommand = event.getMessage().substring(1);
        String[] commandArgs = fullCommand.split(" ");
        String command = commandArgs[0];
        String[] args = (String[])Arrays.copyOfRange(commandArgs, 1, commandArgs.length);

        if (this.playerCommandCooldowns.containsKey(player) && ((Set)this.playerCommandCooldowns.get(player)).contains(command)) {
            event.setCancelled(true);
            int cooldownTime = this.plugin.getConfig().getInt("cooldown-commands", 5);
            String message = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("messages.cooldown-commands")).replace("{cooldown-command}", String.valueOf(cooldownTime));
            player.sendMessage(message);
        } else {
            Iterator var7 = this.commandGroups.values().iterator();

            CommandGroup group;
            do {
                if (!var7.hasNext()) {
                    return;
                }

                group = (CommandGroup)var7.next();
            } while(!group.getCommandsCooldown().contains(command) || !group.handleCommand(player, command, args));

            event.setCancelled(true);
        }
    }

    public void startCooldown(Player player, String action, int cooldownTime, String[] args) {
        if (this.commandGroups.containsKey(action)) {
            CommandGroup group = (CommandGroup)this.commandGroups.get(action);
            group.startCooldown(player, action, cooldownTime, args);
        }
    }

    public boolean canUseCommand(Player player, String command) {
        return player.hasPermission("neocooldown.admin");
    }

    public void addPlayerBossBar(Player player, BossBar bossBar) {
        this.playerBossBars.put(player, bossBar);
    }

    public void removePlayerBossBar(Player player) {
        this.playerBossBars.remove(player);
    }

    public boolean isPlayerInCooldown(Player player) {
        return this.playerCooldowns.containsKey(player) && (Integer)this.playerCooldowns.get(player) > 0;
    }

    /**
     * Проверяет, находится ли игрок в режиме PVP
     * @param player Игрок для проверки
     * @return true, если игрок в режиме PVP
     */
    public boolean isPlayerInPvP(Player player) {
        // Проверяем наличие PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String pvpStatus = PlaceholderAPI.setPlaceholders(player, "%antirelog_in_pvp%");
            return "true".equalsIgnoreCase(pvpStatus);
        }
        return false;
    }

    private static class Cooldown {
        private final String permission;
        private final int cooldown;

        public Cooldown(String permission, int cooldown) {
            this.permission = permission;
            this.cooldown = cooldown;
        }

        public String getPermission() {
            return this.permission;
        }

        public int getCooldown() {
            return this.cooldown;
        }
    }

    private static class CommandGroup {
        private final RestCooldownNeo plugin;
        private final CooldownLogic cooldownLogic;
        private final boolean bossBarEnable;
        private final BarColor bossBarColor;
        private final BarStyle bossBarStyle;
        private final boolean titleEnable;
        private final String titleText;
        private final String subtitleText;
        private final boolean soundEnable;
        private final Sound sound;
        private final boolean effectsEnable;
        private final List<PotionEffect> effects;
        private final Map<Integer, Cooldown> cooldowns;
        private final List<String> commandsCooldown;

        public CommandGroup(ConfigurationSection section, RestCooldownNeo plugin, CooldownLogic cooldownLogic) {
            this.plugin = plugin;
            this.cooldownLogic = cooldownLogic;
            ConfigurationSection bossBarSection = section.getConfigurationSection("bossbar");
            this.bossBarEnable = bossBarSection.getBoolean("enable", false);
            this.bossBarColor = BarColor.valueOf(bossBarSection.getString("color", "WHITE").toUpperCase());
            this.bossBarStyle = BarStyle.valueOf(bossBarSection.getString("style", "SOLID").toUpperCase());
            ConfigurationSection titleSection = section.getConfigurationSection("title");
            this.titleEnable = titleSection.getBoolean("enable", false);
            this.titleText = ChatColor.translateAlternateColorCodes('&', titleSection.getString("title-text", ""));
            this.subtitleText = ChatColor.translateAlternateColorCodes('&', titleSection.getString("subtitle-text", ""));
            this.soundEnable = section.getBoolean("sound-enable", false);
            this.sound = Sound.valueOf(section.getString("sound", "ENTITY_CREEPER_DEATH").toUpperCase());
            this.effectsEnable = section.getBoolean("effects-enable", false);
            this.effects = this.parseEffects(section.getStringList("effects"));
            this.cooldowns = new HashMap<>();
            ConfigurationSection cooldownSection = section.getConfigurationSection("cooldown");
            Iterator var7 = cooldownSection.getKeys(false).iterator();

            while(var7.hasNext()) {
                String key = (String)var7.next();
                ConfigurationSection permSection = cooldownSection.getConfigurationSection(key);
                String permission = permSection.getString("permission");
                int cooldown = permSection.getInt("cooldown");
                this.cooldowns.put(Integer.parseInt(key), new Cooldown(permission, cooldown));
            }

            this.commandsCooldown = section.getStringList("commands-cooldown");
        }

        public List<String> getCommandsCooldown() {
            return this.commandsCooldown;
        }

        public boolean handleCommand(Player player, String command, String[] args) {
            if (this.cooldownLogic.canUseCommand(player, command)) {
                this.executeCommand(player, command, args);
                return true;
            } else {
                Optional<Map.Entry<Integer, Cooldown>> highestPriorityCooldown = this.cooldowns.entrySet().stream().filter((entry) -> {
                    return player.hasPermission(((Cooldown)entry.getValue()).getPermission());
                }).max(Comparator.comparingInt(Map.Entry::getKey));
                if (highestPriorityCooldown.isPresent()) {
                    this.startCooldown(player, command, ((Cooldown)((Map.Entry)highestPriorityCooldown.get()).getValue()).getCooldown(), args);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
                    return false;
                }
            }
        }

        public void startCooldown(final Player player, final String command, final int cooldownTime, final String[] args) {
            // Проверка на режим PVP
            if (cooldownLogic.isPlayerInPvP(player)) {
                // Если игрок в режиме PVP, сразу выполняем команду без задержки
                this.executeCommand(player, command, args);
                return;
            }

            // Если игрок находится в дуэли (и не оператор) – выходим без сообщения
            if (plugin.isPlayerInDuel(player.getUniqueId()) && !player.isOp()) {
                return;
            }

            this.cooldownLogic.playerCooldowns.put(player, cooldownTime);
            NeoPlaceholders.setCooldown(player, cooldownTime);
            int commandCooldownTime = this.plugin.getConfig().getInt("cooldown-commands", 5);
            this.startCommandCooldown(player, commandCooldownTime);

            if (this.bossBarEnable) {
                String bossbarMsgTemplate = this.plugin.getConfig().getString("messages.bossbar-msg");
                final String translatedBossbarMsgTemplate = ChatColor.translateAlternateColorCodes('&', bossbarMsgTemplate);

                // Создаем BossBar с начальным значением таймера
                final BossBar bossBar = Bukkit.createBossBar(
                        translatedBossbarMsgTemplate.replace("{time}", String.valueOf(cooldownTime)),
                        this.bossBarColor,
                        this.bossBarStyle,
                        new BarFlag[0]
                );
                bossBar.addPlayer(player);
                this.cooldownLogic.addPlayerBossBar(player, bossBar);

                // Отображаем информацию об отмене телепорта через JsonMessageBuilder
                JsonMessageBuilder.sendCancelMessage(player);
                JsonMessageBuilder.sendCancelButton(player, this.plugin.getConfig().getString("cancel-button.text"));

                // УДАЛЯЕМ ДУБЛИРУЮЩИЙ КОД - не отправляем то же сообщение второй раз
                // String infoText = this.plugin.getConfig().getString("messages.info-text", "");
                // if (!infoText.isEmpty()) {
                //     player.sendMessage(ChatColor.translateAlternateColorCodes('&', infoText)
                //             .replace("{cooldown}", String.valueOf(cooldownTime)));
                // }

                // Сохраняем общее время для расчета прогресса
                final int totalCooldownTime = cooldownTime;

                BukkitRunnable runnable = new BukkitRunnable() {
                    int timeLeft = cooldownTime;

                    public void run() {
                        // Проверяем, не вошел ли игрок в режим PVP во время кулдауна
                        if (cooldownLogic.isPlayerInPvP(player)) {
                            // Если игрок в PVP, отменяем кулдаун и выполняем команду сразу
                            CommandGroup.this.cooldownLogic.playerCooldowns.put(player, 0);
                            NeoPlaceholders.setCooldown(player, 0);
                            bossBar.removeAll();
                            CommandGroup.this.cooldownLogic.removePlayerBossBar(player);
                            CommandGroup.this.executeCommand(player, command, args);
                            this.cancel();
                            return;
                        }

                        // Обновляем данные о кулдауне
                        CommandGroup.this.cooldownLogic.playerCooldowns.put(player, timeLeft);
                        NeoPlaceholders.setCooldown(player, timeLeft);

                        // Обновляем заголовок и прогресс босс-бара
                        bossBar.setTitle(translatedBossbarMsgTemplate.replace("{time}", String.valueOf(timeLeft)));
                        bossBar.setProgress((double)timeLeft / (double)totalCooldownTime);

                        // Уменьшаем счетчик ПЕРЕД проверкой на завершение
                        timeLeft--;

                        // Проверяем, завершился ли таймер
                        if (timeLeft < 0) {
                            // Устанавливаем значение в 0 перед завершением
                            CommandGroup.this.cooldownLogic.playerCooldowns.put(player, 0);
                            NeoPlaceholders.setCooldown(player, 0);

                            // Удаляем BossBar и выполняем команду
                            bossBar.removeAll();
                            CommandGroup.this.cooldownLogic.removePlayerBossBar(player);
                            CommandGroup.this.executeCommand(player, command, args);
                            this.cancel();
                        }
                    }
                };

                // Важно: запускаем БЕЗ начальной задержки (0L), чтобы сразу начать отсчет
                runnable.runTaskTimer(this.plugin, 0L, 20L);
                this.cooldownLogic.playerCooldownRunnables.put(player, runnable);
            } else {
                // Для случая без BossBar оставляем как есть
                String infoText = this.plugin.getConfig().getString("messages.info-text", "");
                if (!infoText.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', infoText)
                            .replace("{cooldown}", String.valueOf(cooldownTime)));
                }

                BukkitRunnable runnable = new BukkitRunnable() {
                    int timeLeft = cooldownTime;

                    public void run() {
                        // Проверяем, не вошел ли игрок в режим PVP во время кулдауна
                        if (cooldownLogic.isPlayerInPvP(player)) {
                            // Если игрок в PVP, отменяем кулдаун и выполняем команду сразу
                            CommandGroup.this.cooldownLogic.playerCooldowns.put(player, 0);
                            NeoPlaceholders.setCooldown(player, 0);
                            CommandGroup.this.executeCommand(player, command, args);
                            this.cancel();
                            return;
                        }

                        // Обновляем данные о кулдауне
                        CommandGroup.this.cooldownLogic.playerCooldowns.put(player, timeLeft);
                        NeoPlaceholders.setCooldown(player, timeLeft);

                        // Уменьшаем счетчик ПЕРЕД проверкой на завершение
                        timeLeft--;

                        // Проверяем, завершился ли таймер
                        if (timeLeft < 0) {
                            // Устанавливаем значение в 0 перед завершением
                            CommandGroup.this.cooldownLogic.playerCooldowns.put(player, 0);
                            NeoPlaceholders.setCooldown(player, 0);

                            CommandGroup.this.executeCommand(player, command, args);
                            this.cancel();
                        }
                    }
                };

                // Запускаем БЕЗ начальной задержки (0L)
                runnable.runTaskTimer(this.plugin, 0L, 20L);
                this.cooldownLogic.playerCooldownRunnables.put(player, runnable);
            }
        }

        private void startCommandCooldown(final Player player, int commandCooldownTime) {
            if (!this.cooldownLogic.playerCommandCooldowns.containsKey(player)) {
                this.cooldownLogic.playerCommandCooldowns.put(player, new HashSet<>());
            }

            ((Set)this.cooldownLogic.playerCommandCooldowns.get(player)).addAll(this.commandsCooldown);
            BukkitRunnable commandCooldownRunnable = new BukkitRunnable() {
                public void run() {
                    ((Set)CommandGroup.this.cooldownLogic.playerCommandCooldowns.get(player)).removeAll(CommandGroup.this.commandsCooldown);
                }
            };
            commandCooldownRunnable.runTaskLater(this.plugin, (long)commandCooldownTime * 20L);
        }

        private void executeCommand(Player player, String command, String[] args) {
            // Проигрываем звук ДО выполнения команды, если он включен
            if (this.soundEnable) {
                try {
                    player.playSound(player.getLocation(), this.sound, 1.0F, 1.0F);
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Проигрываем звук " + this.sound.name() + " для игрока " + player.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при проигрывании звука: " + e.getMessage());
                }
            }

            // Показываем title, если он включен
            if (this.titleEnable) {
                player.sendTitle(this.titleText, this.subtitleText, 10, 70, 20);
            }

            // Применяем эффекты, если они включены
            if (this.effectsEnable) {
                Iterator var5 = this.effects.iterator();
                while(var5.hasNext()) {
                    PotionEffect effect = (PotionEffect)var5.next();
                    player.addPotionEffect(effect);
                }
            }

            // Выполняем команду
            String fullCommand = command + " " + String.join(" ", args);
            Bukkit.dispatchCommand(player, fullCommand);
        }

        private List<PotionEffect> parseEffects(List<String> effectsStrings) {
            List<PotionEffect> effects = new ArrayList<>();

            if (effectsStrings == null) {
                return effects;
            }

            Iterator var3 = effectsStrings.iterator();
            while(var3.hasNext()) {
                String effectString = (String)var3.next();
                String[] parts = effectString.split(":");
                if (parts.length >= 2) {
                    try {
                        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                        if (type == null) {
                            plugin.getLogger().warning("Неизвестный эффект: " + parts[0]);
                            continue;
                        }

                        int duration = Integer.parseInt(parts[1]) * 20; // Преобразуем секунды в тики (20 тиков = 1 секунда)
                        int amplifier = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                        effects.add(new PotionEffect(type, duration, amplifier));
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Ошибка в формате эффекта: " + effectString);
                    }
                }
            }

            return effects;
        }
    }
}