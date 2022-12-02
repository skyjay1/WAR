package sswar.client.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageIconButton extends Button {

    protected final ResourceLocation resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;

    protected final int iconU;
    protected final int iconV;
    protected final int iconW;
    protected final int iconH;

    public ImageIconButton(int x, int y, int w, int h, int u, int v, int deltaV, ResourceLocation texture, int iconU, int iconV, int iconW, int iconH, Button.OnPress onPress, Button.OnTooltip onTooltip, Component title) {
        super(x, y, w, h, title, onPress, onTooltip);
        this.resourceLocation = texture;
        this.xTexStart = u;
        this.yTexStart = v;
        this.yDiffTex = deltaV;
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconW = iconW;
        this.iconH = iconH;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.resourceLocation);
        // determine v coordinate
        int i = this.yTexStart;
        if (!this.isEnabled() || this.isHoveredOrFocused()) {
            i += this.yDiffTex;
        }
        // render image
        RenderSystem.enableDepthTest();
        blit(poseStack, this.x, this.y, (float)this.xTexStart, (float)i, this.width, this.height, 256, 256);
        // render icon
        int renderX = this.x + (this.width - iconW) / 2;
        int renderY = this.y + (this.height - iconH) / 2;
        blit(poseStack, renderX, renderY, getIconU(), getIconV(), iconW, iconH, 256, 256);
        // render tooltip
        if (this.isHovered) {
            this.renderToolTip(poseStack,mouseX, mouseY);
        }
    }

    @Override
    public void onPress() {
        if(this.isEnabled()) {
            super.onPress();
        }
    }

    //// GETTERS AND SETTERS ////

    public int getIconU() {
        return iconU;
    }

    public int getIconV() {
        return iconV;
    }

    public boolean isEnabled() {
        return active;
    }

    public void setEnabled(boolean enabled) {
        this.active = enabled;
    }
}
