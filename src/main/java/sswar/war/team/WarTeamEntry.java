package sswar.war.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class WarTeamEntry implements INBTSerializable<CompoundTag> {

    private int deathCount;
    private boolean forfeit;

    //// CONSTRUCTORS ////

    public WarTeamEntry() {
        deathCount = 0;
        forfeit = false;
    }

    public WarTeamEntry(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// METHODS ////

    public void addDeath() {
        deathCount += 1;
    }

    //// GETTERS AND SETTERS ////

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    public boolean isForfeit() {
        return forfeit;
    }

    public void setForfeit(boolean forfeit) {
        this.forfeit = forfeit;
    }

    //// NBT ////

    private static final String KEY_DEATH_COUNT = "Deaths";
    private static final String KEY_FORFEIT = "Forfeit";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_DEATH_COUNT, deathCount);
        tag.putBoolean(KEY_FORFEIT, forfeit);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        deathCount = tag.getInt(KEY_DEATH_COUNT);
        forfeit = tag.getBoolean(KEY_FORFEIT);
    }
}
