package sswar;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
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
        public static void onAttachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
            if(event.getObject() instanceof Player) {
                event.addCapability(WarMember.ID, new WarMember.Provider());
            }
        }
    }
}
