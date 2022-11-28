package sswar.war.recruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

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
     * Updates the recruit entry for the given player to ACCEPT
     * @param player the player
     * @return true if there is an entry for this player
     */
    public boolean accept(final UUID player) {
        WarRecruitEntry entry = invitedPlayers.get(player);
        if(entry != null) {
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
    public boolean reject(final UUID player) {
        WarRecruitEntry entry = invitedPlayers.get(player);
        if(entry != null) {
            entry.setState(WarRecruitState.REJECT);
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

    //// GETTERS AND SETTERS ////

    public Map<UUID, WarRecruitEntry> getInvitedPlayers() {
        return invitedPlayers;
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
        return null;
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
