package sswar;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;
import sswar.data.TeamSavedData;
import sswar.data.WarSavedData;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;
import sswar.war.recruit.WarRecruitEntry;
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
            if(event.getEntity().level.isClientSide()) {
                return;
            }
            // TODO send message about pending recruit requests
        }

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
    }

    /**
     * Updates each war recruit in the server, then removes any war recruits that are expired
     * @param server the server
     * @param data the war data
     */
    private static void updateServerWarRecruits(final MinecraftServer server, final WarSavedData data) {
        // TODO
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
        if(data.hasRandomWar()) {
            Optional<War> randomWar = data.getWar(data.getRandomWarId());
            randomWar.ifPresent(war -> updateRandomWar(server, data, data.getRandomWarId(), war));
        } else if(gameTime - data.getRandomWarTimestamp() > SSWar.CONFIG.getRandomWarIntervalTicks()) {
            Optional<UUID> warId = WarUtils.tryCreateWar(null, WarUtils.WAR_NAME, WarUtils.TEAM_A_NAME, WarUtils.TEAM_B_NAME, List.of(), List.of(), WarUtils.MAX_PLAYER_COUNT);
            // update timestamp
            warId.ifPresent(uuid -> {
                data.setRandomWarId(uuid, gameTime);
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
            data.getWars().remove(warId);
        }
    }

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
            data.clearRandomWar();
        }
    }

    private static void updateWarRecruit(final MinecraftServer server, final WarSavedData data, final UUID warId, final WarRecruit recruit) {
        // TODO
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
                // check forfeit conditions
                checkAndUpdateForfeit(server, warData, teamData, warId, war, teams, gameTime);
                // check win conditions
                checkAndUpdateWin(server, warData, teamData, warId, war, teams, gameTime);
                break;
            case ENDED:
                updateReward(server, warData, teamData, warId, war, teams);
                break;
            case INVALID: default:
                break;
        }
    }

    /**
     * Checks and updates forfeit status if more than half of one team forfeit
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
            // update win status
            warTeams.getTeamA().setWin(false);
            warTeams.getTeamB().setWin(true);
            // update war state
            War.end(server, warData, warId, war, warTeams.getTeamB(), warTeams.getTeamA(), timestamp);
            return;
        }

        // check team B forfeit status
        int forfeitB = warTeams.getTeamB().countForfeits();
        if(forfeitB > warTeams.getTeamB().getTeam().size() / 2) {
            // update win status
            warTeams.getTeamA().setWin(true);
            warTeams.getTeamB().setWin(false);
            // update war state
            War.end(server, warData, warId, war, warTeams.getTeamA(), warTeams.getTeamB(), timestamp);
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
        // TODO count player deaths to see if either team wins




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
            if(!entry.getValue().isRewarded() && WarUtils.reward(server, entry.getKey(), isWinA)) {
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
            if(!entry.getValue().isRewarded() && WarUtils.reward(server, entry.getKey(), isWinB)) {
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
