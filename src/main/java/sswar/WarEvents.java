package sswar;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;
import sswar.data.WarSavedData;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;

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

    private static void updateWar(final MinecraftServer server, final WarSavedData data, final UUID warId, final War war) {

        switch (war.getState()) {
            case CREATING:
                // TODO check if created timestamp is more than 10 minutes and invalidate war
                break;
            case RECRUITING:
                // TODO check failed recruitment
                // TODO check successful recruitment
                break;
            case PREPARING:
                // TODO check for preparation timer expire
                // TODO check for forfeit conditions
                break;
            case ACTIVE:
                // TODO check win conditions and forfeit conditions
                break;
            case ENDED:
                // TODO check for online player to give reward
                break;
            case INVALID: default:
                break;
        }
    }
}
