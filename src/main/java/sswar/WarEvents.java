package sswar;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;
import sswar.data.TeamSavedData;
import sswar.data.WarSavedData;
import sswar.util.MessageUtils;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;
import sswar.war.recruit.WarRecruitEntry;
import sswar.war.recruit.WarRecruitState;
import sswar.war.team.WarTeam;
import sswar.war.team.WarTeamEntry;
import sswar.war.team.WarTeams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


public final class WarEvents {

    public static final class ModHandler {

        @SubscribeEvent
        public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(IWarMember.class);
        }
    }

    public static final class ForgeHandler {

        @SubscribeEvent
        public static void onRegisterCommands(final RegisterCommandsEvent event) {
            WarCommands.register(event);
        }

        @SubscribeEvent
        public static void onAttachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
            if(event.getObject() instanceof Player) {
                event.addCapability(WarMember.ID, new WarMember.Provider());
            }
        }

        /**
         * Used to ensure that capabilities persist across deaths
         * @param event the player clone event
         */
        @SubscribeEvent
        public static void onPlayerClone(final PlayerEvent.Clone event) {
            // revive capabilities in order to copy to the clone
            event.getOriginal().reviveCaps();
            LazyOptional<IWarMember> original = event.getOriginal().getCapability(SSWar.WAR_MEMBER);
            LazyOptional<IWarMember> copy = event.getEntity().getCapability(SSWar.WAR_MEMBER);
            if(original.isPresent() && copy.isPresent()) {
                copy.ifPresent(f -> f.deserializeNBT(original.orElse(WarMember.EMPTY).serializeNBT()));
            }
        }

        private static int ticker;

        @SubscribeEvent
        public static void onServerTick(final TickEvent.ServerTickEvent event) {
            if(event.phase != TickEvent.Phase.END) {
                return;
            }
            // update when server is not overloaded, at most every few ticks
            if(event.haveTime() || ticker++ > 5) {
                ticker = 0;
                final WarSavedData warData = WarSavedData.get(event.getServer());
                updateServerWars(event.getServer(), warData);
                updateServerWarRecruits(event.getServer(), warData);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
            if(event.getEntity().level.isClientSide() || null == event.getEntity().getServer()) {
                return;
            }
            // load war data
            final WarSavedData warData = WarSavedData.get(event.getEntity().getServer());
            // check for pending requests
            Optional<Pair<UUID, WarRecruitEntry>> oWarRecruit = warData.getRecruitForPlayer(event.getEntity().getUUID());
            if(oWarRecruit.isPresent() && oWarRecruit.get().getSecond().getState() == WarRecruitState.PENDING) {
                event.getEntity().displayClientMessage(WarUtils.createRecruitComponent(), false);
            }
        }

        /**
         * Updates death counts when a player dies during an active war
         * @param event the death event
         */
        @SubscribeEvent
        public static void onPlayerDeath(final LivingDeathEvent event) {
            if(event.getEntity().level.isClientSide()) {
                return;
            }
            // check if a player died
            if(event.getEntity() instanceof ServerPlayer player) {
                IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
                if(iWarMember.hasActiveWar()) {
                    // update death count
                    WarUtils.onPlayerDeath(player, iWarMember.getActiveWar());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if(event.player.level.isClientSide() || !(event.player instanceof ServerPlayer)) {
                return;
            }
            if(event.phase != TickEvent.Phase.END) {
                return;
            }
            if(event.player.tickCount < 2 || event.player.tickCount % 20 != 0) {
                return;
            }
            // load active war and check if war still exists
            Optional<IWarMember> oWarMember = event.player.getCapability(SSWar.WAR_MEMBER).resolve();
            oWarMember.ifPresent(c -> {
                if(c.hasActiveWar()) {
                    // load war data
                    final WarSavedData data = WarSavedData.get(event.player.getServer());
                    final Optional<War> oWar = data.getWar(c.getActiveWar());
                    // check for invalid war
                    if(oWar.isEmpty() || oWar.get().getState() == WarState.INVALID) {
                        c.clearActiveWar();
                    }
                }
            });
            // check inventory for war compass
            WarUtils.tryUpdateWarCompass((ServerPlayer) event.player);
        }

        /**
         * Prevents friendly fire while a war is active
         * @param event the living hurt event
         */
        @SubscribeEvent
        public static void onPlayerHurt(final LivingHurtEvent event) {
            if(event.getEntity().level.isClientSide()) {
                return;
            }
            if(!(event.getEntity() instanceof ServerPlayer)) {
                return;
            }
            if(!(event.getSource().getEntity() instanceof ServerPlayer)) {
                return;
            }
            final ServerPlayer player = (ServerPlayer) event.getEntity();
            final ServerPlayer source = (ServerPlayer) event.getSource().getEntity();
            // check if player is in a war
            IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
            if(!iWarMember.hasActiveWar()) {
                return;
            }
            // load team data
            final TeamSavedData teamData = TeamSavedData.get(player.getServer());
            Optional<WarTeams> oTeams = teamData.getTeams(iWarMember.getActiveWar());
            if(oTeams.isEmpty()) {
                return;
            }
            // check if players are not on the same team
            final Optional<Pair<WarTeam, WarTeamEntry>> oPlayerTeam = oTeams.get().getTeamForPlayer(player.getUUID());
            if(oPlayerTeam.isEmpty() || oPlayerTeam.get().getFirst().getEntry(source.getUUID()).isEmpty()) {
                return;
            }
            // all checks passed, cancel damage event
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onPlayerSleepInBed(final PlayerSleepInBedEvent event) {
            if(event.getEntity().level.isClientSide()) {
                return;
            }
            // check if player has active war
            IWarMember iWarMember = event.getEntity().getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
            if(iWarMember.hasActiveWar()) {
                // set result
                event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
                // send message
                event.getEntity().displayClientMessage(MessageUtils.component("block.minecraft.bed.active_war"), true);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
            if(event.getEntity().level.isClientSide() || !(event.getEntity() instanceof ServerPlayer)) {
                return;
            }
            // attempt to open compass menu
            if(WarUtils.isWarCompass(event.getItemStack()) && WarUtils.openWarCompassMenu((ServerPlayer) event.getEntity(), event.getItemStack())) {
                // consume the event
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Updates each war recruit in the server to expire pending requests
     * @param server the server
     * @param data the war data
     */
    private static void updateServerWarRecruits(final MinecraftServer server, final WarSavedData data) {
        for(Map.Entry<UUID, WarRecruit> entry : data.getRecruits().entrySet()) {
            updateWarRecruit(server, data, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Updates each request for the given war recruit instance to expire pending requests
     * @param server the server
     * @param data the war data
     * @param warId the war ID
     * @param recruit the war recruit instance
     */
    private static void updateWarRecruit(final MinecraftServer server, final WarSavedData data, final UUID warId, final WarRecruit recruit) {
        final long gameTime = server.overworld().getGameTime();
        for(Map.Entry<UUID, WarRecruitEntry> recruitEntry : recruit.getInvitedPlayers().entrySet()) {
            if(recruitEntry.getValue().getState() == WarRecruitState.PENDING && recruitEntry.getValue().isExpired(gameTime)) {
                recruitEntry.getValue().setState(WarRecruitState.INVALID);
                data.setDirty();
            }
        }
    }

    /**
     * Updates each war in the server, then removes any wars that are invalid
     * @param server the server
     * @param data the war data
     */
    private static void updateServerWars(final MinecraftServer server, final WarSavedData data) {
        // determine game time
        final long gameTime = server.getLevel(Level.OVERWORLD).getGameTime();
        // update or create random war
        if(data.hasPeriodicWar()) {
            Optional<War> randomWar = data.getWar(data.getPeriodicWarId());
            randomWar.ifPresent(war -> updateRandomWar(server, data, data.getPeriodicWarId(), war));
        } else if(server.getPlayerList().getPlayerCount() > 1 && (data.getPeriodicWarTimestamp() == 0 || gameTime - data.getPeriodicWarTimestamp() > SSWar.CONFIG.getRandomWarIntervalTicks())) {
            Optional<UUID> warId = WarUtils.tryCreateWar(null, WarUtils.WAR_NAME, WarUtils.TEAM_A_NAME, WarUtils.TEAM_B_NAME, List.of(), List.of(), WarUtils.MAX_PLAYER_COUNT, true);
            // update timestamp
            warId.ifPresent(uuid -> {
                data.setPeriodicWarId(uuid, gameTime);
            });
        }
        // create list of wars to remove
        final List<UUID> toRemove = new ArrayList<>();
        // update each war
        for(Map.Entry<UUID, War> entry : data.getWars().entrySet()) {
            updateWar(server, data, entry.getKey(), entry.getValue());
            // check if war should be removed
            if(entry.getValue().getState() == WarState.INVALID) {
                toRemove.add(entry.getKey());
            }
        }
        // remove invalid wars
        for(UUID warId : toRemove) {
            data.removeWar(warId);
            if(data.hasPeriodicWar() && warId.equals(data.getPeriodicWarId())) {
                data.setPeriodicWarId(null, gameTime);
            }
        }
    }

    /**
     * Checks if the server war event needs to invite another player
     * @param server the server
     * @param data the war data
     * @param warId the war ID
     * @param war the server war instance
     */
    private static void updateRandomWar(final MinecraftServer server, final WarSavedData data, final UUID warId, final War war) {
        if(war.getState() == WarState.RECRUITING) {
            // load the war recruit instance
            Optional<WarRecruit> oWarRecruit = data.getRecruit(warId);
            oWarRecruit.ifPresent(recruit -> {
                // check if there are no pending recruit requests
                if(!recruit.isFull() && recruit.getPendingCount() == 0) {
                    // find a player to recruit
                    Optional<ServerPlayer> oPlayer = WarUtils.findValidPlayer(server, recruit.getInvitedPlayers().keySet());
                    oPlayer.ifPresent(player -> {
                        // add a new pending recruit request
                        recruit.add(server, data, player.getUUID(), player.level.getGameTime());
                    });
                }
            });
        } else {
            // war is no longer recruiting, remove it from data
            data.clearPeriodicWar();
        }
    }

    private static void updateWar(final MinecraftServer server, final WarSavedData warData, final UUID warId, final War war) {
        final long gameTime = server.getLevel(Level.OVERWORLD).getGameTime();
        // load recruit instance
        final Optional<WarRecruit> oRecruit = warData.getRecruit(warId);
        // load war teams instance
        final TeamSavedData teamData = TeamSavedData.get(server);
        final Optional<WarTeams> oTeams = teamData.getTeams(warId);
        if(oTeams.isEmpty()) {
            warData.invalidateWar(warId);
            return;
        }
        WarTeams teams = oTeams.get();
        // update war based on current state
        final WarState state = war.getState();
        switch (state) {
            case CREATING:
                // check if created timestamp is more than 10 minutes
                // this will only happen if the server somehow failed to start recruiting
                if(gameTime - war.getCreatedTimestamp() > 12_000) {
                    warData.invalidateWar(warId);
                }
                break;
            case RECRUITING:
                // check for recruiting timer expire
                if(gameTime - war.getCreatedTimestamp() > SSWar.CONFIG.getRecruitDurationTicks()) {
                    if(oRecruit.isPresent()) {
                        final WarRecruit recruit = oRecruit.get();
                        // filter teams by players who accepted recruit invites
                        teams.filterTeams(uuid -> recruit.getEntry(uuid).orElse(WarRecruitEntry.EMPTY).getState().isAccepted());
                        // save changes
                        teamData.setDirty();
                        // ensure teams are balanced
                        if(WarUtils.tryBalanceTeams(server, teamData, teams.getTeamA(), teams.getTeamB())) {
                            // begin preparing
                            War.startPreparing(server, warData, warId, war, teams, gameTime);
                        } else {
                            // cancel recruiting
                            War.cancelRecruiting(server, warData, warId, war, teams, recruit);
                        }
                    }
                    // remove war recruit instance
                    warData.removeWarRecruit(warId);
                }
                break;
            case PREPARING:
                // check for preparation timer expire
                if(gameTime - war.getPrepareTimestamp() > SSWar.CONFIG.getPreparationDurationTicks()) {
                    // begin activate
                    War.startActivate(server, warData, warId, war, teams, gameTime);
                }
                // check forfeit conditions
                checkAndUpdateForfeit(server, warData, teamData, warId, war, teams, gameTime);
                break;
            case ACTIVE:
                // check all participating players have this war set as their active war
                checkAndUpdateActiveFlags(server, warData, teamData, warId, war, teams, gameTime);
                // check forfeit conditions
                checkAndUpdateForfeit(server, warData, teamData, warId, war, teams, gameTime);
                // check win conditions
                checkAndUpdateWin(server, warData, teamData, warId, war, teams, gameTime);
                break;
            case ENDED:
                // check all participating players have this war set as their active war
                checkAndUpdateActiveFlags(server, warData, teamData, warId, war, teams, gameTime);
                // load all participating players to update reward status
                updateReward(server, warData, teamData, warId, war, teams);
                break;
            case INVALID: default:
                break;
        }
    }

    /**
     * Checks and updates active war flags for all participating players
     * @param server the server
     * @param warData the war data
     * @param teamData the team data
     * @param warId the war ID
     * @param war the war instance
     * @param warTeams the war teams instance
     * @param timestamp the game time
     */
    private static void checkAndUpdateActiveFlags(final MinecraftServer server, final WarSavedData warData, final TeamSavedData teamData,
                                              final UUID warId, final War war, final WarTeams warTeams, final long timestamp) {
        // send messages to players in each team
        for(WarTeam team : warTeams) {
            for(UUID playerId : team) {
                // locate player if they are online
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if(player != null) {
                    // update war flags
                    player.getCapability(SSWar.WAR_MEMBER).ifPresent(c -> {
                        if(war.getState() == WarState.ACTIVE) {
                            c.setActiveWar(warId);
                        } else if(war.getState() == WarState.ENDED) {
                            c.setWarEndedTimestamp(war.getEndTimestamp());
                        }
                    });
                }
            }
        }
    }

    /**
     * Checks and updates forfeit status if more than half of one team forfeit.
     * The winning team will only be given a reward chest if at least one of the forfeiting team players died.
     * @param server the server
     * @param warData the war data
     * @param teamData the team data
     * @param warId the war ID
     * @param war the war instance
     * @param warTeams the war teams instance
     * @param timestamp the game time
     */
    private static void checkAndUpdateForfeit(final MinecraftServer server, final WarSavedData warData, final TeamSavedData teamData,
                                                  final UUID warId, final War war, final WarTeams warTeams, final long timestamp) {
        // check team A forfeit status
        int forfeitA = warTeams.getTeamA().countForfeits();
        if(forfeitA > warTeams.getTeamA().getTeam().size() / 2) {
            // update war state
            War.end(server, warData, warId, war, warTeams.getTeamB(), warTeams.getTeamA(), true, timestamp);
            return;
        }

        // check team B forfeit status
        int forfeitB = warTeams.getTeamB().countForfeits();
        if(forfeitB > warTeams.getTeamB().getTeam().size() / 2) {
            // update war state
            War.end(server, warData, warId, war, warTeams.getTeamA(), warTeams.getTeamB(), true, timestamp);
            return;
        }
    }

    /**
     * Checks and updates win status if all players on one team have died at least once
     * @param server the server
     * @param warData the war data
     * @param teamData the team data
     * @param warId the war ID
     * @param war the war instance
     * @param warTeams the war teams instance
     * @param timestamp the game time
     */
    private static void checkAndUpdateWin(final MinecraftServer server, final WarSavedData warData, final TeamSavedData teamData,
                                              final UUID warId, final War war, final WarTeams warTeams, final long timestamp) {
        // check team A lose condition
        if(checkLoseCondition(server, warData, teamData, warId, war, warTeams.getTeamA())) {
            War.end(server, warData, warId, war, warTeams.getTeamB(), warTeams.getTeamA(), false, timestamp);
            return;
        }
        // check team B lose condition
        if(checkLoseCondition(server, warData, teamData, warId, war, warTeams.getTeamB())) {
            War.end(server, warData, warId, war, warTeams.getTeamA(), warTeams.getTeamB(), false, timestamp);
            return;
        }
    }

    /**
     * Checks if a team meets the losing conditions: all their players die at least once; or
     * players all have at least 1 death except for one, and the one player is offline, and there is more than 1 player on the team
     * @param server the server
     * @param warData the war data
     * @param teamData the team data
     * @param warId the war ID
     * @param war the war instance
     * @param team the war team
     * @return true if the given team lost
     */
    private static boolean checkLoseCondition(final MinecraftServer server, final WarSavedData warData, final TeamSavedData teamData,
                                              final UUID warId, final War war, final WarTeam team) {
        int deathCount = team.countPlayersWithDeaths();
        // check if all players have died at least once
        if(deathCount >= team.getTeam().size()) {
            return true;
        }
        // check if there is one player remaining
        if(team.getTeam().size() > 1 && deathCount == team.getTeam().size() - 1) {
            // locate the remaining player
            for(Map.Entry<UUID, WarTeamEntry> entry : team.getTeam().entrySet()) {
                if(entry.getValue().getDeathCount() <= 0) {
                    // determine if player is offline
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if(null == player) {
                        return true;
                    }
                }
            }
        }
        // no lose condition detected
        return false;
    }

    /**
     * Checks and updates win status if all players on one team have died at least once
     * @param server the server
     * @param warData the war data
     * @param teamData the team data
     * @param warId the war ID
     * @param war the war instance
     * @param warTeams the war teams instance
     */
    private static void updateReward(final MinecraftServer server, final WarSavedData warData, final TeamSavedData teamData,
                                     final UUID warId, final War war, final WarTeams warTeams) {
        int rewardCount = 0;
        // reward all players in team A
        boolean isWinA = warTeams.getTeamA().isWin();
        for(Map.Entry<UUID, WarTeamEntry> entry : warTeams.getTeamA().getTeam().entrySet()) {
            // add reward
            if(!entry.getValue().isRewarded() && WarUtils.reward(server, entry.getKey(), isWinA, warTeams.getTeamA().hasReward())) {
                entry.getValue().setRewarded(true);
                teamData.setDirty();
            }
            // update reward count
            if(entry.getValue().isRewarded()) {
                rewardCount++;
            }
        }
        // reward all players in team B
        boolean isWinB = warTeams.getTeamB().isWin();
        for(Map.Entry<UUID, WarTeamEntry> entry : warTeams.getTeamB().getTeam().entrySet()) {
            // add reward
            if(!entry.getValue().isRewarded() && WarUtils.reward(server, entry.getKey(), isWinB, warTeams.getTeamB().hasReward())) {
                entry.getValue().setRewarded(true);
                teamData.setDirty();
            }
            // update reward count
            if(entry.getValue().isRewarded()) {
                rewardCount++;
            }
        }
        // remove war once all players have been rewarded
        if(rewardCount >= warTeams.getTeamA().getTeam().size() + warTeams.getTeamB().getTeam().size()) {
            warData.invalidateWar(warId);
        }
    }


}
