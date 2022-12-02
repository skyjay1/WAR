package sswar.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import sswar.WarRegistry;
import sswar.client.menu.DeclareWarScreen;
import sswar.client.menu.WarCompassScreen;

public final class WarClientEvents {

    public static final class ModHandler {

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(ModHandler::registerContainerRenders);
        }

        private static void registerContainerRenders() {
            MenuScreens.register(WarRegistry.DECLARE_WAR_MENU.get(), DeclareWarScreen::new);
            MenuScreens.register(WarRegistry.WAR_COMPASS_MENU.get(), WarCompassScreen::new);
        }
    }


    public static final class ForgeHandler {


    }
}
