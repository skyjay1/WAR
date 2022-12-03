package sswar.integration;

import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import sswar.SSWar;
import sswar.capability.IWarMember;
import sswar.data.WarSavedData;

import java.util.UUID;

public final class CosmeticArmorHandler {

    public static void onPlayerDeath(final ServerPlayer player, final WarSavedData warData, final UUID warId) {
        if(player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            writeCosmeticArmorTag(player, warMember);
        });
    }

    public static void onPlayerRespawn(final ServerPlayer player) {
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            if(warMember.hasCosmeticArmorTag()) {
                readCosmeticArmorTag(player, warMember, warMember.getCosmeticArmorTag());
            }
        });
    }

    private static void readCosmeticArmorTag(final ServerPlayer player, final IWarMember warMember, final CompoundTag tag) {
        CosArmorAPI.getCAStacks(player.getUUID()).deserializeNBT(tag);
        warMember.setCosmeticArmorTag(null);
    }

    private static void writeCosmeticArmorTag(final ServerPlayer player, final IWarMember warMember) {
        // save tag
        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());
        warMember.setCosmeticArmorTag(stacks.serializeNBT());
        // remove items
        for(int i = 0, n = stacks.getSlots(); i < n; i++) {
            stacks.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
