package sswar.war;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class War implements INBTSerializable<CompoundTag> {

    @Nullable
    private UUID owner;
    private String name;
    private WarState state;
    private long createdTimestamp;
    private LocalDateTime createdDate;
    private long prepareTimestamp;
    private long activateTimestamp;
    private long endTimestamp;

    //// CONSTRUCTORS ////

    public War(final String name, final long createdTimestamp) {
        this(null, name, createdTimestamp);
    }

    public War(@Nullable UUID owner, String name, long createdTimestamp) {
        this.owner = owner;
        this.name = name;
        this.state = WarState.CREATING;
        this.createdTimestamp = createdTimestamp;
        this.createdDate = LocalDateTime.now();
    }

    public War(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// HELPER METHODS ////

    public boolean hasOwner() {
        return owner != null;
    }

    //// GETTERS AND SETTERS ////

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public WarState getState() {
        return state;
    }

    public void setState(WarState state) {
        this.state = state;
    }

    public long getPrepareTimestamp() {
        return prepareTimestamp;
    }

    public void setPrepareTimestamp(long prepareTimestamp) {
        this.prepareTimestamp = prepareTimestamp;
    }

    public long getActivateTimestamp() {
        return activateTimestamp;
    }

    public void setActivateTimestamp(long activateTimestamp) {
        this.activateTimestamp = activateTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    //// NBT ////

    private static final String KEY_OWNER = "Owner";
    private static final String KEY_NAME = "Name";
    private static final String KEY_STATE = "State";
    private static final String KEY_CREATE = "Create";
    private static final String KEY_CREATE_DATE = "CreateDate";
    private static final String KEY_PREPARE = "Prepare";
    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_END = "End";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if(hasOwner()) {
            tag.putUUID(KEY_OWNER, owner);
        }
        tag.putString(KEY_NAME, name);
        tag.putByte(KEY_STATE, state.getId());
        tag.putLong(KEY_CREATE, createdTimestamp);
        tag.putLong(KEY_PREPARE, prepareTimestamp);
        tag.putLong(KEY_ACTIVE, activateTimestamp);
        tag.putLong(KEY_END, endTimestamp);
        tag.putString(KEY_CREATE_DATE, DATE_TIME_FORMATTER.format(createdDate));
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if(tag.contains(KEY_OWNER)) {
            owner = tag.getUUID(KEY_OWNER);
        }
        name = tag.getString(KEY_NAME);
        state = WarState.getById(tag.getByte(KEY_STATE));
        createdTimestamp = tag.getLong(KEY_CREATE);
        createdDate = LocalDateTime.parse(tag.getString(KEY_CREATE_DATE), DATE_TIME_FORMATTER);
        prepareTimestamp = tag.getLong(KEY_PREPARE);
        activateTimestamp = tag.getLong(KEY_ACTIVE);
        endTimestamp = tag.getLong(KEY_END);
    }
}
