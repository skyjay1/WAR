package sswar.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import sswar.SSWar;

import java.util.Optional;

public final class WarNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(SSWar.MODID, "channel"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        int index = 0;
        CHANNEL.registerMessage(index++, ServerBoundDeclareWarPacket.class, ServerBoundDeclareWarPacket::toBytes, ServerBoundDeclareWarPacket::fromBytes, ServerBoundDeclareWarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
