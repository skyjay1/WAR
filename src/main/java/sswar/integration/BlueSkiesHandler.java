package sswar.integration;

import com.legacy.blue_skies.capability.SkiesPlayer;
import com.legacy.blue_skies.capability.util.ISkiesPlayer;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import sswar.SSWar;
import sswar.capability.IWarMember;
import sswar.data.WarSavedData;

import java.util.UUID;

public final class BlueSkiesHandler {

    public static void onPlayerDeath(final ServerPlayer player, final WarSavedData warData, final UUID warId) {
        if(player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            writeBlueSkiesTag(player, warMember);
        });
    }

    public static void onPlayerRespawn(final ServerPlayer player) {
        player.getCapability(SSWar.WAR_MEMBER).ifPresent(warMember -> {
            if(warMember.hasBlueSkiesTag()) {
                readBlueSkiesTag(player, warMember, warMember.getBlueSkiesTag());
            }
        });
    }

    private static void readBlueSkiesTag(final ServerPlayer player, final IWarMember warMember, final ListTag tag) {
        player.getCapability(SkiesPlayer.INSTANCE).ifPresent(skiesPlayer -> {
            skiesPlayer.getArcInventory().read(tag);
        });
        warMember.setBlueSkiesTag(null);
    }

    private static void writeBlueSkiesTag(final ServerPlayer player, final IWarMember warMember) {
        player.getCapability(SkiesPlayer.INSTANCE).ifPresent(skiesPlayer -> {
            warMember.setBlueSkiesTag(skiesPlayer.getArcInventory().write(new ListTag()));
            skiesPlayer.getArcInventory().clearContent();
        });
    }
}
