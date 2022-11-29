package sswar.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public final class MessageUtils {

    /**
     * Sends a system message to the given player as a translated component
     *
     * @param player the player
     * @param key    the translation key
     * @param format the message format
     * @param params the translation parameters
     */
    public static void sendMessage(Player player, String key, ChatFormatting format, Object... params) {
        player.displayClientMessage(component(key, params).withStyle(format), false);
    }

    /**
     * Sends a system message to the given player as a translated component
     *
     * @param player the player
     * @param key    the translation key
     * @param params the translation parameters
     */
    public static void sendMessage(Player player, String key, Object... params) {
        player.displayClientMessage(component(key, params), false);
    }

    /**
     * Creates a text component with the translated key
     *
     * @param key    the translation key
     * @param params the translation parameters
     * @return the text component
     */
    public static MutableComponent component(String key, Object... params) {
        return Component.translatable(key, params);
    }

    /**
     * Creates a text component with the translated key
     *
     * @param key    the translation key
     * @param color the text color
     * @param params the translation parameters
     * @return the text component
     */
    public static MutableComponent component(String key, ChatFormatting color, Object... params) {
        return Component.translatable(key, params).withStyle(color);
    }
}