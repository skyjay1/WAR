package sswar.war.recruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import sswar.SSWar;

import java.util.Objects;

public class WarRecruitEntry implements INBTSerializable<CompoundTag> {

    public static final WarRecruitEntry EMPTY = new WarRecruitEntry(0);

    private long timestamp;
    private WarRecruitState state;
    private boolean canChange;

    //// CONSTRUCTORS ////

    public WarRecruitEntry(final long timestamp) {
        this.state = WarRecruitState.PENDING;
        this.timestamp = timestamp;
    }

    public WarRecruitEntry(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// HELPER METHODS ////

    public boolean isExpired(final long gameTime) {
        return gameTime - timestamp > SSWar.CONFIG.getRecruitTimeoutTicks();
    }

    //// GETTERS AND SETTERS ////

    public long getTimestamp() {
        return timestamp;
    }

    public WarRecruitState getState() {
        return state;
    }

    public void setState(WarRecruitState state) {
        this.state = state;
    }

    public boolean canChange() {
        return canChange;
    }

    public void setCanChange(boolean canChange) {
        this.canChange = canChange;
    }

    //// NBT ////

    private static final String KEY_TIMESTAMP = "Timestamp";
    private static final String KEY_STATE = "State";
    private static final String KEY_CAN_CHANGE = "CanChange";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(KEY_TIMESTAMP, timestamp);
        tag.putByte(KEY_STATE, state.getId());
        tag.putBoolean(KEY_CAN_CHANGE, canChange);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        timestamp = tag.getLong(KEY_TIMESTAMP);
        state = WarRecruitState.getById(tag.getByte(KEY_STATE));
        canChange = tag.getBoolean(KEY_CAN_CHANGE);
    }

    //// EQUALITY ////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarRecruitEntry)) return false;
        WarRecruitEntry that = (WarRecruitEntry) o;
        return timestamp == that.timestamp && state == that.state && canChange == that.canChange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, state, canChange);
    }
}
