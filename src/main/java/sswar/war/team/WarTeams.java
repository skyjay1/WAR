package sswar.war.team;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Optional;
import java.util.UUID;

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

    //// HELPER METHODS ////

    /**
     * @param player the player UUID
     * @return the team for the given player, or empty if they are not in this war
     */
    public Optional<WarTeam> getTeamForPlayer(final UUID player) {
        if(teamA.getTeam().containsKey(player)) {
            return Optional.of(teamA);
        }
        if(teamB.getTeam().containsKey(player)) {
            return Optional.of(teamB);
        }
        return Optional.empty();
    }

    /**
     * @param player the player UUID
     * @return the team and entry for the given player, or empty if they are not in this war
     */
    public Optional<Pair<WarTeam, WarTeamEntry>> getTeamAndEntryForPlayer(final UUID player) {
        WarTeamEntry entry = teamA.getTeam().get(player);
        if(entry != null) {
            return Optional.of(new Pair<>(teamA, entry));
        }
        entry = teamB.getTeam().get(player);
        if(entry != null) {
            return Optional.of(new Pair<>(teamB, entry));
        }
        return Optional.empty();
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
