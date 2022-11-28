package sswar.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import sswar.SSWar;

import java.util.UUID;

public class WarMember implements IWarMember {

    public static final ResourceLocation ID = new ResourceLocation(SSWar.MODID, "war_member");
    public static final WarMember EMPTY = new WarMember();

    private UUID activeWar;
    private long warEndedTimestamp;
    private int wins;
    private int losses;

    public WarMember() {
    }

    @Nullable
    @Override
    public UUID getActiveWar() {
        return activeWar;
    }

    @Override
    public void setActiveWar(@Nullable UUID id) {
        this.activeWar = id;
    }

    @Override
    public long getWarEndedTimestamp() {
        return warEndedTimestamp;
    }

    @Override
    public void setWarEndedTimestamp(long timestamp) {
        this.warEndedTimestamp = timestamp;
    }

    @Override
    public int getWins() {
        return wins;
    }

    @Override
    public void setWins(int wins) {
        this.wins = wins;
    }

    @Override
    public int getLosses() {
        return losses;
    }

    @Override
    public void setLosses(int losses) {
        this.losses = losses;
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final IWarMember instance;
        private final LazyOptional<IWarMember> storage;

        public Provider() {
            instance = new WarMember();
            storage = LazyOptional.of(() -> instance);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if(cap == SSWar.WAR_MEMBER) {
                return storage.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            instance.deserializeNBT(tag);
        }
    }
}
