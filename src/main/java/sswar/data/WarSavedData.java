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
    private UUID randomWarId;
    private long randomWarTimestamp;

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

    public boolean hasRandomWar() {
        return randomWarId != null;
    }

    private void loadWar(final UUID warId, final War war) {
        wars.put(warId, war);
    }

    private void loadRecruit(final UUID warId, final WarRecruit warRecruit) {
        recruits.put(warId, warRecruit);
    }

    public Pair<UUID, War> createWar(@Nullable final UUID owner, final String name, final long timestamp, final int maxPlayers) {
        final UUID warId = getNextUUID(this);
        War war = new War(owner, name, timestamp);
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
    public Optional<UUID> getPendingRecruitForPlayer(final UUID playerId) {
        for(Map.Entry<UUID, WarRecruit> entry : recruits.entrySet()) {
            if(entry.getValue().getEntry(playerId).isPresent()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void removeWar(final UUID warId) {
        wars.remove(warId);
        recruits.remove(warId);
        setDirty();
    }

    public void removeWarRecruit(final UUID warId) {
        recruits.remove(warId);
        setDirty();
    }

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

    public Map<UUID, War> getWars() {
        return wars;
    }

    public Map<UUID, WarRecruit> getRecruits() {
        return recruits;
    }

    public Optional<War> getWar(final UUID warId) {
        return Optional.ofNullable(wars.get(warId));
    }

    public Optional<WarRecruit> getRecruit(final UUID warId) {
        return Optional.ofNullable(recruits.get(warId));
    }

    @Nullable
    public UUID getRandomWarId() {
        return randomWarId;
    }

    public void setRandomWarId(final UUID randomWarId, final long timestamp) {
        this.randomWarId = randomWarId;
        this.randomWarTimestamp = timestamp;
        setDirty();
    }

    public void clearRandomWar() {
        this.randomWarId = null;
        setDirty();
    }

    public long getRandomWarTimestamp() {
        return randomWarTimestamp;
    }


    //// NBT ////

    private static final String KEY_WAR_MAP = "WarMap";
    private static final String KEY_ID = "ID";
    private static final String KEY_WAR = "War";
    private static final String KEY_RECRUIT = "Recruit";
    private static final String KEY_RANDOM_WAR_ID = "RandomWar";
    private static final String KEY_RANDOM_WAR_TIMESTAMP = "RandomWarTimestamp";

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
        tag.putLong(KEY_RANDOM_WAR_TIMESTAMP, randomWarTimestamp);
        if(randomWarId != null) {
            tag.putUUID(KEY_RANDOM_WAR_ID, randomWarId);
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
        randomWarTimestamp = tag.getLong(KEY_RANDOM_WAR_TIMESTAMP);
        if(tag.contains(KEY_RANDOM_WAR_ID)) {
            randomWarId = tag.getUUID(KEY_RANDOM_WAR_ID);
        }
    }
}
