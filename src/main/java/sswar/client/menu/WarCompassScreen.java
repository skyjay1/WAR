package sswar.client.menu;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import sswar.SSWar;
import sswar.menu.WarCompassMenu;

public class WarCompassScreen extends Screen implements MenuAccess<WarCompassMenu>, IWidgetManager {
    public static final ResourceLocation TEXTURE = new ResourceLocation(SSWar.MODID, "textures/gui/war_compass.png");

    private static final int IMAGE_WIDTH = 194;
    private static final int IMAGE_HEIGHT = 96;

    private static final int VALID_PLAYERS_X = 7;
    private static final int VALID_PLAYERS_Y = -3;

    protected int leftPos;
    protected int topPos;

    private final WarCompassMenu menu;

    private ItemButtonHolder validPlayerButtons;

    public WarCompassScreen(WarCompassMenu menu, Inventory playerInv, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // draw background image
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        // draw title
        this.font.draw(poseStack, this.title, (float)this.leftPos + 8, (float)this.topPos + 6, 0x404040);
        // render components
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;
        // create button holder for valid players
        validPlayerButtons = this.addWidget(new ItemButtonHolder(this, menu.getValidPlayers(), this.leftPos + VALID_PLAYERS_X, this.topPos + VALID_PLAYERS_Y,
                12, 4, 120, Component.empty(), b -> {
            int slot = b.getSlot();
            if(slot >= 0) {
                getMenu().sendPacketToServer(slot);
                onClose();
            }
        }, s -> {}));
    }

    @Override
    public WarCompassMenu getMenu() {
        return menu;
    }

    //// WIDGET MANAGER ////

    @Override
    public <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidgetToManager(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public Screen getScreen() {
        return this;
    }

    @Override
    public void renderItemStackTooltip(final PoseStack poseStack, final ItemStack itemStack, final int x, final int y) {
        renderTooltip(poseStack, itemStack, x, y);
    }

    @Override
    public boolean hasSelected() {
        return false;
    }

    @Override
    public boolean isSelected(final ItemButton button) {
        return false;
    }
}
