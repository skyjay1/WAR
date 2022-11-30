package sswar;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import sswar.capability.IWarMember;
import sswar.network.WarNetwork;

@Mod(SSWar.MODID)
public class SSWar {
    public static final String MODID = "sswar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Capability<IWarMember> WAR_MEMBER = CapabilityManager.get(new CapabilityToken<>() {});

    private static final ForgeConfigSpec.Builder CONFIG_BUILDER = new ForgeConfigSpec.Builder();
    public static final WarConfig CONFIG = new WarConfig(CONFIG_BUILDER);
    private static final ForgeConfigSpec CONFIG_SPEC = CONFIG_BUILDER.build();

    public SSWar() {
        // register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CONFIG_SPEC);
        // register event handlers
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

}
