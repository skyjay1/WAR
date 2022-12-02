package sswar.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import sswar.WarUtils;
import sswar.menu.DeclareWarMenu;
import sswar.menu.WarCompassMenu;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent from the client to the server to update tracking player
 **/
public class ServerBoundWarCompassPacket {

    private int selectedSlot;
    
    public ServerBoundWarCompassPacket(final int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a ServerBoundWarCompassPacket based on the PacketBuffer
     */
    public static ServerBoundWarCompassPacket fromBytes(final FriendlyByteBuf buf) {
        int selectedSlot = buf.readInt();
        return new ServerBoundWarCompassPacket(selectedSlot);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the ServerBoundWarCompassPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final ServerBoundWarCompassPacket msg, final FriendlyByteBuf buf) {
        buf.writeInt(msg.selectedSlot);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the ServerBoundWarCompassPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final ServerBoundWarCompassPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER && context.getSender() != null) {
            context.enqueueWork(() -> {
                if(context.getSender().containerMenu instanceof WarCompassMenu warMenu) {
                    // load selected container
                    Container container = warMenu.getValidPlayers();
                    // verify slot exists
                    if(message.selectedSlot < 0 || message.selectedSlot >= container.getContainerSize()) {
                        return;
                    }
                    // locate item
                    ItemStack itemStack = warMenu.getValidPlayers().getItem(message.selectedSlot);
                    // close container
                    context.getSender().closeContainer();
                    // read player from item
                    Optional<UUID> uuid = DeclareWarMenu.parsePlayerFromHead(itemStack);
                    if(uuid.isEmpty()) {
                        return;
                    }
                    // update war compass
                    ServerPlayer target = context.getSender().getServer().getPlayerList().getPlayer(uuid.get());
                    if(target != null) {
                        WarUtils.updateWarCompassTarget(context.getSender(), target);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
