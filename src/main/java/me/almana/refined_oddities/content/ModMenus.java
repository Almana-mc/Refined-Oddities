package me.almana.refined_oddities.content;

import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(
        Registries.MENU,
        Refined_oddities.MODID
    );
    public static final DeferredHolder<MenuType<?>, MenuType<CompressionConfigurationMenu>> COMPRESSION_CONFIGURATION =
        REGISTER.register("compression_storage_configuration", () -> IMenuTypeExtension.create(
            (syncId, inventory, data) -> new CompressionConfigurationMenu(
                syncId,
                inventory,
                data.readEnum(InteractionHand.class)
            )
        ));

    private ModMenus() {
    }
}
