package sswar.client.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageTextButton extends ImageButton {

    protected final Font font;
    protected boolean enableTooltip;

    public ImageTextButton(int x, int y, int w, int h, int u, int v, int deltaV, ResourceLocation texture, OnPress onPress, OnTooltip onTooltip, Font font, Component title) {
        super(x, y, w, h, u, v, deltaV, texture, 256, 256, onPress, onTooltip, title);
        this.font = font;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // disable tooltip
        enableTooltip = false;
        // render image
        super.renderButton(poseStack, mouseX, mouseY, partialTick);
        // render text
        int renderX = this.x + (this.width) / 2;
        int renderY = this.y + (this.height - font.lineHeight) / 2;
        drawCenteredString(poseStack, font, getMessage(), renderX, renderY, 0xFFFFFF);
        // re-enable tooltip
        enableTooltip = true;
        if (this.isHovered) {
            this.renderToolTip(poseStack, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(PoseStack poseStack, int mouseX, int mouseY) {
        if(enableTooltip) {
            super.renderToolTip(poseStack, mouseX, mouseY);
        }
    }
}
