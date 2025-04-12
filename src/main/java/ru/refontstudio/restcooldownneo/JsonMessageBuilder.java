package ru.refontstudio.restcooldownneo;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import ru.refontstudio.restcooldownneo.placeholders.NeoPlaceholders;

public class JsonMessageBuilder {
    public JsonMessageBuilder() {
    }

    public static void sendCancelMessage(Player player) {
        String message = RestCooldownNeo.getInstance().getConfig().getString("messages.info-text", "");
        String formattedMessage = NeoPlaceholders.parsePlaceholders(player, message);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', formattedMessage));
    }

    public static void sendCancelButton(Player player, String cancelText) {
        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', cancelText));
        message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/neocommand_cancel"));
        player.spigot().sendMessage(message);
    }
}