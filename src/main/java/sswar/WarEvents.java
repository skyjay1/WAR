package sswar;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;


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
    }
}
