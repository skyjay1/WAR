package sswar.client.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public interface IWidgetManager {

    <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidgetToManager(T widget);

    void renderItemStackTooltip(final PoseStack poseStack, final ItemStack itemStack, final int x, final int y);

    Font getFont();

    Screen getScreen();

    boolean hasSelected();

    boolean isSelected(ItemButton b);
}
