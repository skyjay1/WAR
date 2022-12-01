package sswar.war.recruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.INBTSerializable;
import sswar.WarUtils;
import sswar.data.WarSavedData;
import sswar.war.War;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarRecruit implements INBTSerializable<CompoundTag> {

    private Map<UUID, WarRecruitEntry> invitedPlayers = new HashMap<>();
    private int maxPlayers;

    public WarRecruit(final int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public WarRecruit(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// METHODS ////

    /**
     * Adds a player to this war recruit and sends a recruit message
     * @param server the server
     * @param warData the war saved data
     * @param uuid the player to add
     * @param timestamp the game time
     */
    public void add(final MinecraftServer server, WarSavedData warData, final UUID uuid, final long timestamp) {


        if(!invitedPlayers.containsKey(uuid)) {
            invitedPlayers.put(uuid, new WarRecruitEntry(timestamp));
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if(player != null) {
                player.displayClientMessage(WarUtils.createRecruitComponent(), false);
            }
        }
        warData.setDirty();
    }

    /**
     * Adds all of the players to this war recruit and sends a recruit message
     * @param server the server
     * @param warData the war saved data
     * @param war the war instance
     * @param players the players to add
     * @param timestamp the game time
     */
    public void addAll(final MinecraftServer server, WarSavedData warData, War war, final Collection<UUID> players, final long timestamp) {
        for(UUID uuid : players) {
            if(!invitedPlayers.containsKey(uuid)) {
                WarRecruitEntry entry = new WarRecruitEntry(timestamp);
                invitedPlayers.put(uuid, entry);
                // owner cannot change their state
                if(war.hasOwner() && uuid.equals(war.getOwner())) {
                    entry.setState(WarRecruitState.ACCEPT);
                    entry.setCanChange(false);
                    continue;
                }
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if(player != null) {
                    player.displayClientMessage(WarUtils.createRecruitComponent(), false);
                }
            }
        }
        warData.setDirty();
    }

    /**
     * Updates the recruit entry for the given player to ACCEPT
     * @param player the player
     * @return true if there is an entry for this player and the recruit is not full
     */
    public boolean accept(final UUID player) {
        WarRecruitEntry entry = invitedPlayers.get(player);
        if(entry != null && entry.canChange() && !isFull()) {
            entry.setState(WarRecruitState.ACCEPT);
            return true;
        }
        return false;
    }

    /**
     * Updates the recruit entry for the given player to REJECT
     * @param player the player
     * @return true if there is an entry for this player
     */
    public boolean deny(final UUID player) {
        WarRecruitEntry entry = invitedPlayers.get(player);
        if(entry != null && entry.canChange()) {
            entry.setState(WarRecruitState.DENY);
            return true;
        }
        return false;
    }

    /**
     * Updates the recruit entry for the given player to INVALID
     * @param player the player
     * @return true if there is an entry for this player
     */
    public boolean timeout(final UUID player) {
        WarRecruitEntry entry = invitedPlayers.get(player);
        if(entry != null) {
            entry.setState(WarRecruitState.INVALID);
            return true;
        }
        return false;
    }

    public Optional<WarRecruitEntry> getEntry(final UUID player) {
        return Optional.ofNullable(invitedPlayers.get(player));
    }

    public boolean isFull() {
        return getAcceptedCount() >= maxPlayers;
    }

    public boolean isFull(final Map<WarRecruitState, Integer> counts) {
        return counts.getOrDefault(WarRecruitState.ACCEPT, 0) >= maxPlayers;
    }

    /**
     * Iterates over all entries and counts any that are accepted
     * @return the total number of accepted entries
     * @see #getCounts()
     */
    public int getAcceptedCount() {
        return (int) invitedPlayers.values().stream().filter(entry -> entry.getState().isAccepted()).count();
    }

    /**
     * Iterates over all entries and counts any that are pending
     * @return the total number of pending entries
     * @see #getCounts()
     */
    public int getPendingCount() {
        return (int) invitedPlayers.values().stream().filter(entry -> entry.getState() == WarRecruitState.PENDING).count();
    }

    /**
     * More efficient than counting each state separately
     * @return map where Key=WarRecruitState and Value=Count of entries with that state
     */
    public Map<WarRecruitState, Integer> getCounts() {
        final Map<WarRecruitState, Integer> counts = new EnumMap<>(WarRecruitState.class);
        for(WarRecruitEntry entry : invitedPlayers.values()) {
            if(counts.containsKey(entry.getState())) {
                counts.put(entry.getState(), counts.get(entry.getState()) + 1);
            } else {
                counts.put(entry.getState(), 1);
            }
        }
        return counts;
    }

    //// GETTERS AND SETTERS ////

    public Map<UUID, WarRecruitEntry> getInvitedPlayers() {
        return invitedPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    //// NBT ////

    private static final String KEY_PLAYER_MAP = "Players";
    private static final String KEY_ID = "ID";
    private static final String KEY_ENTRY = "Entry";
    private static final String KEY_MAX_PLAYERS = "MaxPlayers";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        // write map
        ListTag listTag = new ListTag();
        for(Map.Entry<UUID, WarRecruitEntry> entry : invitedPlayers.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_ID, entry.getKey());
            entryTag.put(KEY_ENTRY, entry.getValue().serializeNBT());
            listTag.add(entryTag);
        }
        tag.put(KEY_PLAYER_MAP, listTag);
        tag.putInt(KEY_MAX_PLAYERS, maxPlayers);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.invitedPlayers.clear();
        // read map
        ListTag listTag = tag.getList(KEY_PLAYER_MAP, Tag.TAG_COMPOUND);
        for(int i = 0, n = listTag.size(); i < n; i++) {
            CompoundTag entryTag = listTag.getCompound(i);
            UUID playerId = entryTag.getUUID(KEY_ID);
            WarRecruitEntry entry = new WarRecruitEntry(entryTag.getCompound(KEY_ENTRY));
            invitedPlayers.put(playerId, entry);
        }
        this.maxPlayers = tag.getInt(KEY_MAX_PLAYERS);
    }


}
