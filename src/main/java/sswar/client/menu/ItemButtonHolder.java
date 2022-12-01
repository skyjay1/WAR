package sswar.client.menu;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import sswar.WarUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    protected final Consumer<String> onEditText;
    public int x;
    public int y;
    public final int countX;
    public final int countY;
    public final int editWidth;
    public final int width;
    public final int height;

    protected boolean collapse;

    protected float scrollAmount; // TODO

    protected EditBox editBox;
    protected List<ItemButton> buttons;
    protected Button scrollBar;

    public ItemButtonHolder(final DeclareWarScreen screen, Container container, int x, int y, int countX, int countY,
                            int editWidth, final Component text, final Consumer<ItemButton> onClick, final Consumer<String> onEditText) {
        this.screen = screen;
        this.container = container;
        this.onClickItem = onClick;
        this.onEditText = onEditText;
        this.x = x;
        this.y = y;
        this.countX = countX;
        this.countY = countY;
        this.text = text;
        this.width = BUTTON_WIDTH * countX + SCROLL_WIDTH + MARGIN_X;
        this.height = BUTTON_HEIGHT * countY + EDIT_HEIGHT + MARGIN_Y;
        this.editWidth = Math.min(editWidth, this.width);
        this.buttons = new ArrayList<>();
        this.collapse = false;
    }

    //// METHODS ////

    public void init() {
        // add edit box
        editBox = screen.addRenderableWidget(new EditBox(screen.getFont(), x, y, editWidth, EDIT_HEIGHT, text) {
            @Override
            public void setFocus(boolean isFocused) {
                super.setFocus(isFocused);
                if(!isFocused && getValue().isEmpty()) {
                    setValue(text.getString());
                }
            }
        });
        editBox.setValue(text.getString());
        editBox.setFilter(s -> s.matches(WarUtils.WAR_NAME_REGEX));
        editBox.setMaxLength(WarUtils.WAR_NAME_MAX_LENGTH);
        // add item buttons
        this.buttons.clear();
        for(int i = 0; i < countY; i++) {
            for(int j = 0; j < countX; j++) {
                this.buttons.add(screen.addRenderableWidget(new ItemButton(screen,
                        this.x + j * BUTTON_WIDTH, this.y + EDIT_HEIGHT + MARGIN_Y + i * BUTTON_HEIGHT,
                        BUTTON_WIDTH, BUTTON_HEIGHT, b -> onClickItem.accept((ItemButton) b))));
            }
        }
        // add scroll bar
        // TODO actually add scroll bar
        scrollBar = screen.addRenderableWidget(new Button(this.x + BUTTON_WIDTH * countX + MARGIN_X + 1, this.y + EDIT_HEIGHT + MARGIN_Y + 1, 12, 15, Component.empty(), b -> {}));
        // update all buttons
        updateItemButtons();
    }

    public void onKeyPressed() {
        onEditText.accept(editBox.getValue());
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

    public void enableText(final boolean enableText) {
        this.editBox.visible = enableText;
    }

    //// GETTERS AND SETTERS ////

    public Container getContainer() {
        return container;
    }

    public EditBox getEditBox() {
        return editBox;
    }

    public List<ItemButton> getButtons() {
        return buttons;
    }

    public Button getScrollBar() {
        return scrollBar;
    }

    public boolean isCollapse() {
        return collapse;
    }

    public void setCollapse(final boolean collapse) {
        this.collapse = collapse;
        this.buttons.forEach(b -> b.visible = !collapse);
        this.scrollBar.visible = !collapse;
    }
}
