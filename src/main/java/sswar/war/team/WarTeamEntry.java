package sswar.war.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class WarTeamEntry implements INBTSerializable<CompoundTag> {

    private int deathCount;
    private boolean forfeit;
    private boolean rewarded;

    //// CONSTRUCTORS ////

    public WarTeamEntry() {
        deathCount = 0;
        forfeit = false;
        rewarded = false;
    }

    public WarTeamEntry(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// METHODS ////

    /**
     * Increments the number of deaths for this player
     */
    public void addDeath() {
        deathCount += 1;
    }

    //// GETTERS AND SETTERS ////

    /**
     * @return the number of deaths for this player
     */
    public int getDeathCount() {
        return deathCount;
    }

    /**
     * @param deathCount the number of deaths for this player
     */
    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    /**
     * @return true if this player forfeits
     */
    public boolean isForfeit() {
        return forfeit;
    }

    /**
     * @param forfeit true if this player forfeits
     */
    public void setForfeit(boolean forfeit) {
        this.forfeit = forfeit;
    }

    /**
     * @return true if this player received their reward
     */
    public boolean isRewarded() {
        return rewarded;
    }

    /**
     * @param rewarded true if this player received their reward
     */
    public void setRewarded(boolean rewarded) {
        this.rewarded = rewarded;
    }

    //// NBT ////

    private static final String KEY_DEATH_COUNT = "Deaths";
    private static final String KEY_FORFEIT = "Forfeit";
    private static final String KEY_REWARDED = "Rewarded";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_DEATH_COUNT, deathCount);
        tag.putBoolean(KEY_FORFEIT, forfeit);
        tag.putBoolean(KEY_REWARDED, rewarded);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        deathCount = tag.getInt(KEY_DEATH_COUNT);
        forfeit = tag.getBoolean(KEY_FORFEIT);
        rewarded = tag.getBoolean(KEY_REWARDED);
    }
}
