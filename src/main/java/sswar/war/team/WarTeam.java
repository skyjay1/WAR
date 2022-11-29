package sswar.war.team;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.INBTSerializable;
import sswar.WarUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarTeam implements INBTSerializable<CompoundTag> {

    private String name;
    private Map<UUID, WarTeamEntry> team = new HashMap<>();

    public WarTeam(final String name, final Collection<UUID> players) {
        this.name = name;
        for(UUID uuid : players) {
            team.put(uuid, new WarTeamEntry());
        }
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

    public List<String> getSortedPlayerNames(final MinecraftServer server) {
        List<String> nameList = new ArrayList<>();
        for(UUID uuid : team.keySet()) {
            nameList.add(WarUtils.getPlayerName(server, uuid));
        }
        nameList.sort(String::compareToIgnoreCase);
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

    //// NBT ////

    private static final String KEY_NAME = "Name";
    private static final String KEY_TEAM_MAP = "Team";
    private static final String KEY_ID = "ID";
    private static final String KEY_ENTRY = "Entry";


    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_NAME, name);
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
}
