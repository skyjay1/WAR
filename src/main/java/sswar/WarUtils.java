package sswar;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import sswar.capability.IWarMember;
import sswar.data.TeamSavedData;
import sswar.data.WarSavedData;
import sswar.util.MessageUtils;
import sswar.war.War;
import sswar.war.recruit.WarRecruit;
import sswar.war.team.WarTeam;
import sswar.war.team.WarTeamEntry;
import sswar.war.team.WarTeams;

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
     * Called when a player forfeits to check if the war should end
     * @param player the player that forfeited
     * @param teamData the war team data
     * @param teams the war teams
     * @param team the player war team
     * @param entry the player war team entry
     */
    public static void onPlayerForfeit(final ServerPlayer player, final TeamSavedData teamData, final WarTeams teams, final WarTeam team, final WarTeamEntry entry) {
        // TODO
    }


    public static void onPlayerAcceptRecruit(final ServerPlayer player, final WarSavedData data, final UUID warId) {
        Optional<War> oWar = data.getWar(warId);
        // send feedback to war owner, if present
        oWar.ifPresent(war -> {
            if(war.hasOwner()) {
                ServerPlayer owner = player.getServer().getPlayerList().getPlayer(war.getOwner());
                if(owner != null) {
                    MessageUtils.sendMessage(owner, "command.war.accept.feedback", player.getDisplayName().getString());
                }
            }
        });
    }

    public static void onPlayerDenyRecruit(final ServerPlayer player, final WarSavedData data, final UUID warId) {
        Optional<War> oWar = data.getWar(warId);
        // send feedback to war owner, if present
        oWar.ifPresent(war -> {
            if(war.hasOwner()) {
                ServerPlayer owner = player.getServer().getPlayerList().getPlayer(war.getOwner());
                if(owner != null) {
                    MessageUtils.sendMessage(owner, "command.war.deny.feedback", player.getDisplayName().getString());
                }
            }
        });
    }

    public static void onRecruitExpire(final MinecraftServer server, final WarSavedData data, final UUID warId, final WarRecruit recruit) {
        // TODO if unsuccessful: remove war and recruit from data; send messages to invited players about cancellation
        // TODO if successful: update war state, send messages to participating players
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

    public static boolean openWarMenu(final ServerPlayer player, final int maxPlayers) {
        // TODO
        return false;
    }

    public static Component createRecruitComponent() {
        // create components
        Component feedback = MessageUtils.component("message.war_recruit.prompt");
        Component yes = MessageUtils.component("message.war_recruit.prompt.yes")
                .withStyle(ChatFormatting.GREEN)
                .withStyle(a -> a
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MessageUtils.component("message.war_recruit.prompt.yes.tooltip")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/war accept")));
        Component no = MessageUtils.component("message.war_recruit.prompt.no")
                .withStyle(ChatFormatting.RED)
                .withStyle(a -> a
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MessageUtils.component("message.war_recruit.prompt.no.tooltip")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/war deny")));
        // combine components
        feedback.getSiblings().add(Component.literal(" "));
        feedback.getSiblings().add(yes);
        feedback.getSiblings().add(Component.literal(" "));
        feedback.getSiblings().add(no);
        return feedback;
    }
}
