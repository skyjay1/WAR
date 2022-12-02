package sswar.war;

import com.google.common.collect.Iterables;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.INBTSerializable;
import sswar.SSWar;
import sswar.WarUtils;
import sswar.data.WarSavedData;
import sswar.util.MessageUtils;
import sswar.war.recruit.WarRecruit;
import sswar.war.team.WarTeam;
import sswar.war.team.WarTeams;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class War implements INBTSerializable<CompoundTag> {

    @Nullable
    private UUID owner;
    private String name;
    private WarState state;
    private long createdTimestamp;
    private LocalDateTime createdDate;
    private long prepareTimestamp;
    private long activateTimestamp;
    private long endTimestamp;

    //// CONSTRUCTORS ////

    public War(@Nullable UUID owner, String name, long createdTimestamp) {
        this.owner = owner;
        this.name = name;
        this.state = WarState.CREATING;
        this.createdTimestamp = createdTimestamp;
        this.createdDate = LocalDateTime.now();
    }

    public War(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// HELPER METHODS ////

    public boolean hasOwner() {
        return owner != null;
    }

    /**
     * Called when a war failed recruiting to update state and send messages
     * @param server the server
     * @param warData the war data
     * @param warId the war ID
     * @param war the war instance
     * @param teams the war teams
     * @param recruit the war recruit instance
     */
    public static void cancelRecruiting(final MinecraftServer server, final WarSavedData warData, final UUID warId, final War war, final WarTeams teams, final WarRecruit recruit) {
        // update state
        warData.invalidateWar(warId);
        // send messages to all players who were recruited, regardless of status
        Component message = MessageUtils.component("message.war.lifecycle.recruit.cancel").withStyle(ChatFormatting.RED);
        for(UUID playerId : recruit.getInvitedPlayers().keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if(player != null) {
                player.displayClientMessage(message, false);
            }
        }
    }

    /**
     * Called when a war successfully finishes recruiting to update state and send messages
     * @param server the server
     * @param warData the war data
     * @param warId the war ID
     * @param war the war instance
     * @param teams the war teams
     * @param timestamp the game time
     */
    public static void startPreparing(final MinecraftServer server, final WarSavedData warData, final UUID warId, final War war,
                                      final WarTeams teams, final long timestamp) {
        // update state and timestamp
        war.setState(WarState.PREPARING);
        war.setPrepareTimestamp(timestamp);
        warData.setDirty();
        // send messages to players in each team
        for(WarTeam team : teams) {
            // create message
            Component message = Component.empty();
            message.getSiblings().add(MessageUtils.component("message.war.lifecycle.start_preparation").withStyle(ChatFormatting.YELLOW));
            message.getSiblings().add(Component.literal(" "));
            message.getSiblings().add(MessageUtils.component("message.war.lifecycle.start_preparation.team_name", team.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
            message.getSiblings().add(Component.literal(" "));
            // create team members component
            Component playerNames = Component.empty();
            for(String name : team.getPlayerNames(server).values()) {
                playerNames.getSiblings().add(Component.literal(name));
                playerNames.getSiblings().add(Component.literal(", "));
            }
            // remove trailing comma
            playerNames.getSiblings().remove(playerNames.getSiblings().size() - 1);
            // add team members component
            message.getSiblings().add(MessageUtils.component("message.war.lifecycle.start_preparation.team_members", playerNames.getString()));
            // send message to each player
            for(UUID playerId : team) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if(player != null) {
                    player.displayClientMessage(message, false);
                }
            }
        }
    }

    /**
     * Called when a war finishes preparing to update state and send messages
     * @param server the server
     * @param warData the war data
     * @param warId the war ID
     * @param war the war instance
     * @param teams the war teams
     * @param timestamp the game time
     */
    public static void startActivate(final MinecraftServer server, final WarSavedData warData, final UUID warId, final War war,
                                      final WarTeams teams, final long timestamp) {
        // update state and timestamp
        war.setState(WarState.ACTIVE);
        war.setPrepareTimestamp(timestamp);
        warData.setDirty();
        // send messages to players in each team
        Component message = MessageUtils.component("message.war.lifecycle.activate").withStyle(ChatFormatting.YELLOW);
        for(WarTeam team : teams) {
            for(UUID playerId : team) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if(player != null) {
                    player.displayClientMessage(message, false);
                }
            }
        }
    }

    /**
     * Called when a war ends to update state and send messages
     * @param server the server
     * @param warData the war data
     * @param warId the war ID
     * @param war the war instance
     * @param win the winning team
     * @param lose the losing team
     * @param timestamp the game time
     */
    public static void end(final MinecraftServer server, final WarSavedData warData, final UUID warId, final War war,
                                     final WarTeam win, final WarTeam lose, final boolean wasForfeit, final long timestamp) {
        if(win == lose) {
            SSWar.LOGGER.error("[War#end] Failed to end war because the winning and losing team are the same");
            warData.invalidateWar(warId);
            return;
        }
        // determine whether to give reward
        boolean hasReward = !war.hasOwner() && !(wasForfeit && lose.countPlayersWithDeaths() <= 0);
        // update win-lose
        win.setWin(true, hasReward);
        lose.setWin(false, hasReward);
        // update state
        war.setState(WarState.ENDED);
        war.setEndTimestamp(timestamp);
        warData.setDirty();
        // send messages to players in each team
        Component message = (wasForfeit ? MessageUtils.component("message.war.lifecycle.end.forfeit", lose.getName(), win.getName()) : MessageUtils.component("message.war.lifecycle.end.win", win.getName()))
                .withStyle(ChatFormatting.YELLOW);
        for(UUID playerId : Iterables.concat(win.getTeam().keySet(), lose.getTeam().keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if(player != null) {
                player.displayClientMessage(message, false);
            }
        }
    }

    //// GETTERS AND SETTERS ////

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public WarState getState() {
        return state;
    }

    public void setState(WarState state) {
        this.state = state;
    }

    public long getPrepareTimestamp() {
        return prepareTimestamp;
    }

    public void setPrepareTimestamp(long prepareTimestamp) {
        this.prepareTimestamp = prepareTimestamp;
    }

    public long getActivateTimestamp() {
        return activateTimestamp;
    }

    public void setActivateTimestamp(long activateTimestamp) {
        this.activateTimestamp = activateTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    //// NBT ////

    private static final String KEY_OWNER = "Owner";
    private static final String KEY_NAME = "Name";
    private static final String KEY_STATE = "State";
    private static final String KEY_CREATE = "Create";
    private static final String KEY_CREATE_DATE = "CreateDate";
    private static final String KEY_PREPARE = "Prepare";
    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_END = "End";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if(hasOwner()) {
            tag.putUUID(KEY_OWNER, owner);
        }
        tag.putString(KEY_NAME, name);
        tag.putByte(KEY_STATE, state.getId());
        tag.putLong(KEY_CREATE, createdTimestamp);
        tag.putLong(KEY_PREPARE, prepareTimestamp);
        tag.putLong(KEY_ACTIVE, activateTimestamp);
        tag.putLong(KEY_END, endTimestamp);
        tag.putString(KEY_CREATE_DATE, DATE_TIME_FORMATTER.format(createdDate));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if(tag.contains(KEY_OWNER)) {
            owner = tag.getUUID(KEY_OWNER);
        }
        name = tag.getString(KEY_NAME);
        state = WarState.getById(tag.getByte(KEY_STATE));
        createdTimestamp = tag.getLong(KEY_CREATE);
        createdDate = LocalDateTime.parse(tag.getString(KEY_CREATE_DATE), DATE_TIME_FORMATTER);
        prepareTimestamp = tag.getLong(KEY_PREPARE);
        activateTimestamp = tag.getLong(KEY_ACTIVE);
        endTimestamp = tag.getLong(KEY_END);
    }
}
