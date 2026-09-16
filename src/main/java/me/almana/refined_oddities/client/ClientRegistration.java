package me.almana.refined_oddities.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.content.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Refined_oddities.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPRESSION_CONFIGURATION.get(), CompressionConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerDiskModel(final ModelEvent.RegisterGeometryLoaders event) {
        RefinedStorageClientApi.INSTANCE.registerDiskModel(
            ModItems.BULK_STORAGE_DISK.get(),
            ResourceLocation.fromNamespaceAndPath("refinedstorage", "block/disk/disk")
        );
    }
}
