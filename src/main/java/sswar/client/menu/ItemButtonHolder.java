package sswar.client.menu;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemButtonHolder {

    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 18;

    private static final int EDIT_HEIGHT = 16;
    private static final int SCROLL_WIDTH = 14;

    private static final int MARGIN_X = 4;
    private static final int MARGIN_Y = 4;

    protected final DeclareWarScreen screen;
    protected final Container container;
    protected final Component text;
    protected final Consumer<ItemButton> onClickItem;
    public int x;
    public int y;
    public final int countX;
    public final int countY;
    public final int editWidth;
    public final int width;
    public final int height;

    protected float scrollAmount; // TODO

    protected EditBox editBox;
    protected List<ItemButton> buttons;
    protected Button scrollBar; // TODO

    public ItemButtonHolder(final DeclareWarScreen screen, Container container, int x, int y, int countX, int countY,
                            int editWidth, final Component text, final Supplier<ItemButtonHolder> containerTo) {
        this.screen = screen;
        this.container = container;
        this.onClickItem = b -> {
            int slot = b.getSlot();
            if(slot >= 0) {
                ItemButtonHolder other = containerTo.get();
                screen.getMenu().transfer(container, other.container, slot);
                updateItemButtons();
                other.updateItemButtons();
            }
        };
        this.x = x;
        this.y = y;
        this.countX = countX;
        this.countY = countY;
        this.text = text;
        this.width = BUTTON_WIDTH * countX + SCROLL_WIDTH + MARGIN_X;
        this.height = BUTTON_HEIGHT * countY + EDIT_HEIGHT + MARGIN_Y;
        this.editWidth = Math.min(editWidth, this.width);
        this.buttons = new ArrayList<>();
    }

    //// METHODS ////

    public void init() {
        // add edit box
        editBox = screen.addEditBox(x, y, editWidth, EDIT_HEIGHT, text);
        editBox.setValue(text.getString());
        // add item buttons
        this.buttons.clear();
        for(int i = 0; i < countY; i++) {
            for(int j = 0; j < countX; j++) {
                this.buttons.add(screen.addRenderableWidget(new ItemButton(screen,
                        this.x + j * BUTTON_WIDTH, this.y + EDIT_HEIGHT + MARGIN_Y + i * BUTTON_HEIGHT,
                        BUTTON_WIDTH, BUTTON_HEIGHT, b -> onClickItem.accept((ItemButton) b))));
            }
        }
        // update all buttons
        updateItemButtons();
    }

    public void updateItemButtons() {
        int scrollRows = (int) Math.floor(scrollAmount * countY);
        for(int i = 0; i < countY; i++) {
            for(int j = 0; j < countX; j++) {
                // determine itemstack
                int index = j + (i + scrollRows) * countX;
                boolean valid = index < container.getContainerSize();
                ItemStack itemStack = valid ? container.getItem(index) : ItemStack.EMPTY;
                this.buttons.get(j + i * countX).setItemStack(itemStack, valid ? index : -1);
            }
        }
    }

    //// GETTERS AND SETTERS ////

    public EditBox getEditBox() {
        return editBox;
    }

    public List<ItemButton> getButtons() {
        return buttons;
    }

    public Button getScrollBar() {
        return scrollBar;
    }
}
