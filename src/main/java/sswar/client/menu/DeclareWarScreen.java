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
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import sswar.SSWar;
import sswar.WarUtils;
import sswar.menu.DeclareWarMenu;
import sswar.network.ServerBoundDeclareWarPacket;
import sswar.network.WarNetwork;
import sswar.util.MessageUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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
    private List<Button> advancedButtons;

    private int totalPlayerCount;

    public DeclareWarScreen(DeclareWarMenu menu, Inventory playerInv, Component title) {
        super(title);
        this.menu = menu;
        this.isSimple = true;
        this.advancedButtons = new ArrayList<>();
        this.totalPlayerCount = 0;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // draw background image
        RenderSystem.setShaderTexture(0, getTexture());
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        // render count
        String sMaxPlayers = (getMenu().getMaxPlayers() == WarUtils.MAX_PLAYER_COUNT) ? "\u221E" : String.valueOf(getMenu().getMaxPlayers());
        this.font.draw(poseStack, MessageUtils.component("gui.sswar.declare_war.invited_and_max", totalPlayerCount, sMaxPlayers), leftPos + 104, topPos + 111, 0);
        // render components
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - 12 - IMAGE_HEIGHT) / 2;
        // create button holder for valid players
        validPlayerButtons = new ItemButtonHolder(this, menu.getValidPlayers(), this.leftPos + VALID_PLAYERS_X, this.topPos + VALID_PLAYERS_Y,
                12, 4, 120, Component.literal(WarUtils.WAR_NAME), b -> {
            int slot = b.getSlot();
            if(slot >= 0) {
                ItemButtonHolder other = (isSimple ? selectedPlayerButtons : teamAPlayerButtons);
                DeclareWarMenu.transfer(validPlayerButtons.getContainer(), other.getContainer(), slot);
                validPlayerButtons.updateItemButtons();
                other.updateItemButtons();
                clearSelected();
                updateCount();
            }
        }, s -> getMenu().setWarName(s));
        // create button holder for selected players
        selectedPlayerButtons = new ItemButtonHolder(this, menu.getSelectedPlayers(), this.leftPos + TEAM_A_X, this.topPos + TEAM_A_Y,
                12, 4, 90, Component.literal(WarUtils.TEAM_A_NAME), b -> {
            int slot = b.getSlot();
            if(slot >= 0) {
                ItemButtonHolder other = validPlayerButtons;
                DeclareWarMenu.transfer(selectedPlayerButtons.getContainer(), other.getContainer(), slot);
                selectedPlayerButtons.updateItemButtons();
                other.updateItemButtons();
                clearSelected();
                updateCount();
            }
        }, s -> getMenu().setTeamAName(s));
        // TODO figure out how to have both Team A and Team B name edit boxes

        // create button holder for team A
        teamAPlayerButtons = new ItemButtonHolder(this, menu.getTeamA(), this.leftPos + TEAM_A_X, this.topPos + TEAM_A_Y,
                4, 5, 90, Component.literal(WarUtils.TEAM_A_NAME), b -> setSelected(teamAPlayerButtons, b), s -> getMenu().setTeamAName(s));
        // create button holder for team B
        teamBPlayerButtons = new ItemButtonHolder(this, menu.getTeamB(), this.leftPos + TEAM_B_X, this.topPos + TEAM_B_Y,
                4, 5, 90, Component.literal(WarUtils.TEAM_B_NAME), b -> setSelected(teamBPlayerButtons, b), s -> getMenu().setTeamBName(s));

        // init item button holders
        validPlayerButtons.init();
        if(isSimple) {
            selectedPlayerButtons.init();
        } else {
            teamAPlayerButtons.init();
            teamBPlayerButtons.init();
        }

        // add upper buttons
        // add one random
        this.addRenderableWidget(new ImageButton(leftPos + 165, topPos + 9, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 256, 256,
                b -> {
                    ItemButtonHolder other = (isSimple ? selectedPlayerButtons : teamAPlayerButtons);
                    int slot = DeclareWarMenu.getRandomSlot(validPlayerButtons.getContainer());
                    if(slot >= 0) {
                        DeclareWarMenu.transfer(validPlayerButtons, other, slot);
                        updateCount();
                    }
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.add_one_random")));
        // add max random
        this.addRenderableWidget(new ImageButton(leftPos + 187, topPos + 9, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 256, 256,
                b -> {
                    while(!validPlayerButtons.getContainer().isEmpty() && getCount() < getMenu().getMaxPlayers()) {
                        ItemButtonHolder other = (isSimple ? selectedPlayerButtons : teamAPlayerButtons);
                        int slot = DeclareWarMenu.getRandomSlot(validPlayerButtons.getContainer());
                        if(slot >= 0) {
                            DeclareWarMenu.transfer(validPlayerButtons, other, slot);
                            updateCount();
                        }
                    }
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.add_max_random")));
        // reset
        this.addRenderableWidget(new ImageButton(leftPos + 209, topPos + 9, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 256, 256,
                b -> {
                    if(isSimple) {
                        DeclareWarMenu.transferAll(selectedPlayerButtons, validPlayerButtons);
                    } else {
                        DeclareWarMenu.transferAll(teamAPlayerButtons, validPlayerButtons);
                        DeclareWarMenu.transferAll(teamBPlayerButtons, validPlayerButtons);
                    }
                    clearSelected();
                    updateCount();
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.remove_all")));

        // add Customize Teams button
        this.customizeTeamsButton = this.addRenderableWidget(new ImageButton(leftPos + 11, topPos + 203, 108, 18, 0, 0, 18, TEXTURE_WIDGETS, 256, 256,
                b -> setSimple(false), (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.customize_teams")));
        this.customizeTeamsButton.visible = isSimple;

        // add advanced buttons
        this.advancedButtons.clear();
        // swap team
        this.advancedButtons.add(this.addRenderableWidget(new ImageButton(leftPos + 110, topPos + 147, 36, 18, 18, 36, 18, TEXTURE_WIDGETS, 256, 256,
                b -> {
                    if(hasSelected()) {
                        ItemButtonHolder opposite = (selectedHolder == teamAPlayerButtons) ? teamBPlayerButtons : teamAPlayerButtons;
                        DeclareWarMenu.transfer(selectedHolder, opposite, selectedButton.getSlot());
                        clearSelected();
                    }
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.swap"))));
        // remove from team
        this.advancedButtons.add(this.addRenderableWidget(new ImageButton(leftPos + 110, topPos + 169, 36, 18, 18, 36, 18, TEXTURE_WIDGETS, 256, 256,
                b -> {
                    if(hasSelected()) {
                        DeclareWarMenu.transfer(selectedHolder, validPlayerButtons, selectedButton.getSlot());
                        clearSelected();
                    }
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.remove"))));
        // randomize teams
        this.advancedButtons.add(this.addRenderableWidget(new ImageButton(leftPos + 110, topPos + 199, 36, 18, 18, 36, 18, TEXTURE_WIDGETS, 256, 256,
                b -> {
                    Container temp = new SimpleContainer(validPlayerButtons.getContainer().getContainerSize());
                    DeclareWarMenu.transferAll(teamAPlayerButtons.getContainer(), temp);
                    DeclareWarMenu.transferAll(teamBPlayerButtons.getContainer(), temp);
                    DeclareWarMenu.randomlySplitContents(temp, teamAPlayerButtons.getContainer(), teamBPlayerButtons.getContainer());
                    teamAPlayerButtons.updateItemButtons();
                    teamBPlayerButtons.updateItemButtons();
                    clearSelected();
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.randomize"))));
        this.advancedButtons.forEach(b -> b.visible = !isSimple);
        // add Done button
        this.addRenderableWidget(new Button(this.leftPos, this.topPos + IMAGE_HEIGHT + 1, IMAGE_WIDTH, 11, getTitle(), b -> {
            //TODO re-enable if(getCount() > 1) {
                getMenu().sendPacketToServer();
            //}
            onClose();
        }));
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        if(super.keyPressed(p_96552_, p_96553_, p_96554_)) {
            validPlayerButtons.onKeyPressed();
            selectedPlayerButtons.onKeyPressed();
            teamAPlayerButtons.onKeyPressed();
            teamBPlayerButtons.onKeyPressed();
            return true;
        }
        return false;
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

    public boolean hasSelected() {
        return selectedHolder != null && selectedButton != null;
    }

    public boolean isSelected(final ItemButton button) {
        return button != null && button == selectedButton;
    }

    public void updateCount() {
        if(isSimple) {
            totalPlayerCount = DeclareWarMenu.countNonEmpty(selectedPlayerButtons.getContainer());
        } else {
            totalPlayerCount = DeclareWarMenu.countNonEmpty(teamAPlayerButtons.getContainer()) + DeclareWarMenu.countNonEmpty(teamBPlayerButtons.getContainer());
        }
    }

    public int getCount() {
        return totalPlayerCount;
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
