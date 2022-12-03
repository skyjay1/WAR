package sswar.data;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import sswar.SSWar;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;
import sswar.war.recruit.WarRecruitEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarSavedData extends SavedData {

    private static final String KEY = SSWar.MODID + "_wars";

    private Map<UUID, War> wars = new HashMap<>();
    private Map<UUID, WarRecruit> recruits = new HashMap<>();

    @Nullable
    private UUID periodicWarId;
    private long periodicWarTimestamp;

    //// CONSTRUCTORS ////

    /**
     * Gets or creates the saved data, shared across all levels
     * @param server the server
     * @return the saved data
     */
    public static WarSavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(WarSavedData::read, WarSavedData::new, KEY);
    }

    /**
     * Loads the saved data from NBT
     * @param nbt the NBT tag
     * @return the loaded saved data
     */
    public static WarSavedData read(CompoundTag nbt) {
        WarSavedData instance = new WarSavedData();
        instance.load(nbt);
        return instance;
    }

    //// HELPER METHODS ////

    public boolean hasPeriodicWar() {
        return periodicWarId != null;
    }

    private void loadWar(final UUID warId, final War war) {
        wars.put(warId, war);
    }

    private void loadRecruit(final UUID warId, final WarRecruit warRecruit) {
        recruits.put(warId, warRecruit);
    }

    public Pair<UUID, War> createWar(@Nullable final UUID owner, final String name, final long timestamp, final boolean noPrepPeriod, final int maxPlayers) {
        final UUID warId = getNextUUID(this);
        War war = new War(owner, name, noPrepPeriod, timestamp);
        WarRecruit recruit = new WarRecruit(maxPlayers);
        // add war and recruit to maps
        wars.put(warId, war);
        recruits.put(warId, recruit);
        // save data
        setDirty();
        return new Pair<>(warId, war);
    }

    /**
     * @param playerId the player ID
     * @return the war ID that contains a pending recruit for this player, if any
     */
    public Optional<Pair<UUID, WarRecruitEntry>> getRecruitForPlayer(final UUID playerId) {
        for(Map.Entry<UUID, WarRecruit> entry : recruits.entrySet()) {
            Optional<WarRecruitEntry> oEntry = entry.getValue().getEntry(playerId);
            if(oEntry.isPresent()) {
                return Optional.of(new Pair<>(entry.getKey(), oEntry.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * Removes a war and its corresponding war recruit data
     * @param warId the war ID
     */
    public void removeWar(final UUID warId) {
        wars.remove(warId);
        recruits.remove(warId);
        setDirty();
    }

    /**
     * Removes a war recruit instance
     * @param warId the war ID
     */
    public void removeWarRecruit(final UUID warId) {
        recruits.remove(warId);
        setDirty();
    }

    /**
     * Indicates the given war should be removed during the next update
     * @param warId the war ID
     */
    public void invalidateWar(final UUID warId) {
        War war = wars.get(warId);
        if(war != null) {
            war.setState(WarState.INVALID);
            setDirty();
        }
    }

    /**
     * @param data the saved data
     * @return a random UUID that is not currently in use
     */
    private static UUID getNextUUID(final WarSavedData data) {
        UUID uuid;
        do {
            uuid = UUID.randomUUID();
        } while(data.getWars().containsKey(uuid));
        return uuid;
    }

    //// GETTERS AND SETTERS ////

    /**
     * @return map where Key=WarID, Value=War
     */
    public Map<UUID, War> getWars() {
        return wars;
    }

    /**
     * @return map where Key=WarID, Value=WarRecruit
     */
    public Map<UUID, WarRecruit> getRecruits() {
        return recruits;
    }

    /**
     * @param warId the war ID
     * @return the war if it exists, or empty
     */
    public Optional<War> getWar(final UUID warId) {
        return Optional.ofNullable(wars.get(warId));
    }

    /**
     * @param warId the war ID
     * @return the war recruit instance if it exists, or empty
     */
    public Optional<WarRecruit> getRecruit(final UUID warId) {
        return Optional.ofNullable(recruits.get(warId));
    }

    /**
     * @return the war ID of the active periodic war, if it exists
     */
    @Nullable
    public UUID getPeriodicWarId() {
        return periodicWarId;
    }

    /**
     * Updates the periodic war and timestamp
     * @param periodicWarId the periodic war ID
     * @param timestamp the game time
     */
    public void setPeriodicWarId(final UUID periodicWarId, final long timestamp) {
        this.periodicWarId = periodicWarId;
        this.periodicWarTimestamp = timestamp;
        setDirty();
    }

    /**
     * Resets information about the periodic war
     */
    public void clearPeriodicWar() {
        this.periodicWarId = null;
        setDirty();
    }

    /**
     * @return the game time of the end of the most recent periodic war
     */
    public long getPeriodicWarTimestamp() {
        return periodicWarTimestamp;
    }

    //// NBT ////

    private static final String KEY_WAR_MAP = "WarMap";
    private static final String KEY_ID = "ID";
    private static final String KEY_WAR = "War";
    private static final String KEY_RECRUIT = "Recruit";
    private static final String KEY_PERIODIC_WAR_ID = "PeriodicWar";
    private static final String KEY_PERIODIC_WAR_TIMESTAMP = "PeriodicWarTimestamp";

    @Override
    public CompoundTag save(CompoundTag tag) {
        // write map
        ListTag listTag = new ListTag();
        for(Map.Entry<UUID, War> entry : wars.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            // write war
            entryTag.putUUID(KEY_ID, entry.getKey());
            entryTag.put(KEY_WAR, entry.getValue().serializeNBT());
            // write recruit, if any
            WarRecruit recruit = recruits.get(entry.getKey());
            if(recruit != null) {
                entryTag.put(KEY_RECRUIT, recruit.serializeNBT());
            }
            // add to list
            listTag.add(entryTag);
        }
        tag.put(KEY_WAR_MAP, listTag);
        tag.putLong(KEY_PERIODIC_WAR_TIMESTAMP, periodicWarTimestamp);
        if(periodicWarId != null) {
            tag.putUUID(KEY_PERIODIC_WAR_ID, periodicWarId);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        this.wars.clear();
        // read map
        ListTag listTag = tag.getList(KEY_WAR_MAP, Tag.TAG_COMPOUND);
        for(int i = 0, n = listTag.size(); i < n; i++) {
            CompoundTag entryTag = listTag.getCompound(i);
            // read war
            UUID warId = entryTag.getUUID(KEY_ID);
            War war = new War(entryTag.getCompound(KEY_WAR));
            loadWar(warId, war);
            // read recruit, if any
            if(entryTag.contains(KEY_RECRUIT)) {
                WarRecruit recruit = new WarRecruit(entryTag.getCompound(KEY_RECRUIT));
                loadRecruit(warId, recruit);
            }
        }
        periodicWarTimestamp = tag.getLong(KEY_PERIODIC_WAR_TIMESTAMP);
        if(tag.contains(KEY_PERIODIC_WAR_ID)) {
            periodicWarId = tag.getUUID(KEY_PERIODIC_WAR_ID);
        }
    }
}
