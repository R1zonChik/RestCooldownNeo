package ru.refontstudio.restcooldownneo.logic;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.refontstudio.restcooldownneo.RestCooldownNeo;
import ru.refontstudio.restcooldownneo.utils.WorldGuardUtils;

public class DamageListener implements Listener {
    private final CooldownLogic cooldownLogic;
    private final RestCooldownNeo plugin;

    public DamageListener(CooldownLogic cooldownLogic, RestCooldownNeo plugin) {
        this.cooldownLogic = cooldownLogic;
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PLAYER) {
            Player player = (Player)entity;
            Entity damager = event.getDamager();
            if (damager.getType() == EntityType.PLAYER) {
                Player attacker = (Player)damager;
                boolean isPvPAllowed = WorldGuardUtils.isPvpAllowed(player) && WorldGuardUtils.isPvpAllowed(attacker);
                if (isPvPAllowed) {
                    if (this.cooldownLogic.isPlayerInCooldown(player)) {
                        this.cooldownLogic.cancelAllCommands(player);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("messages.on-damage")));
                    }

                    if (this.cooldownLogic.isPlayerInCooldown(attacker)) {
                        this.cooldownLogic.cancelAllCommands(attacker);
                        attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("messages.you-damager")));
                    }
                }
            }
        }
    }
}