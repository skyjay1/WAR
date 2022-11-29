package sswar.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import sswar.SSWar;
import sswar.war.team.WarTeams;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TeamSavedData extends SavedData {

    private static final String KEY = SSWar.MODID + "_teams";

    private Map<UUID, WarTeams> teams = new HashMap<>();

    /**
     * Gets or creates the saved data, shared across all levels
     * @param server the server
     * @return the saved data
     */
    public static TeamSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(TeamSavedData::read, TeamSavedData::new, KEY);
    }

    //// HELPER METHODS ////


    //// GETTERS AND SETTERS ////

    public Map<UUID, WarTeams> getTeams() {
        return teams;
    }

    public Optional<WarTeams> getTeams(final UUID warId) {
        return Optional.ofNullable(teams.get(warId));
    }

    //// NBT ////

    private static final String KEY_TEAM_MAP = "TeamMap";
    private static final String KEY_ID = "War";
    private static final String KEY_TEAMS = "Teams";

    /**
     * Loads the saved data from NBT
     * @param nbt the NBT tag
     * @return the loaded saved data
     */
    public static TeamSavedData read(CompoundTag nbt) {
        TeamSavedData instance = new TeamSavedData();
        instance.load(nbt);
        return instance;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // write map
        ListTag listTag = new ListTag();
        for(Map.Entry<UUID, WarTeams> entry : teams.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(KEY_ID, entry.getKey());
            entryTag.put(KEY_TEAMS, entry.getValue().serializeNBT());
            listTag.add(entryTag);
        }
        tag.put(KEY_TEAM_MAP, listTag);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.teams.clear();
        // read map
        ListTag listTag = tag.getList(KEY_TEAM_MAP, Tag.TAG_COMPOUND);
        for(int i = 0, n = listTag.size(); i < n; i++) {
            CompoundTag entryTag = listTag.getCompound(i);
            UUID warId = entryTag.getUUID(KEY_ID);
            WarTeams entry = new WarTeams(entryTag.getCompound(KEY_TEAMS));
            teams.put(warId, entry);
        }
    }
}
