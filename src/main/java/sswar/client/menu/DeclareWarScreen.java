package sswar.client.menu;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import sswar.SSWar;
import sswar.WarUtils;
import sswar.menu.DeclareWarMenu;
import sswar.util.MessageUtils;

import javax.annotation.Nullable;

public class DeclareWarScreen extends Screen implements MenuAccess<DeclareWarMenu>, IWidgetManager {
    public static final ResourceLocation TEXTURE_SIMPLE = new ResourceLocation(SSWar.MODID, "textures/gui/declare_war_simple.png");
    public static final ResourceLocation TEXTURE_CUSTOM = new ResourceLocation(SSWar.MODID, "textures/gui/declare_war.png");
    public static final ResourceLocation TEXTURE_WIDGETS = new ResourceLocation(SSWar.MODID, "textures/gui/declare_war_widgets.png");

    private static final int IMAGE_WIDTH = 248;
    private static final int IMAGE_HEIGHT = 220;

    private static final int VALID_PLAYERS_X = 7;
    private static final int VALID_PLAYERS_Y = 7;
    private static final int TEAM_A_X = VALID_PLAYERS_X;
    private static final int TEAM_A_Y = 103;
    private static final int TEAM_B_X = 151;
    private static final int TEAM_B_Y = TEAM_A_Y;

    private static final int DONE_BUTTON_HEIGHT = 20;

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

    private Button buttonCustomize;
    private Button buttonDone;
    private ImageTextButton buttonCount;
    private ImageIconButton buttonSwap;
    private ImageIconButton buttonRemove;
    private ImageIconButton buttonRandomize;

    private Component countTooltip = Component.literal("");

    private int totalPlayerCount;

    public DeclareWarScreen(DeclareWarMenu menu, Inventory playerInv, Component title) {
        super(title);
        this.menu = menu;
        this.isSimple = true;
        this.totalPlayerCount = 0;
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
        this.topPos = (this.height - DONE_BUTTON_HEIGHT - IMAGE_HEIGHT) / 2;
        getMinecraft().keyboardHandler.setSendRepeatsToGui(true);
        // create button holder for valid players
        validPlayerButtons = this.addWidget(new ItemButtonHolder(this, menu.getValidPlayers(), this.leftPos + VALID_PLAYERS_X, this.topPos + VALID_PLAYERS_Y,
                12, 4, 120, Component.literal(WarUtils.WAR_NAME), b -> {
            int slot = b.getSlot();
            if(slot >= 0) {
                ItemButtonHolder other = (isSimple ? selectedPlayerButtons : teamAPlayerButtons);
                getMenu().transfer(validPlayerButtons.getContainer(), other.getContainer(), slot);
                validPlayerButtons.updateItemButtons();
                other.updateItemButtons();
                clearSelected();
                updateCount();
            }
        }, s -> getMenu().setWarName(s)));
        // create button holder for selected players
        selectedPlayerButtons = this.addWidget(new ItemButtonHolder(this, menu.getSelectedPlayers(), this.leftPos + TEAM_A_X, this.topPos + TEAM_A_Y,
                12, 4, 90, Component.empty(), b -> {
            int slot = b.getSlot();
            if(slot >= 0) {
                ItemButtonHolder other = validPlayerButtons;
                getMenu().transfer(selectedPlayerButtons.getContainer(), other.getContainer(), slot);
                selectedPlayerButtons.updateItemButtons();
                other.updateItemButtons();
                clearSelected();
                updateCount();
            }
        }, s -> getMenu().setTeamAName(s)));

        // create button holder for team A
        teamAPlayerButtons = this.addWidget(new ItemButtonHolder(this, menu.getTeamA(), this.leftPos + TEAM_A_X, this.topPos + TEAM_A_Y,
                4, 5, 90, Component.literal(WarUtils.TEAM_A_NAME), b -> setSelected(teamAPlayerButtons, b), s -> getMenu().setTeamAName(s)));
        // create button holder for team B
        teamBPlayerButtons = this.addWidget(new ItemButtonHolder(this, menu.getTeamB(), this.leftPos + TEAM_B_X, this.topPos + TEAM_B_Y,
                4, 5, 90, Component.literal(WarUtils.TEAM_B_NAME), b -> setSelected(teamBPlayerButtons, b), s -> getMenu().setTeamBName(s)));

        // init item button holders
        if(isSimple) {
            teamAPlayerButtons.collapse();
            teamBPlayerButtons.collapse();
        } else {
            this.selectedPlayerButtons.collapse();
        }

        // add upper buttons
        // toggle prep period
        this.addRenderableWidget(new ImageIconButton(leftPos + 131, topPos + 6, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 0, 140, 11, 11,
                b -> {
                    getMenu().toggleHasPrepPeriod();
                    b.setMessage(MessageUtils.component("gui.sswar.declare_war.prep_period." + (getMenu().hasPrepPeriod() ? "on" : "off")));
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.prep_period." + (getMenu().hasPrepPeriod() ? "on" : "off"))) {
            @Override
            public int getIconV() {
                return (getMenu().hasPrepPeriod() ? 0 : 16) + super.getIconV();
            }
        });
        // add one random
        this.addRenderableWidget(new ImageIconButton(leftPos + 179, topPos + 6, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 16, 140, 7, 7,
                b -> {
                    ItemButtonHolder other = (isSimple ? selectedPlayerButtons : teamAPlayerButtons);
                    int slot = DeclareWarMenu.getRandomSlot(validPlayerButtons.getContainer());
                    if(slot >= 0) {
                        getMenu().transfer(validPlayerButtons, other, slot);
                        updateCount();
                    }
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.add_one_random")));
        // add max random
        this.addRenderableWidget(new ImageIconButton(leftPos + 201, topPos + 6, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 32, 140, 15, 7,
                b -> {
                    while(!validPlayerButtons.getContainer().isEmpty() && getCount() < getMenu().getMaxPlayers()) {
                        ItemButtonHolder other = (isSimple ? selectedPlayerButtons : teamAPlayerButtons);
                        int slot = DeclareWarMenu.getRandomSlot(validPlayerButtons.getContainer());
                        if(slot >= 0) {
                            getMenu().transfer(validPlayerButtons, other, slot);
                            updateCount();
                        }
                    }
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.add_max_random")));
        // reset
        this.addRenderableWidget(new ImageIconButton(leftPos + 223, topPos + 6, 18, 18, 0, 36, 18, TEXTURE_WIDGETS, 48, 140, 10, 10,
                b -> {
                    if(isSimple) {
                        getMenu().transferAll(selectedPlayerButtons, validPlayerButtons);
                    } else {
                        getMenu().transferAll(teamAPlayerButtons, validPlayerButtons);
                        getMenu().transferAll(teamBPlayerButtons, validPlayerButtons);
                    }
                    clearSelected();
                    updateCount();
                }, (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), MessageUtils.component("gui.sswar.declare_war.remove_all")));

        // add Customize Teams button
        this.buttonCustomize = this.addRenderableWidget(new ImageTextButton(leftPos + 7, topPos + 197, 108, 18, 0, 0, 18, TEXTURE_WIDGETS,
                b -> setSimple(false), (b, p, mx, my) -> renderTooltip(p, b.getMessage(), mx, my), font, MessageUtils.component("gui.sswar.declare_war.customize_teams")));
        this.buttonCustomize.visible = isSimple;

        // add count button
        this.buttonCount = this.addRenderableWidget(new ImageTextButton(leftPos + 106, topPos + 102, 36, 18, 106, 102, 0, getTexture(),
                b -> {}, (b, p, mx, my) -> renderTooltip(p, getCountTooltip(), mx, my), font, Component.empty()));

        // add advanced buttons
        final Component noPlayerSelected = MessageUtils.component("gui.sswar.declare_war.no_player_selected").withStyle(ChatFormatting.RED);
        // swap team
        this.buttonSwap = (this.addRenderableWidget(new ImageIconButton(leftPos + 106, topPos + 143, 36, 18, 18, 36, 18, TEXTURE_WIDGETS, 64, 140, 26, 14,
                b -> {
                    if(hasSelected()) {
                        ItemButtonHolder opposite = (selectedHolder == teamAPlayerButtons) ? teamBPlayerButtons : teamAPlayerButtons;
                        getMenu().transfer(selectedHolder, opposite, selectedButton.getSlot());
                        clearSelected();
                    }
                }, (b, p, mx, my) -> renderTooltip(p, hasSelected() ? b.getMessage() : noPlayerSelected, mx, my), MessageUtils.component("gui.sswar.declare_war.swap"))));
        // remove from team
        this.buttonRemove = (this.addRenderableWidget(new ImageIconButton(leftPos + 106, topPos + 165, 36, 18, 18, 36, 18, TEXTURE_WIDGETS, 96, 140, 11, 11,
                b -> {
                    if(hasSelected()) {
                        getMenu().transfer(selectedHolder, validPlayerButtons, selectedButton.getSlot());
                        clearSelected();
                        updateCount();
                    }
                }, (b, p, mx, my) -> renderTooltip(p, hasSelected() ? b.getMessage() : noPlayerSelected, mx, my), MessageUtils.component("gui.sswar.declare_war.remove"))));
        // randomize teams
        this.buttonRandomize = (this.addRenderableWidget(new ImageIconButton(leftPos + 106, topPos + 195, 36, 18, 18, 36, 18, TEXTURE_WIDGETS, 112, 140, 26, 11,
                b -> {
                    Container temp = new SimpleContainer(validPlayerButtons.getContainer().getContainerSize());
                    getMenu().transferAll(teamAPlayerButtons.getContainer(), temp);
                    getMenu().transferAll(teamBPlayerButtons.getContainer(), temp);
                    DeclareWarMenu.randomlySplitContents(temp, teamAPlayerButtons.getContainer(), teamBPlayerButtons.getContainer());
                    teamAPlayerButtons.updateItemButtons();
                    teamBPlayerButtons.updateItemButtons();
                    clearSelected();
                }, (b, p, mx, my) -> renderTooltip(p, getCount() > 0 ? b.getMessage() : noPlayerSelected, mx, my), MessageUtils.component("gui.sswar.declare_war.randomize"))));
        this.buttonSwap.setEnabled(hasSelected());
        this.buttonRemove.setEnabled(hasSelected());
        this.buttonRandomize.setEnabled(totalPlayerCount > 0);
        this.buttonSwap.visible = this.buttonRemove.visible = this.buttonRandomize.visible = !isSimple;
        // add Done button
        this.buttonDone = this.addRenderableWidget(new Button(this.leftPos, this.topPos + IMAGE_HEIGHT + 2, IMAGE_WIDTH, DONE_BUTTON_HEIGHT, getTitle(), b -> {
            if(getCount() > 1) {
                // update text fields
                getMenu().setWarName(validPlayerButtons.getEditBox().getValue());
                getMenu().setTeamAName(teamAPlayerButtons.getEditBox().getValue());
                getMenu().setTeamBName(teamBPlayerButtons.getEditBox().getValue());
                // send selection to server
                getMenu().sendPacketToServer();
            }
            onClose();
        }));
        // update initial count
        updateCount();
    }

    @Override
    public void onClose() {
        super.onClose();
        getMinecraft().keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public DeclareWarMenu getMenu() {
        return menu;
    }

    public ResourceLocation getTexture() {
        return isSimple ? TEXTURE_SIMPLE : TEXTURE_CUSTOM;
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
        return selectedHolder != null && selectedButton != null;
    }

    @Override
    public boolean isSelected(final ItemButton button) {
        return button != null && button == selectedButton;
    }

    //// HELPER METHODS ////

    public void setSelected(final ItemButtonHolder holder, final ItemButton button) {
        this.selectedHolder = holder;
        this.selectedButton = button;
        if(buttonSwap != null && buttonRemove != null) {
            this.buttonSwap.setEnabled(true);
            this.buttonRemove.setEnabled(true);
        }
    }

    public void clearSelected() {
        this.selectedHolder = null;
        this.selectedButton = null;
        if(buttonSwap != null && buttonRemove != null) {
            this.buttonSwap.setEnabled(false);
            this.buttonRemove.setEnabled(false);
        }
    }

    public void updateCount() {
        if(isSimple) {
            totalPlayerCount = DeclareWarMenu.countNonEmpty(selectedPlayerButtons.getContainer());
        } else {
            totalPlayerCount = DeclareWarMenu.countNonEmpty(teamAPlayerButtons.getContainer()) + DeclareWarMenu.countNonEmpty(teamBPlayerButtons.getContainer());
        }
        buttonRandomize.setEnabled(totalPlayerCount > 0);
        // update count display
        Component countMessage;
        countTooltip.getSiblings().clear();
        if(getMenu().getMaxPlayers() == WarUtils.MAX_PLAYER_COUNT) {
            countMessage = (MessageUtils.component("gui.sswar.declare_war.count_and_max", totalPlayerCount, "\u221E"));
            countTooltip = (MessageUtils.component("gui.sswar.declare_war.count.tooltip", totalPlayerCount));
        } else {
            countMessage = (MessageUtils.component("gui.sswar.declare_war.count_and_max", totalPlayerCount, menu.getMaxPlayers()));
            countTooltip = (MessageUtils.component("gui.sswar.declare_war.count_and_max.tooltip", totalPlayerCount, menu.getMaxPlayers()));
        }
        buttonCount.setMessage(countMessage);
    }

    protected Component getCountTooltip() {
        return countTooltip;
    }

    public int getCount() {
        return totalPlayerCount;
    }

    public void setSimple(final boolean isSimple) {
        this.isSimple = isSimple;
        if(isSimple) {
            getMenu().forceTransfer();
            getMenu().transferAll(menu.getTeamA(), menu.getSelectedPlayers());
            getMenu().forceTransfer();
            getMenu().transferAll(menu.getTeamB(), menu.getSelectedPlayers());
        } else {
            getMenu().forceTransfer();
            getMenu().transferAll(menu.getSelectedPlayers(), menu.getTeamA());
        }
        // save text fields
        String warName = validPlayerButtons.getEditBox().getValue();
        String teamAName = teamAPlayerButtons.getEditBox().getValue();
        String teamBName = teamBPlayerButtons.getEditBox().getValue();
        // recreate widgets
        init(getMinecraft(), width, height);
        // load text fields
        validPlayerButtons.getEditBox().setValue(warName);
        teamAPlayerButtons.getEditBox().setValue(teamAName);
        teamBPlayerButtons.getEditBox().setValue(teamBName);
    }
}
