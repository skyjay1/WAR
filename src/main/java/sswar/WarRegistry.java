package sswar;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import sswar.menu.DeclareWarMenu;

public final class WarRegistry {

    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SSWar.MODID);

    public static void register() {
        MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    // MENU TYPES //


    public static final RegistryObject<MenuType<DeclareWarMenu>> DECLARE_WAR_MENU = MENU_TYPES.register("declare_war", () ->
            IForgeMenuType.create((windowId, inv, data) -> {
                // read valid player count
                final int validPlayerCount = data.readInt();
                // read max player count
                final int maxPlayerCount = data.readInt();
                // read NBT tag that contains list of items
                final CompoundTag nbt = data.readNbt();
                final ListTag listTag = nbt.getList(DeclareWarMenu.KEY_VALID_PLAYER_ITEMS, Tag.TAG_COMPOUND);
                // create Container from list
                final SimpleContainer validPlayerItems = new SimpleContainer(validPlayerCount);
                validPlayerItems.fromTag(listTag);
                return new DeclareWarMenu(windowId, validPlayerItems, maxPlayerCount);
            }));
}
