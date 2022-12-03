package sswar;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import sswar.capability.IWarMember;
import sswar.data.WarSavedData;
import sswar.integration.BlueSkiesHandler;
import sswar.integration.CosmeticArmorHandler;
import sswar.integration.CuriosHandler;
import sswar.integration.VanillaHandler;
import sswar.network.WarNetwork;
import sswar.war.War;

import java.util.UUID;

@Mod(SSWar.MODID)
public class SSWar {
    public static final String MODID = "sswar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Capability<IWarMember> WAR_MEMBER = CapabilityManager.get(new CapabilityToken<>() {});

    private static final ForgeConfigSpec.Builder CONFIG_BUILDER = new ForgeConfigSpec.Builder();
    public static final WarConfig CONFIG = new WarConfig(CONFIG_BUILDER);
    private static final ForgeConfigSpec CONFIG_SPEC = CONFIG_BUILDER.build();

    private static boolean isCuriosLoaded;
    private static boolean isBlueSkiesLoaded;
    private static boolean isCosmeticArmorLoaded;

    public SSWar() {
        // register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIG_SPEC);
        // register event handlers
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SSWar::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().register(WarEvents.ModHandler.class);
        MinecraftForge.EVENT_BUS.register(WarEvents.ForgeHandler.class);
        // register client event handlers
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().register(sswar.client.WarClientEvents.ModHandler.class);
            MinecraftForge.EVENT_BUS.register(sswar.client.WarClientEvents.ForgeHandler.class);
        });
        // register network
        WarNetwork.register();
        // register objects
        WarRegistry.register();
    }

    private static void onCommonSetup(final FMLCommonSetupEvent event) {
        isCuriosLoaded = ModList.get().isLoaded("curios");
        isBlueSkiesLoaded = ModList.get().isLoaded("blue_skies");
        isCosmeticArmorLoaded = ModList.get().isLoaded("cosmeticarmorreworked");
    }

    public static boolean isCuriosLoaded() {
        return isCuriosLoaded;
    }

    public static boolean isBlueSkiesLoaded() {
        return isBlueSkiesLoaded;
    }

    public static boolean isCosmeticArmorLoaded() {
        return isCosmeticArmorLoaded;
    }

    /**
     * Called when a player dies during a war
     * @param player the player
     * @param warData the war data
     * @param warId the war ID
     */
    public static void onPlayerDeath(final ServerPlayer player, final WarSavedData warData, final UUID warId) {
        VanillaHandler.onPlayerDeath(player, warData, warId);
        if(isCuriosLoaded()) {
            CuriosHandler.onPlayerDeath(player, warData, warId);
        }
        if(isBlueSkiesLoaded()) {
            BlueSkiesHandler.onPlayerDeath(player, warData, warId);
        }
        if(isCosmeticArmorLoaded()) {
            CosmeticArmorHandler.onPlayerDeath(player, warData, warId);
        }
    }

    /**
     * Called when a player respawns
     * @param player the player
     */
    public static void onPlayerRespawn(final ServerPlayer player) {
        VanillaHandler.onPlayerRespawn(player);
        if(isCuriosLoaded()) {
            CuriosHandler.onPlayerRespawn(player);
        }
        if(isBlueSkiesLoaded()) {
            BlueSkiesHandler.onPlayerRespawn(player);
        }
        if(isCosmeticArmorLoaded()) {
            CosmeticArmorHandler.onPlayerRespawn(player);
        }
    }
}
