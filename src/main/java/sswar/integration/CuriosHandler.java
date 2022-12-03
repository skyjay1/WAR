package sswar.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import sswar.SSWar;
import sswar.capability.IWarMember;
import sswar.data.WarSavedData;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import java.util.UUID;

public final class CuriosHandler {

    public static void onPlayerDeath(final ServerPlayer player, final WarSavedData warData, final UUID warId) {
        if(player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> writeCuriosTag(player, warMember, handler));
        });
    }

    public static void onPlayerRespawn(final ServerPlayer player) {
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            if(warMember.hasCuriosTag()) {
                readCuriosTag(player, warMember, warMember.getCuriosTag());
            }
        });
    }

    private static void readCuriosTag(final ServerPlayer player, final IWarMember warMember, final ListTag tag) {
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> handler.loadInventory(tag));
        warMember.setCuriosTag(null);
    }

    private static void writeCuriosTag(final ServerPlayer player, final IWarMember warMember, final ICuriosItemHandler handler) {
        warMember.setCuriosTag(handler.saveInventory(true));
    }
}
