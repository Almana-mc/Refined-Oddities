package me.almana.refined_oddities.client.integration.jei;

import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.client.CompressionConfigurationScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class RefinedOdditiesJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
        Refined_oddities.MODID,
        "jei"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(final IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
            CompressionConfigurationScreen.class,
            new CompressionGhostIngredientHandler()
        );
    }
}
