package sswar.war.team;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class WarTeams implements INBTSerializable<CompoundTag> {

    private WarTeam teamA;
    private WarTeam teamB;

    //// CONSTRUCTORS ////

    public WarTeams(final WarTeam teamA, final WarTeam teamB) {
        this.teamA = teamA;
        this.teamB = teamB;
    }

    public WarTeams(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// GETTERS ////

    public WarTeam getTeamA() {
        return teamA;
    }

    public WarTeam getTeamB() {
        return teamB;
    }

    //// NBT ////

    private static final String KEY_TEAM_A = "TeamA";
    private static final String KEY_TEAM_B = "TeamB";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put(KEY_TEAM_A, teamA.serializeNBT());
        tag.put(KEY_TEAM_B, teamB.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        teamA = new WarTeam(tag.getCompound(KEY_TEAM_A));
        teamB = new WarTeam(tag.getCompound(KEY_TEAM_B));
    }
}
