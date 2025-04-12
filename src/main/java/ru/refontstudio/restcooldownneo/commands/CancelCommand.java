package ru.refontstudio.restcooldownneo.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ru.refontstudio.restcooldownneo.RestCooldownNeo;
import ru.refontstudio.restcooldownneo.logic.CooldownLogic;

public class CancelCommand implements CommandExecutor, TabCompleter {
    private final RestCooldownNeo plugin;
    private final CooldownLogic cooldownLogic;

    public CancelCommand(RestCooldownNeo plugin, CooldownLogic cooldownLogic) {
        this.plugin = plugin;
        this.cooldownLogic = cooldownLogic;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        } else {
            List<String> completions = new ArrayList<>();
            if (alias.equalsIgnoreCase("neocommands")) {
                if (args.length == 1) {
                    completions.add("info");
                    completions.add("reload");
                    completions.add("newcommand");
                } else if (args[0].equalsIgnoreCase("newcommand")) {
                    if (args.length == 2) {
                        completions.add("Команда на которую вы хотите добавить кулдаун");
                    } else if (args.length == 3) {
                        completions.addAll(Arrays.asList("default", "special"));
                    }
                }
            }

            return completions;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только для игроков.");
            return true;
        } else {
            Player player = (Player)sender;

            // Если игрок не оп и находится в дуэли
            if (!player.isOp() && plugin.isPlayerInDuel(player.getUniqueId())) {
                // Проверяем, является ли команда /hub
                if (!label.equalsIgnoreCase("hub")) {
                    // Если не hub - тихо блокируем команду
                    return true;
                }
                // Если это hub - пропускаем эту проверку и продолжаем выполнение
            }

            if (label.equalsIgnoreCase("neocommand_cancel")) {
                // УДАЛЯЕМ ДУБЛИРОВАНИЕ: метод cancelAllCommands уже отправляет сообщение
                this.cooldownLogic.cancelAllCommands(player);
                // Убираем этот блок кода, который дублирует сообщение:
                // if (cancelled) {
                //     group = this.plugin.getConfig().getString("messages.teleport-is-cancel", "");
                //     player.sendMessage(ChatColor.translateAlternateColorCodes('&', group));
                // }
                return true;
            } else {
                String newCommand;
                if (!player.hasPermission("neocooldown.admin")) {
                    newCommand = this.plugin.getConfig().getString("messages.no-permission");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', newCommand));
                    return true;
                } else if (label.equalsIgnoreCase("neocommands")) {
                    if (args.length == 0) {
                        newCommand = this.plugin.getConfig().getString("messages.use-tab-completer");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', newCommand));
                        return true;
                    } else if (args[0].equalsIgnoreCase("info")) {
                        newCommand = "&f\n&c• &cRestCooldownNeo &fMade By &5Refont &f| Version: &c1.0\n&f\n  &fДоступные команды:\n&c• &c/neocooldown info &f- Просмотреть информацию о плагине\n&c• &c/neocooldown reload - &fПерезагрузить плагин\n&f\n&f  Доступные Пермишены:\n&c• &cneocooldown.admin &f- Права на все команды плагина +пропуск кулдауна\n&f";
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', newCommand));
                        return true;
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        this.plugin.reloadPlugin();
                        newCommand = this.plugin.getConfig().getString("messages.reload");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', newCommand));
                        return true;
                    } else if (args[0].equalsIgnoreCase("newcommand")) {
                        if (args.length != 3) {
                            player.sendMessage(ChatColor.RED + "Используйте: /neocommands newcommand <команда> <группа>");
                            return true;
                        } else {
                            newCommand = args[1];
                            group = args[2];
                            FileConfiguration config = this.plugin.getConfig();
                            if (config.isConfigurationSection("command-groups." + group)) {
                                List<String> commandsCooldown = config.getStringList("command-groups." + group + ".commands-cooldown");
                                commandsCooldown.add(newCommand);
                                config.set("command-groups." + group + ".commands-cooldown", commandsCooldown);
                                this.plugin.saveConfig();
                                String saveReloadMessage = this.plugin.getConfig().getString("messages.save-reload", "&c• &fПерезагрузите плагин для сохранения команды");
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', saveReloadMessage));
                            } else {
                                player.sendMessage(ChatColor.RED + "Группа " + group + " Не найдена.");
                            }

                            return true;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }
}