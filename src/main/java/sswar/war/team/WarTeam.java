package sswar.war.team;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import sswar.WarUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarTeam implements Iterable<UUID>, INBTSerializable<CompoundTag> {

    private String name;
    private Map<UUID, WarTeamEntry> team = new HashMap<>();
    private boolean win;
    private boolean rewarded;
    private boolean hasReward;

    public WarTeam(final String name, final Collection<UUID> players) {
        this.name = name;
        for(UUID uuid : players) {
            team.put(uuid, new WarTeamEntry());
        }
        this.win = false;
        this.rewarded = false;
        this.hasReward = false;
    }

    public WarTeam(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// METHODS ////

    public int countPlayersWithDeaths() {
        int count = 0;
        for(WarTeamEntry entry : team.values()) {
            if(entry.getDeathCount() > 0) {
                count++;
            }
        }
        return count;
    }

    public int countForfeits() {
        int count = 0;
        for(WarTeamEntry entry : team.values()) {
            if(entry.isForfeit()) {
                count++;
            }
        }
        return count;
    }

    /**
     * @param server the server
     * @return Map where Key=PlayerId and Value=Username
     */
    public Map<UUID, String> getPlayerNames(final MinecraftServer server) {
        Map<UUID, String> nameList = new HashMap<>();
        for(UUID uuid : team.keySet()) {
            nameList.put(uuid, WarUtils.getPlayerName(server, uuid));
        }
        return nameList;
    }

    //// GETTERS AND SETTERS ////

    public String getName() {
        return name;
    }

    public Map<UUID, WarTeamEntry> getTeam() {
        return team;
    }

    public Optional<WarTeamEntry> getEntry(final UUID player) {
        return Optional.ofNullable(team.get(player));
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win, boolean hasReward) {
        this.win = win;
        this.hasReward = hasReward;
    }

    public boolean isRewarded() {
        return rewarded;
    }

    public void setRewarded(boolean rewarded) {
        this.rewarded = rewarded;
    }

    public boolean hasReward() {
        return hasReward;
    }

    //// NBT ////

    private static final String KEY_NAME = "Name";
    private static final String KEY_TEAM_MAP = "Team";
    private static final String KEY_ID = "ID";
    private static final String KEY_ENTRY = "Entry";
    private static final String KEY_WIN = "Win";
    private static final String KEY_HAS_REWARD = "HasReward";
    private static final String KEY_REWARDED = "Rewarded";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_NAME, name);
        tag.putBoolean(KEY_WIN, win);
        tag.putBoolean(KEY_HAS_REWARD, hasReward);
        tag.putBoolean(KEY_REWARDED, rewarded);
        // write map
        ListTag listTag = new ListTag();
        for(Map.Entry<UUID, WarTeamEntry> entry : team.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_ID, entry.getKey());
            entryTag.put(KEY_ENTRY, entry.getValue().serializeNBT());
            listTag.add(entryTag);
        }
        tag.put(KEY_TEAM_MAP, listTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.name = tag.getString(KEY_NAME);
        this.win = tag.getBoolean(KEY_WIN);
        this.hasReward = tag.getBoolean(KEY_HAS_REWARD);
        this.rewarded = tag.getBoolean(KEY_REWARDED);
        this.team.clear();
        // read map
        ListTag listTag = tag.getList(KEY_TEAM_MAP, Tag.TAG_COMPOUND);
        for(int i = 0, n = listTag.size(); i < n; i++) {
            CompoundTag entryTag = listTag.getCompound(i);
            UUID playerId = entryTag.getUUID(KEY_ID);
            WarTeamEntry entry = new WarTeamEntry(entryTag.getCompound(KEY_ENTRY));
            team.put(playerId, entry);
        }
    }

    @NotNull
    @Override
    public Iterator<UUID> iterator() {
        return team.keySet().iterator();
    }
}
