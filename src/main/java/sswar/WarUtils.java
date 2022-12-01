package sswar;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;
import sswar.data.TeamSavedData;
import sswar.data.WarSavedData;
import sswar.menu.DeclareWarMenu;
import sswar.util.MessageUtils;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;
import sswar.war.team.WarTeam;
import sswar.war.team.WarTeamEntry;
import sswar.war.team.WarTeams;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WarUtils {

    public static final int MAX_PLAYER_COUNT = 1024;
    public static final int WAR_NAME_MAX_LENGTH = 24;
    public static final String WAR_NAME_REGEX = "[a-zA-Z0-9_ ]{1," + WAR_NAME_MAX_LENGTH + "}";

    public static final String WAR_NAME = "War";
    public static final String TEAM_A_NAME = "Team A";
    public static final String TEAM_B_NAME = "Team B";

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
     * @param server the server
     * @param blacklist the players to exclude
     * @return a list of valid players, excluding the ones in the blacklist
     */
    public static List<ServerPlayer> listValidPlayers(final MinecraftServer server, final Collection<UUID> blacklist) {
        final List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        // remove blacklisted players
        players.removeIf(p -> blacklist.contains(p.getUUID()) || !isValidPlayer(p));
        return players;
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
     * @param uuid the player UUID
     * @return true if the player is online and valid according to {@link #isValidPlayer(ServerPlayer)}
     */
    public static boolean isValidPlayer(final MinecraftServer server, final UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if(null == player) {
            return false;
        }
        return isValidPlayer(player);
    }

    /**
     * Called when a player forfeits
     * @param player the player that forfeited
     * @param teamData the war team data
     * @param teams the war teams
     * @param team the player war team
     * @param entry the player war team entry
     */
    public static void onPlayerForfeit(final ServerPlayer player, final TeamSavedData teamData, final WarTeams teams, final WarTeam team, final WarTeamEntry entry) {
        // TODO send messages to players in the same war
    }

    /**
     * Called when a player dies to update death count
     * @param player the player that died
     * @param warId the ID of the active war
     */
    public static void onPlayerDeath(final ServerPlayer player, final UUID warId) {
        // load war data and active war
        final WarSavedData warData = WarSavedData.get(player.getServer());
        final Optional<War> oWar = warData.getWar(warId);
        if(oWar.isEmpty()) {
            return;
        }
        final War war = oWar.get();
        // verify war is active
        if(war.getState() != WarState.ACTIVE) {
            return;
        }
        // load war teams
        final TeamSavedData teamData = TeamSavedData.get(player.getServer());
        final Optional<WarTeams> oTeams = teamData.getTeams(warId);
        if(oTeams.isEmpty()) {
            warData.invalidateWar(warId);
            return;
        }
        final WarTeams teams = oTeams.get();
        // load player team
        final Optional<WarTeam> oTeam = teams.getTeamForPlayer(player.getUUID());
        if(oTeam.isEmpty()) {
            return;
        }
        final WarTeam team = oTeam.get();
        // load player entry
        final Optional<WarTeamEntry> oEntry = team.getEntry(player.getUUID());
        if(oEntry.isEmpty()) {
            return;
        }
        // add death count
        oEntry.get().addDeath();
        teamData.setDirty();
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
        // TODO filter teams by players who accepted recruit request
    }

    /**
     * Loads the player if they are online and updates stats and items
     * @param server the server
     * @param playerId the player
     * @param isWin true if the player wins
     * @return true if the player was rewarded
     */
    public static boolean reward(final MinecraftServer server, final UUID playerId, final boolean isWin) {
        // load the player
        final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if(null == player) {
            return false;
        }
        // load capability
        IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(isWin) {
            // add stats
            iWarMember.addWin();
            // TODO give item
            // TODO add fireworks
        } else {
            // add stats
            iWarMember.addLoss();
        }
        // reward was successful
        return true;
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
        // create container with player heads
        final SimpleContainer playerHeads = createPlayerHeads(player, listValidPlayers(player.getServer(), List.of(player.getUUID())));
        // open menu
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) ->
                        new DeclareWarMenu(id, playerHeads, maxPlayers), MessageUtils.component("gui.sswar.declare_war")),
                buf -> {
                    buf.writeInt(playerHeads.getContainerSize());
                    buf.writeInt(maxPlayers);
                    CompoundTag tag = new CompoundTag();
                    tag.put(DeclareWarMenu.KEY_VALID_PLAYER_ITEMS, playerHeads.createTag());
                    buf.writeNbt(tag);
                });
        return true;
    }


    public static SimpleContainer createPlayerHeads(final ServerPlayer required, final List<ServerPlayer> validPlayers) {
        // create container
        SimpleContainer container = new SimpleContainer(validPlayers.size() + 1);
        // add non-required players
        for(int i = 0, n = validPlayers.size(); i < n; i++) {
            container.setItem(i, createPlayerHead(validPlayers.get(i), false));
        }
        // required player is last
        container.addItem(createPlayerHead(required, true));
        return container;
    }


    /**
     * @param player the player
     * @param required true if the player is marked as required
     * @return an ItemStack with a player head for the given player
     */
    public static ItemStack createPlayerHead(final ServerPlayer player, final boolean required) {
        ItemStack itemStack = Items.PLAYER_HEAD.getDefaultInstance();
        GameProfile profile = player.getGameProfile();
        CompoundTag tag = new CompoundTag();
        NbtUtils.writeGameProfile(tag, profile);
        itemStack.getOrCreateTag().put(DeclareWarMenu.KEY_SKULL_OWNER, tag);
        if(required) {
            // TODO re-enable itemStack.getTag().putBoolean(DeclareWarMenu.KEY_REQUIRED_PLAYER, true);
        }
        return itemStack;
    }

    /**
     * Checks if either team is empty switches player teams until they are both non-empty
     * @param server the server
     * @param data the team data
     * @param teamA the first team
     * @param teamB the second team
     * @return true if the teams are valid
     */
    public static boolean tryBalanceTeams(final MinecraftServer server, final TeamSavedData data, final WarTeam teamA, final WarTeam teamB) {
        // move players from team B to team A
        if(teamA.getTeam().isEmpty()) {
            if(teamB.getTeam().size() < 2) {
                return false;
            }
            // move one random player
            UUID player = Util.getRandom(teamB.getTeam().keySet().toArray(new UUID[0]), server.overworld().getRandom());
            WarTeamEntry entry = teamB.getTeam().remove(player);
            teamA.getTeam().put(player, entry);
            data.setDirty();
            return true;
        }
        // move players from team B to team A
        if(teamB.getTeam().isEmpty()) {
            if(teamA.getTeam().size() < 2) {
                return false;
            }
            // move one random player
            UUID player = Util.getRandom(teamA.getTeam().keySet().toArray(new UUID[0]), server.overworld().getRandom());
            WarTeamEntry entry = teamA.getTeam().remove(player);
            teamB.getTeam().put(player, entry);
            data.setDirty();
            return true;
        }
        // both teams are non-empty
        return true;
    }

    /**
     * Attempts to create war and war recruit objects from the given data
     * @param owner the player who created the war, or null for server events
     * @param warName the war name
     * @param nameA the name of team A
     * @param nameB the name of team B
     * @param listA the list of players to include in team A, can be empty
     * @param listB the list of players to include in team B, can be empty
     * @param maxPlayers the maximum number of players to participate
     * @return the war ID if it was successfully created
     */
    public static Optional<UUID> tryCreateWar(@Nullable final UUID owner, final String warName, final String nameA, final String nameB,
                                              final List<UUID> listA, final List<UUID> listB, final int maxPlayers) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(null == server) {
            return Optional.empty();
        }
        // ensure all players are online and valid
        final List<UUID> teamA = new ArrayList<>(listA);
        teamA.removeIf(uuid -> !isValidPlayer(server, uuid));
        final List<UUID> teamB = new ArrayList<>(listB);
        teamB.removeIf(uuid -> !isValidPlayer(server, uuid));
        // ensure there is at least 1 player per team
        // EDIT: teams can be empty for server-created wars
        /*if(teamA.isEmpty() || teamB.isEmpty()) {
            return false;
        }*/
        // determine game time
        final long gameTime = server.getLevel(Level.OVERWORLD).getGameTime();
        // load war data
        final WarSavedData warData = WarSavedData.get(server);
        // create war
        final Pair<UUID, War> pair = warData.createWar(owner, warName, gameTime, maxPlayers);
        // load WarRecruit
        final Optional<WarRecruit> oRecruit = warData.getRecruit(pair.getFirst());
        oRecruit.ifPresent(recruit -> {
            recruit.addAll(server, warData, pair.getSecond(), teamA, gameTime);
            recruit.addAll(server, warData, pair.getSecond(), teamB, gameTime);
            // update war state
            pair.getSecond().setState(WarState.RECRUITING);
            warData.setDirty();
        });
        // load team data
        final TeamSavedData teamData = TeamSavedData.get(server);
        teamData.addTeams(pair.getFirst(), nameA, nameB, teamA, teamB);
        return Optional.of(pair.getFirst());
    }





    public static Component createRecruitComponent() {
        // create components
        Component feedback = Component.empty().withStyle(ChatFormatting.BOLD);
        feedback.getSiblings().add(MessageUtils.component("message.war_recruit.prompt"));
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
