package sswar.war.recruit;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Objects;

public class WarRecruitEntry implements INBTSerializable<CompoundTag> {

    private long timestamp;
    private WarRecruitState state;

    //// CONSTRUCTORS ////

    public WarRecruitEntry(final long timestamp) {
        this.state = WarRecruitState.PENDING;
        this.timestamp = timestamp;
    }

    public WarRecruitEntry(final CompoundTag tag) {
        deserializeNBT(tag);
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

    //// NBT ////

    private static final String KEY_TIMESTAMP = "Timestamp";
    private static final String KEY_STATE = "State";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(KEY_TIMESTAMP, timestamp);
        tag.putByte(KEY_STATE, state.getId());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        timestamp = tag.getLong(KEY_TIMESTAMP);
        state = WarRecruitState.getById(tag.getByte(KEY_STATE));
    }

    //// EQUALITY ////

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarRecruitEntry)) return false;
        WarRecruitEntry that = (WarRecruitEntry) o;
        return timestamp == that.timestamp && state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, state);
    }
}
