package sswar.integration;


import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import sswar.SSWar;
import sswar.WarUtils;

@JeiPlugin
public class JeiCompat implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(SSWar.MODID, "jei_provider");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
       /* registration.registerSubtypeInterpreter(Items.COMPASS, (ingredient, context) -> {
            if(ingredient.hasTag() && ingredient.getTag().getBoolean(WarUtils.KEY_WAR_COMPASS)) {
                return WarUtils.KEY_WAR_COMPASS;
            }
            return "";
        });*/
    }
}
