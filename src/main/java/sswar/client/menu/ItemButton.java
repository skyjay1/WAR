package sswar.client.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemButton extends Button {

    protected final DeclareWarScreen screen;
    protected ItemStack itemStack;
    protected int slot;

    public ItemButton(final DeclareWarScreen screen, int x, int y, int width, int height, Button.OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, (b, p, mx, my) -> screen.renderItemStackTooltip(p, ((ItemButton)b).getItemStack(), mx, my));
        this.screen = screen;
        this.itemStack = ItemStack.EMPTY;
        this.slot = -1;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // do not render empty item
        if(null == itemStack || itemStack.isEmpty()) {
            return;
        }
        // render selected background
        if(isHoveredOrFocused() || (screen.hasSelected() && screen.isSelected(this))) {
            int color = 0x88FFFFFF;
            GuiComponent.fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, color);
        }
        // render centered item
        int renderX = this.x + (this.width - 16) / 2;
        int renderY = this.y + (this.height - 16) / 2;
        screen.getMinecraft().getItemRenderer().renderGuiItem(itemStack, renderX, renderY);
        // render tooltip
        if (this.isHoveredOrFocused()) {
            this.renderToolTip(poseStack, mouseX, mouseY);
        }
    }

    @Override
    public Component getMessage() {
        return (itemStack != null && !itemStack.isEmpty()) ? itemStack.getDisplayName() : super.getMessage();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getSlot() {
        return slot;
    }

    public void setItemStack(ItemStack itemStack, int slot) {
        this.itemStack = itemStack;
        this.slot = slot;
        this.visible = itemStack != null && !itemStack.isEmpty();
    }
}
