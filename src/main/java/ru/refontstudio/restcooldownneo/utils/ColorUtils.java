package ru.refontstudio.restcooldownneo.utils;

import org.bukkit.ChatColor;

public class ColorUtils {
    public ColorUtils() {
    }

    public static String translateHexColorCodes(String message) {
        String hexPattern = "&#[A-Fa-f0-9]{6}";
        StringBuilder translatedMessage = new StringBuilder();
        char[] b = message.toCharArray();

        for(int i = 0; i < b.length; ++i) {
            if (i + 7 < b.length && b[i] == '&' && b[i + 1] == '#' && isHexCode(b, i + 2)) {
                translatedMessage.append(convertHexToColorCode(message.substring(i + 2, i + 8)));
                i += 7;
            } else {
                translatedMessage.append(b[i]);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', translatedMessage.toString());
    }

    private static boolean isHexCode(char[] b, int start) {
        for(int i = 0; i < 6; ++i) {
            if (!isHexDigit(b[start + i])) {
                return false;
            }
        }

        return true;
    }

    private static boolean isHexDigit(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
    }

    private static String convertHexToColorCode(String hex) {
        StringBuilder colorCode = new StringBuilder("§x");
        char[] var2 = hex.toCharArray();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            char c = var2[var4];
            colorCode.append('§').append(c);
        }

        return colorCode.toString();
    }
}