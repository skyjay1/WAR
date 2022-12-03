package sswar.menu;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.PlayerHeadBlock;
import sswar.SSWar;
import sswar.WarRegistry;
import sswar.WarUtils;
import sswar.client.menu.ItemButtonHolder;
import sswar.network.ServerBoundDeclareWarPacket;
import sswar.network.WarNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

public class DeclareWarMenu extends AbstractContainerMenu {

    public static final String KEY_VALID_PLAYER_ITEMS = "ValidPlayerItems";
    public static final String KEY_REQUIRED_PLAYER = "RequiredPlayer";
    public static final String KEY_SKULL_OWNER = "SkullOwner";

    private final Container validPlayers;
    private final SimpleContainer selectedPlayers;
    private final SimpleContainer teamA;
    private final SimpleContainer teamB;
    private final int maxPlayers;

    private String warName;
    private String teamAName;
    private String teamBName;
    private boolean hasPrepPeriod;
    private boolean forceTransfer;

    public DeclareWarMenu(int id, final Container validPlayers, final int maxPlayers) {
        super(WarRegistry.DECLARE_WAR_MENU.get(), id);
        this.validPlayers = validPlayers;
        this.maxPlayers = maxPlayers;
        this.hasPrepPeriod = true;
        final int containerSize = Math.min(WarUtils.MAX_PLAYER_COUNT, Math.min(validPlayers.getContainerSize(), maxPlayers));
        this.selectedPlayers = new SimpleContainer(containerSize);
        this.teamA = new SimpleContainer(containerSize);
        this.teamB = new SimpleContainer(containerSize);
        this.warName = WarUtils.WAR_NAME;
        this.teamAName = WarUtils.TEAM_A_NAME;
        this.teamBName = WarUtils.TEAM_B_NAME;
        // attempt to move required players from valid to selected
        for(int i = 0, n = this.validPlayers.getContainerSize(); i < n; i++) {
            ItemStack itemStack = this.validPlayers.getItem(i);
            if(!itemStack.isEmpty() && itemStack.hasTag() && itemStack.getTag().getBoolean(KEY_REQUIRED_PLAYER)) {
                this.selectedPlayers.addItem(this.validPlayers.removeItemNoUpdate(i));
                this.validPlayers.setChanged();
            }
        }
    }

    //// METHODS ////

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    //// HELPER METHODS ////

    public static int countNonEmpty(final Container container) {
        int count = 0;
        for(int i = 0, n = container.getContainerSize(); i < n; i++) {
            if(!container.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void transfer(final ItemButtonHolder from, final ItemButtonHolder to, final int slotFrom) {
        transfer(from.getContainer(), to.getContainer(), slotFrom);
        from.updateItemButtons();
        to.updateItemButtons();
    }

    public void transfer(final Container from, final Container to, final int slotFrom) {
        if(slotFrom < 0 || slotFrom >= from.getContainerSize()) {
            return;
        }
        ItemStack itemStack = from.getItem(slotFrom);
        // determine if item can be removed
        if(!forceTransfer && itemStack.hasTag() && itemStack.getTag().getBoolean(KEY_REQUIRED_PLAYER)) {
            return;
        }
        forceTransfer = false;
        from.removeItemNoUpdate(slotFrom);
        // find first available slot
        for(int i = 0, n = to.getContainerSize(); i < n; i++) {
            if(to.getItem(i).isEmpty() && to.canPlaceItem(i, itemStack)) {
                // add the item to this slot
                to.setItem(i, itemStack);
                return;
            }
        }
        // no slot found, return item to original container
        from.setItem(slotFrom, itemStack);
    }

    public void transferAll(final ItemButtonHolder from, final ItemButtonHolder to) {
        transferAll(from.getContainer(), to.getContainer());
        from.updateItemButtons();
        to.updateItemButtons();
    }

    public void transferAll(final Container from, final Container to) {
        for(int i = 0, n = from.getContainerSize(); i < n; i++) {
            // remove item
            ItemStack item = from.getItem(i);
            // determine if item can be removed
            if(!forceTransfer && item.hasTag() && item.getTag().getBoolean(KEY_REQUIRED_PLAYER)) {
                continue;
            }
            from.removeItemNoUpdate(i);
            // find valid slot
            for(int j = 0, m = to.getContainerSize(); j < m; j++) {
                if(to.canPlaceItem(j, item) && to.getItem(j).isEmpty()) {
                    to.setItem(j, item);
                    break;
                }
            }
        }
        forceTransfer = false;
        from.setChanged();
        to.setChanged();
    }

    public static int getRandomSlot(final Container container) {
        int seed = Mth.floor(Math.random() * Short.MAX_VALUE);
        for(int i = 0, n = container.getContainerSize(), slot = 0; i < n; i++) {
            slot = (seed + i) % n;
            if(!container.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Called when the inventory closes. Sends a packet to the server
     * with the UUID changes (which will be verified by the server)
     */
    public void sendPacketToServer() {
        // if there are players in the main container, randomly split into team A and B
        if(!selectedPlayers.isEmpty()) {
            randomlySplitContents(selectedPlayers, teamA, teamB);
        }
        // create UUID lists from items
        final List<UUID> listA = parsePlayersFromHeads(teamA);
        final List<UUID> listB = parsePlayersFromHeads(teamB);
        WarNetwork.CHANNEL.sendToServer(new ServerBoundDeclareWarPacket(listA, listB, warName, teamAName, teamBName, hasPrepPeriod));
    }

    /**
     * Checks each item in the container for a Player Head with UUID information
     * @param container the container
     * @return a list of UUIDs corresponding to the player heads in the container
     * @see #parsePlayerFromHead(ItemStack)
     */
    public static List<UUID> parsePlayersFromHeads(final Container container) {
        final List<UUID> list = new ArrayList<>();
        for(int i = 0, n = container.getContainerSize(); i < n; i++) {
            parsePlayerFromHead(container.getItem(i)).ifPresent(uuid -> list.add(uuid));
        }
        return list;
    }

    /**
     * Checks the item for a Player Head with UUID information
     * @param itemStack the player head item stack
     * @return the UUID of the player with the given head, or empty if none is found
     */
    public static Optional<UUID> parsePlayerFromHead(final ItemStack itemStack) {
        // check if the item stack is a player head with correct NBT
        if(itemStack.is(Items.PLAYER_HEAD) && itemStack.hasTag() && itemStack.getTag().contains(KEY_SKULL_OWNER, Tag.TAG_COMPOUND)) {
            // read game profile from item stack NBT
            GameProfile profile = NbtUtils.readGameProfile(itemStack.getTag().getCompound(KEY_SKULL_OWNER));
            if(profile != null) {
                return Optional.of(profile.getId());
            }
        }
        // no checks passed
        return Optional.empty();
    }

    /**
     * Removes items from the first container to split evenly and randomly between the last two containers
     * @param selected the container contents to split
     * @param a the first container
     * @param b the second container
     * @return true if the items were removed and shuffled successfully
     */
    public static boolean randomlySplitContents(final Container selected, final Container a, final Container b) {
        // create stack of items to split
        final Stack<ItemStack> removedItems = new Stack<>();
        for(int i = 0, n = selected.getContainerSize(); i < n; i++) {
            if(!selected.getItem(i).isEmpty()) {
                removedItems.push(selected.removeItemNoUpdate(i));
            }
        }
        // determine required size
        int teamSize = removedItems.size() / 2;
        // check container size
        if(teamSize < 1 || a.getContainerSize() < teamSize || b.getContainerSize() < teamSize) {
            return false;
        }
        // shuffle the list
        Collections.shuffle(removedItems);
        // assign players from selected into A and B evenly (extras go in A)
        a.clearContent();
        b.clearContent();
        int slot = 0;
        while(!removedItems.isEmpty() && slot < a.getContainerSize()) {
            a.setItem(slot, removedItems.pop());
            if(!removedItems.isEmpty()) {
                b.setItem(slot, removedItems.pop());
            }
            slot++;
        }
        // update containers
        selected.setChanged();
        a.setChanged();
        b.setChanged();
        return true;
    }

    //// GETTERS ////

    public Container getValidPlayers() {
        return validPlayers;
    }

    public SimpleContainer getSelectedPlayers() {
        return selectedPlayers;
    }

    public SimpleContainer getTeamA() {
        return teamA;
    }

    public SimpleContainer getTeamB() {
        return teamB;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean hasPrepPeriod() {
        return hasPrepPeriod;
    }

    //// SETTERS ////

    public void setWarName(final String value) {
        warName = value;
    }

    public void setTeamAName(final String value) {
        teamAName = value;
    }

    public void setTeamBName(final String value) {
        teamBName = value;
    }

    public void toggleHasPrepPeriod() {
        this.hasPrepPeriod = !this.hasPrepPeriod;
    }

    /**
     * Indicates that the next transfer or transferAll operation should
     * move items that are marked as Required
     */
    public void forceTransfer() {
        forceTransfer = true;
    }
}
