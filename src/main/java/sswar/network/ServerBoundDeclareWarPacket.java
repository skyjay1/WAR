package sswar.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import sswar.SSWar;
import sswar.WarUtils;
import sswar.menu.DeclareWarMenu;
import sswar.util.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent from the client to the server to declare a war
 **/
public class ServerBoundDeclareWarPacket {

    private static final int MAX_STRING_LENGTH = WarUtils.WAR_NAME_MAX_LENGTH;

    private List<UUID> teamA;
    private List<UUID> teamB;
    private String warName;
    private String nameA;
    private String nameB;
    private boolean hasPrepPeriod;
    
    public ServerBoundDeclareWarPacket(final List<UUID> teamA, final List<UUID> teamB,
                                       final String warName, final String nameA, final String nameB,
                                       final boolean hasPrepPeriod) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.warName = warName;
        this.nameA = nameA;
        this.nameB = nameB;
        this.hasPrepPeriod = hasPrepPeriod;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a ServerBoundDeclareWarPacket based on the PacketBuffer
     */
    public static ServerBoundDeclareWarPacket fromBytes(final FriendlyByteBuf buf) {
        // read war name
        String warName = buf.readUtf(MAX_STRING_LENGTH);
        // read prep flag
        boolean hasPrepPeriod = buf.readBoolean();
        // read team A
        String nameA = buf.readUtf(MAX_STRING_LENGTH);
        int sizeA = buf.readInt();
        List<UUID> teamA = new ArrayList<>(sizeA);
        for(int i = 0; i < sizeA; i++) {
            teamA.add(buf.readUUID());
        }
        // read team B
        String nameB = buf.readUtf(MAX_STRING_LENGTH);
        int sizeB = buf.readInt();
        List<UUID> teamB = new ArrayList<>(sizeB);
        for(int i = 0; i < sizeB; i++) {
            teamB.add(buf.readUUID());
        }
        return new ServerBoundDeclareWarPacket(teamA, teamB, warName, nameA, nameB, hasPrepPeriod);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the ServerBoundDeclareWarPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundDeclareWarPacket msg, final FriendlyByteBuf buf) {
        // write war name
        buf.writeUtf(msg.warName, MAX_STRING_LENGTH);
        // write prep flag
        buf.writeBoolean(msg.hasPrepPeriod);
        // write team A
        buf.writeUtf(msg.nameA, MAX_STRING_LENGTH);
        buf.writeInt(msg.teamA.size());
        for(UUID uuid : msg.teamA) {
            buf.writeUUID(uuid);
        }
        // write team B
        buf.writeUtf(msg.nameB, MAX_STRING_LENGTH);
        buf.writeInt(msg.teamB.size());
        for(UUID uuid : msg.teamB) {
            buf.writeUUID(uuid);
        }
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the ServerBoundDeclareWarPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundDeclareWarPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER && context.getSender() != null) {
            context.enqueueWork(() -> {
                if(context.getSender().containerMenu instanceof DeclareWarMenu warMenu) {
                    context.getSender().closeContainer();
                    if(WarUtils.tryCreateWar(context.getSender().getUUID(), message.warName, message.nameA, message.nameB, message.teamA, message.teamB, warMenu.getMaxPlayers(), message.hasPrepPeriod).isPresent()) {
                        // send feedback
                        int totalCount = message.teamA.size() + message.teamB.size();
                        int minutesLeft = SSWar.CONFIG.RECRUIT_DURATION.get();
                        context.getSender().displayClientMessage(MessageUtils.component("command.war.declare.success.feedback", totalCount, minutesLeft).withStyle(ChatFormatting.GREEN), false);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
