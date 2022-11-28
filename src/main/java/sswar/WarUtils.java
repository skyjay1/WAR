package sswar;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import sswar.capability.IWarMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WarUtils {

    /**
     * Selects a random player from the server, excluding players with the given UUIDs
     * @param server the server
     * @param blacklist the players to exclude
     * @return a random valid player, or empty if none is found
     */
    public static Optional<ServerPlayer> findValidPlayer(final MinecraftServer server, final Collection<UUID> blacklist) {
        final List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        // remove blacklisted players
        players.removeIf(p -> blacklist.contains(p.getUUID()));
        // randomize order
        Collections.shuffle(players);
        // return the first valid player
        for(ServerPlayer player : players) {
            if(isValidPlayer(player)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    /**
     * @param player the player
     * @return true if the player is not currently or recently in a war
     */
    public static boolean isValidPlayer(final ServerPlayer player) {
        Optional<IWarMember> oWarMember = player.getCapability(SSWar.WAR_MEMBER).resolve();
        if(oWarMember.isPresent()) {
            IWarMember iWarMember = oWarMember.get();
            return !iWarMember.hasActiveWar() && (iWarMember.getWarEndedTimestamp() <= 0 || player.level.getGameTime() - iWarMember.getWarEndedTimestamp() > SSWar.CONFIG.getPlayerWarCooldownTicks());
        }
        return false;
    }


    /**
     * @param server the server
     * @param player the player UUID
     * @return the player name if it is known, otherwise the UUID as a string
     */
    public static String getPlayerName(final MinecraftServer server, final UUID player) {
        Optional<GameProfile> oProfile = server.getProfileCache().get(player);
        if(oProfile.isPresent()) {
            return oProfile.get().getName();
        }
        return player.toString();
    }

    public static boolean openWarCompassMenu(final ServerPlayer player, final ItemStack compass, final int slot) {
        // TODO
        return false;
    }

    public static boolean openWarMenu(final ServerPlayer player) {
        // TODO
        return false;
    }
}
