package sswar.war.team;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class WarTeams implements Iterable<WarTeam>, INBTSerializable<CompoundTag> {

    private WarTeam teamA;
    private WarTeam teamB;

    private final List<WarTeam> teamList = new ArrayList<>();

    //// CONSTRUCTORS ////

    public WarTeams(final WarTeam teamA, final WarTeam teamB) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.teamList.add(teamA);
        this.teamList.add(teamB);
    }

    public WarTeams(final CompoundTag tag) {
        deserializeNBT(tag);
    }

    //// HELPER METHODS ////

    /**
     * @param player the player UUID
     * @return the team and entry for the given player, or empty if they are not in this war
     */
    public Optional<Pair<WarTeam, WarTeamEntry>> getTeamForPlayer(final UUID player) {
        // locate on team A
        WarTeamEntry entry = teamA.getTeam().get(player);
        if(entry != null) {
            return Optional.of(new Pair<>(teamA, entry));
        }
        // locate on team B
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
        teamList.clear();
        teamList.add(teamA);
        teamList.add(teamB);
    }

    @NotNull
    @Override
    public Iterator<WarTeam> iterator() {
        return teamList.iterator();
    }
}
