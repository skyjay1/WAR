package sswar.client.menu;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
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
import sswar.menu.DeclareWarMenu;
import sswar.util.MessageUtils;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class DeclareWarScreen extends Screen implements MenuAccess<DeclareWarMenu> {
    public static final ResourceLocation TEXTURE_SIMPLE = new ResourceLocation(SSWar.MODID, "textures/gui/declare_war_simple.png");
    public static final ResourceLocation TEXTURE_CUSTOM = new ResourceLocation(SSWar.MODID, "textures/gui/declare_war.png");
    public static final ResourceLocation TEXTURE_WIDGETS = new ResourceLocation(SSWar.MODID, "textures/gui/declare_war_widgets.png");

    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 228;

    private static final int VALID_PLAYERS_X = 11;
    private static final int VALID_PLAYERS_Y = 11;
    private static final int TEAM_A_X = VALID_PLAYERS_X;
    private static final int TEAM_A_Y = 107;
    private static final int TEAM_B_X = 155;
    private static final int TEAM_B_Y = TEAM_A_Y;


    protected int leftPos;
    protected int topPos;
    protected boolean isSimple;

    private final DeclareWarMenu menu;

    private ItemButtonHolder validPlayerButtons;
    private ItemButtonHolder selectedPlayerButtons;
    private ItemButtonHolder teamAPlayerButtons;
    private ItemButtonHolder teamBPlayerButtons;

    @Nullable
    private ItemButtonHolder selectedHolder;
    @Nullable
    private ItemButton selectedButton;

    private Button customizeTeamsButton;

    public DeclareWarScreen(DeclareWarMenu menu, Inventory playerInv, Component title) {
        super(title);
        this.menu = menu;
        this.isSimple = true;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // draw background image
        RenderSystem.setShaderTexture(0, getTexture());
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        // render components
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - 12 - IMAGE_HEIGHT) / 2;
        validPlayerButtons = new ItemButtonHolder(this, menu.getValidPlayers(), this.leftPos + VALID_PLAYERS_X, this.topPos + VALID_PLAYERS_Y,
                12, 4, 120, Component.literal("War"), () -> isSimple ? selectedPlayerButtons : teamAPlayerButtons);
        selectedPlayerButtons = new ItemButtonHolder(this, menu.getSelectedPlayers(), this.leftPos + TEAM_A_X, this.topPos + TEAM_A_Y,
                12, 4, 90, Component.literal("Team A"), () -> validPlayerButtons);
        // TODO figure out how to have both Team A and Team B name edit boxes

        teamAPlayerButtons = new ItemButtonHolder(this, menu.getTeamA(), this.leftPos + TEAM_A_X, this.topPos + TEAM_A_Y,
                4, 5, 90, Component.literal("Team A"), () -> validPlayerButtons);
        teamBPlayerButtons = new ItemButtonHolder(this, menu.getTeamB(), this.leftPos + TEAM_B_X, this.topPos + TEAM_B_Y,
                4, 5, 90, Component.literal("Team B"), () -> validPlayerButtons);

        // init item button holders
        validPlayerButtons.init();
        if(isSimple) {
            selectedPlayerButtons.init();
        } else {
            teamAPlayerButtons.init();
            teamBPlayerButtons.init();
        }
        // add Customize Teams button
        this.customizeTeamsButton = this.addRenderableWidget(new ImageButton(leftPos + 11, topPos + 203, 108, 18, 0, 0, 18, TEXTURE_WIDGETS, 256, 256,
                b -> setSimple(false), (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.customize_teams")));
        this.customizeTeamsButton.visible = isSimple;
        // add Done button
        this.addRenderableWidget(new Button(this.leftPos, this.topPos + IMAGE_HEIGHT + 1, IMAGE_WIDTH, 11, getTitle(), b -> onClose()));
    }

    @Override
    public DeclareWarMenu getMenu() {
        return menu;
    }

    public ResourceLocation getTexture() {
        return isSimple ? TEXTURE_SIMPLE : TEXTURE_CUSTOM;
    }

    @Override
    public <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidget(T widget) {
        return super.addRenderableWidget(widget);
    }

    public EditBox addEditBox(int x, int y, int width, int height, Component text) {
        return addRenderableWidget(new EditBox(font, x, y, width, height, text));
    }

    public void renderItemStackTooltip(final PoseStack poseStack, final ItemStack itemStack, final int x, final int y) {
        renderTooltip(poseStack, itemStack, x, y);
    }

    public void setSelected(final ItemButtonHolder holder, final ItemButton button) {
        this.selectedHolder = holder;
        this.selectedButton = button;
    }

    public void clearSelected() {
        setSelected(null, null);
    }

    public void setSimple(final boolean isSimple) {
        this.isSimple = isSimple;
        if(isSimple) {
            menu.transferAll(menu.getTeamA(), menu.getSelectedPlayers());
            menu.transferAll(menu.getTeamB(), menu.getSelectedPlayers());
        } else {
            menu.transferAll(menu.getSelectedPlayers(), menu.getTeamA());
        }
        // save text fields
        String warName = validPlayerButtons.getEditBox().getValue();
        String teamAName = selectedPlayerButtons.getEditBox().getValue();
        // TODO team B name
        // recreate widgets
        init(getMinecraft(), width, height);
        // load text fields
        validPlayerButtons.getEditBox().setValue(warName);
        teamAPlayerButtons.getEditBox().setValue(teamAName);
        // TODO team B name
    }
}
