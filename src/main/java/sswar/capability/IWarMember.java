package sswar.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IWarMember extends INBTSerializable<CompoundTag> {

    /**
     * @return the ID of the war this user is currently in, or null if they are not in a war
     */
    @Nullable
    UUID getActiveWar();

    /**
     * @param id the ID of the war this user is currently in, or null if they are not in a war
     */
    void setActiveWar(@Nullable final UUID id);

    /**
     * @return the timestamp of the most recent war, or 0 if there is none
     */
    long getWarEndedTimestamp();

    /**
     * @param timestamp the timestamp of the most recent war
     */
    void setWarEndedTimestamp(final long timestamp);

    /**
     * @return the total number of wars won
     */
    int getWins();

    /**
     * @param wins the total number of wars won
     */
    void setWins(final int wins);

    /**
     * @return the total number of wars lost
     */
    int getLosses();

    /**
     * @param losses the total number of wars lost
     */
    void setLosses(final int losses);

    //// HELPER METHODS ////

    default void addWin() {
        setWins(getWins() + 1);
    }

    default void addLoss() {
        setLosses(getLosses() + 1);
    }

    default boolean hasActiveWar() {
        return getActiveWar() != null;
    }

    default void clearActiveWar() {
        setActiveWar(null);
    }

    //// NBT ////

    static final String KEY_WAR = "War";
    static final String KEY_WINS = "Wins";
    static final String KEY_LOSSES = "Losses";
    static final String KEY_WAR_ENDED = "WarEnded";

    @Override
    default CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        UUID warId = getActiveWar();
        if(warId != null) {
            tag.putUUID(KEY_WAR, warId);
        }
        tag.putInt(KEY_WINS, getWins());
        tag.putInt(KEY_LOSSES, getLosses());
        tag.putLong(KEY_WAR_ENDED, getWarEndedTimestamp());
        return tag;
    }

    @Override
    default void deserializeNBT(final CompoundTag tag) {
        if(tag.contains(KEY_WAR)) {
            setActiveWar(tag.getUUID(KEY_WAR));
        }
        setWins(tag.getInt(KEY_WAR));
        setLosses(tag.getInt(KEY_LOSSES));
        setWarEndedTimestamp(tag.getLong(KEY_WAR_ENDED));
    }
}
