package sswar.war.team;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

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

    /**
     * Removes players that do not match the filter
     * @param filter a predicate for players to keep
     */
    public void filterTeams(final Predicate<UUID> filter) {
        final Set<UUID> toRemove = new HashSet<>();
        // determine team A members to remove
        for(UUID uuid : teamA.getTeam().keySet()) {
            if(!filter.test(uuid)) {
                toRemove.add(uuid);
            }
        }
        // remove from team A
        toRemove.forEach(uuid -> teamA.getTeam().remove(uuid));
        // determine team B members to remove
        toRemove.clear();
        for(UUID uuid : teamB.getTeam().keySet()) {
            if(!filter.test(uuid)) {
                toRemove.add(uuid);
            }
        }
        // remove from team B
        toRemove.forEach(uuid -> teamB.getTeam().remove(uuid));
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
