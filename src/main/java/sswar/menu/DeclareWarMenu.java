package sswar.menu;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeclareWarMenu extends AbstractContainerMenu {

    public static final String KEY_VALID_PLAYER_ITEMS = "ValidPlayerItems";
    public static final String KEY_REQUIRED_PLAYER = "RequiredPlayer";
    public static final String KEY_SKULL_OWNER = "SkullOwner";
    public static final String NAME_REGEX = "[a-zA-Z0-9_ ]{1,24}";

    private final Container validPlayers;
    private final SimpleContainer selectedPlayers;
    private final SimpleContainer teamA;
    private final SimpleContainer teamB;
    private final int maxPlayers;

    private String warName;
    private String teamAName;
    private String teamBName;

    public DeclareWarMenu(int id, final Container validPlayers, final int maxPlayers) {
        super(WarRegistry.DECLARE_WAR_MENU.get(), id);
        this.validPlayers = validPlayers;
        this.maxPlayers = maxPlayers;
        final int containerSize = Math.min(WarUtils.MAX_PLAYER_COUNT, Math.min(validPlayers.getContainerSize(), maxPlayers));
        this.selectedPlayers = new SimpleContainer(containerSize);
        this.teamA = new SimpleContainer(containerSize);
        this.teamB = new SimpleContainer(containerSize);
        this.warName = "War";
        this.teamAName = "Team A";
        this.teamBName = "Team B";
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        if(!player.level.isClientSide()) {
            // DEBUG
            SSWar.LOGGER.debug("war menu closed");
            if(!isSelectionValid() || !tryCreateWar()) {
                // clear containers
                selectedPlayers.clearContent();
                teamA.clearContent();
                teamB.clearContent();
            }
        }
    }

    //// HELPER METHODS ////

    public void transfer(final Container from, final Container to, final int slotFrom) {
        if(slotFrom < 0 || slotFrom >= from.getContainerSize()) {
            return;
        }
        ItemStack itemStack = from.removeItemNoUpdate(slotFrom);
        for(int i = 0, n = to.getContainerSize(); i < n; i++) {
            if(to.getItem(i).isEmpty() && to.canPlaceItem(i, itemStack)) {
                to.setItem(i, itemStack);
            }
        }
    }

    public void transferAll(final Container from, final Container to) {
        for(int i = 0, n = from.getContainerSize(); i < n; i++) {
            // remove item
            ItemStack item = from.removeItemNoUpdate(i);
            // find valid slot
            for(int j = 0, m = to.getContainerSize(); j < m; j++) {
                if(to.canPlaceItem(j, item) && to.getItem(j).isEmpty()) {
                    to.setItem(j, item);
                    break;
                }
            }
        }
        from.setChanged();
        to.setChanged();
    }

    /**
     * @return true if there are a valid number of players in the invited lists
     */
    public boolean isSelectionValid() {
        if(!selectedPlayers.isEmpty()) {
            int selectedCount = selectedPlayers.countItem(Items.PLAYER_HEAD);
            return selectedCount >= 2;
        }
        // check if both team A and team B have at least 1 player
        if(teamA.isEmpty() || teamB.isEmpty()) {
            return false;
        }
        // all checks passed
        return true;
    }

    /**
     * Called when the inventory closes to attempt to create a war
     * @return true if the war was successfully created
     * @see WarUtils#tryCreateWar(String, String, String, List, List, int)
     */
    private boolean tryCreateWar() {
        // if there are players in the main container, randomly split into team A and B
        if(!selectedPlayers.isEmpty()) {
            randomlySplitContents(selectedPlayers, teamA, teamB);
        }
        // create UUID lists from items
        final List<UUID> listA = parsePlayersFromHeads(teamA);
        final List<UUID> listB = parsePlayersFromHeads(teamB);
        // attempt to create a war with the given players
        return WarUtils.tryCreateWar(warName, teamAName, teamBName, listA, listB, maxPlayers).isPresent();
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
        // create list of items to split
        final List<Integer> validSlots = new ArrayList<>();
        for(int i = 0, n = selected.getContainerSize(); i < n; i++) {
            if(!selected.getItem(i).isEmpty()) {
                validSlots.add(i);
            }
        }
        // determine size of each team
        int teamSize = validSlots.size() / 2;
        // check container size
        if(a.getContainerSize() < teamSize || b.getContainerSize() < teamSize) {
            return false;
        }
        // shuffle the list
        Collections.shuffle(validSlots);
        // assign players from selected into A and B evenly (extras go in A)
        a.clearContent();
        b.clearContent();
        for(int i = 0, n = validSlots.size(), slot = 0; i < n; i++) {
            slot = validSlots.remove(0);
            a.setItem(i, selected.removeItemNoUpdate(slot));
            if(!validSlots.isEmpty()) {
                slot = validSlots.remove(0);
                b.setItem(i, selected.removeItemNoUpdate(slot));
            }
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


}
