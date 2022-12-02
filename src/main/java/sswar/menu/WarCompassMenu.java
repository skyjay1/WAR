package sswar.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import sswar.WarRegistry;
import sswar.network.ServerBoundWarCompassPacket;
import sswar.network.WarNetwork;

public class WarCompassMenu extends AbstractContainerMenu {

    private final Container validPlayers;

    public WarCompassMenu(int id, final Container validPlayers) {
        super(WarRegistry.WAR_COMPASS_MENU.get(), id);
        this.validPlayers = validPlayers;
    }

    //// METHODS ////

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(player.getUsedItemHand()).is(Items.COMPASS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    /**
     * Called when the inventory closes. Sends a packet to the server
     * with the UUID changes (which will be verified by the server)
     * @param slot the selected slot
     */
    public void sendPacketToServer(final int slot) {
        WarNetwork.CHANNEL.sendToServer(new ServerBoundWarCompassPacket(slot));
    }

    public Container getValidPlayers() {
        return validPlayers;
    }
}
