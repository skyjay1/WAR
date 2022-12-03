package sswar.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

    ListTag getCuriosTag();

    void setCuriosTag(final ListTag tag);

    ListTag getBlueSkiesTag();

    void setBlueSkiesTag(final ListTag tag);

    CompoundTag getCosmeticArmorTag();

    void setCosmeticArmorTag(final CompoundTag tag);

    ListTag getInventoryTag();

    void setInventoryTag(final ListTag tag);

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

    default boolean hasCuriosTag() {
        return getCuriosTag() != null;
    }

    default boolean hasInventoryTag() {
        return getInventoryTag() != null;
    }

    default boolean hasCosmeticArmorTag() {
        return getCosmeticArmorTag() != null;
    }

    default boolean hasBlueSkiesTag() {
        return getBlueSkiesTag() != null;
    }

    //// NBT ////

    static final String KEY_WAR = "War";
    static final String KEY_WINS = "Wins";
    static final String KEY_LOSSES = "Losses";
    static final String KEY_WAR_ENDED = "WarEnded";
    static final String KEY_CURIOS = "Curios";
    static final String KEY_BLUE_SKIES = "BlueSkies";
    static final String KEY_COSMETIC_ARMOR = "CosmeticArmor";
    static final String KEY_INVENTORY = "Inventory";

    @Override
    default CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        if(hasActiveWar()) {
            tag.putUUID(KEY_WAR, getActiveWar());
        }
        tag.putInt(KEY_WINS, getWins());
        tag.putInt(KEY_LOSSES, getLosses());
        tag.putLong(KEY_WAR_ENDED, getWarEndedTimestamp());
        if(hasCuriosTag()) {
            tag.put(KEY_CURIOS, getCuriosTag());
        }
        if(hasBlueSkiesTag()) {
            tag.put(KEY_BLUE_SKIES, getBlueSkiesTag());
        }
        if(hasCosmeticArmorTag()) {
            tag.put(KEY_COSMETIC_ARMOR, getCosmeticArmorTag());
        }
        if(hasInventoryTag()) {
            tag.put(KEY_INVENTORY, getInventoryTag());
        }
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
        if(tag.contains(KEY_CURIOS, Tag.TAG_LIST)) {
            setCuriosTag((ListTag) tag.get(KEY_CURIOS));
        }
        if(tag.contains(KEY_BLUE_SKIES, Tag.TAG_LIST)) {
            setBlueSkiesTag((ListTag) tag.get(KEY_BLUE_SKIES));
        }
        if(tag.contains(KEY_COSMETIC_ARMOR, Tag.TAG_COMPOUND)) {
            setCosmeticArmorTag(tag.getCompound(KEY_COSMETIC_ARMOR));
        }
        if(tag.contains(KEY_INVENTORY, Tag.TAG_LIST)) {
            setInventoryTag((ListTag) tag.get(KEY_INVENTORY));
        }
    }
}
