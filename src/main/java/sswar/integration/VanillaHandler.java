package sswar.integration;

import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import sswar.SSWar;
import sswar.capability.IWarMember;
import sswar.data.WarSavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class VanillaHandler {

    public static void onPlayerDeath(final ServerPlayer player, final WarSavedData warData, final UUID warId) {
        if(player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            writeInventoryTag(player, warMember);
        });
    }

    public static void onPlayerRespawn(final ServerPlayer player) {
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            if(warMember.hasInventoryTag()) {
                readInventoryTag(player, warMember, warMember.getInventoryTag());
            }
        });
    }

    private static void readInventoryTag(final ServerPlayer player, final IWarMember warMember, final ListTag tag) {
        // save existing items
        final List<ItemStack> items = new ArrayList<>();
        items.addAll(player.getInventory().items);
        items.addAll(player.getInventory().armor);
        items.addAll(player.getInventory().offhand);
        // clear inventory and load from NBT
        player.getInventory().clearContent();
        player.getInventory().load(tag);
        // reset inventory tag
        warMember.setInventoryTag(null);
        // re-add existing items
        for(ItemStack itemStack : items) {
            if(!itemStack.isEmpty() && !player.getInventory().add(itemStack)) {
                player.drop(itemStack, false);
            }
        }
    }

    private static void writeInventoryTag(final ServerPlayer player, final IWarMember warMember) {
        // save tag
        warMember.setInventoryTag(player.getInventory().save(new ListTag()));
        // remove items
        player.getInventory().clearContent();
    }
}
