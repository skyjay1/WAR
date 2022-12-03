package sswar.client.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import sswar.WarUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemButtonHolder implements GuiEventListener, Widget, NarratableEntry {

    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 18;

    private static final int EDIT_HEIGHT = 16;
    private static final int SCROLL_WIDTH = 14;

    private static final int MARGIN_X = 4;
    private static final int MARGIN_Y = 4;

    protected final IWidgetManager screen;
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
    protected int scrollOffset;

    protected EditBox editBox;
    protected List<ItemButton> buttons;
    protected ScrollButton scrollBar;

    public ItemButtonHolder(final IWidgetManager screen, Container container, int x, int y, int countX, int countY,
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
        init();
    }

    //// METHODS ////

    private void init() {
        // add edit box
        editBox = screen.addRenderableWidgetToManager(new EditBox(screen.getFont(), x, y, editWidth, EDIT_HEIGHT, text) {
            @Override
            public void setFocus(boolean isFocused) {
                super.setFocus(isFocused);
                if (!isFocused && getValue().isEmpty()) {
                    setValue(text.getString());
                }
            }
        });
        editBox.setValue(text.getString());
        editBox.setFilter(s -> s.matches(WarUtils.WAR_NAME_REGEX));
        editBox.setMaxLength(WarUtils.WAR_NAME_MAX_LENGTH);
        // only add the edit box if the text is not empty
        enableText(text.getContents() != ComponentContents.EMPTY);
        // add scroll bar
        scrollBar = screen.addRenderableWidgetToManager(new ScrollButton(this.x + BUTTON_WIDTH * countX + MARGIN_X + 1, this.y + EDIT_HEIGHT + MARGIN_Y + 1, 12, BUTTON_HEIGHT * countY - 2, DeclareWarScreen.TEXTURE_WIDGETS,
                0, 72, 12, 15, 15, true, 1.0F / Math.max(1.0F, countX), (b, scroll) -> updateScroll(scroll)));
        scrollBar.active = getContainer().getContainerSize() > countX * countY;
        // add item buttons
        this.buttons.clear();
        for(int i = 0; i < countY; i++) {
            for(int j = 0; j < countX; j++) {
                this.buttons.add(screen.addRenderableWidgetToManager(new ItemButton(screen,
                        this.x + j * BUTTON_WIDTH, this.y + EDIT_HEIGHT + MARGIN_Y + i * BUTTON_HEIGHT,
                        BUTTON_WIDTH, BUTTON_HEIGHT, b -> onClickItem.accept((ItemButton) b), b -> (screen.hasSelected() && screen.isSelected(b))) {
                    @Override
                    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
                        return scrollBar.active && scrollBar.mouseScrolled(mouseX, mouseY, amount);
                    }
                }));
            }
        }
        // update all buttons
        updateItemButtons();
    }

    public void updateScroll(final float scrollPercent) {
        int scrollRows = (int) Math.floor(scrollPercent * countY);
        if(scrollRows != scrollOffset) {
            scrollOffset = scrollRows;
            updateItemButtons();
        }
    }

    public void updateItemButtons() {
        for(int i = 0; i < countY; i++) {
            for(int j = 0; j < countX; j++) {
                // determine itemstack
                int index = j + (i + scrollOffset) * countX;
                if(index < container.getContainerSize()) {
                    this.buttons.get(j + i * countX).setItemStack(container.getItem(index), index);
                } else {
                    this.buttons.get(j + i * countX).setItemStack(ItemStack.EMPTY, -1);
                }
            }
        }
    }

    public void enableText(final boolean enableText) {
        this.editBox.visible = this.editBox.active = enableText;
    }

    /**
     * Hides all elements except for the edit box
     */
    public void collapse() {
        this.collapse = true;
        this.buttons.forEach(b -> b.visible = b.active = false);
        this.scrollBar.visible = this.scrollBar.active = false;
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

    //// WIDGET ////

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        for(ItemButton button : buttons) {
            if(button.isHoveredOrFocused()) {
                button.renderToolTip(poseStack, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return scrollBar.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public NarrationPriority narrationPriority() {
        if(editBox != null && editBox.isFocused()) {
            return editBox.narrationPriority();
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        if(editBox != null && editBox.isFocused()) {
            editBox.updateNarration(output);
        }
        // do nothing
    }
}
