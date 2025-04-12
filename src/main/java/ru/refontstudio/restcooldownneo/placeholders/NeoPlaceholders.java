package ru.refontstudio.restcooldownneo.placeholders;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import ru.refontstudio.restcooldownneo.RestCooldownNeo;

public class NeoPlaceholders {
    private static final String COOLDOWN_PLACEHOLDER = "cooldown";

    public NeoPlaceholders() {
    }

    public static void setCooldown(Player player, int cooldown) {
        player.setMetadata("cooldown", new FixedMetadataValue(RestCooldownNeo.getInstance(), cooldown));
    }

    public static void updateCooldown(Player player) {
        if (player.hasMetadata("cooldown")) {
            List<MetadataValue> metadataValues = player.getMetadata("cooldown");
            if (!metadataValues.isEmpty()) {
                int currentCooldown = ((MetadataValue)metadataValues.get(0)).asInt();
                player.setMetadata("cooldown", new FixedMetadataValue(RestCooldownNeo.getInstance(), currentCooldown - 1));
            }
        }
    }

    public static String parsePlaceholders(Player player, String text) {
        if (player.hasMetadata("cooldown")) {
            int cooldown = ((MetadataValue)player.getMetadata("cooldown").get(0)).asInt();
            text = text.replace("{time}", String.valueOf(cooldown));
            text = text.replace("{cooldown}", String.valueOf(cooldown));
        } else {
            text = text.replace("{time}", "0");
            text = text.replace("{cooldown}", "0");
        }

        return text;
    }
}