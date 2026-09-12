package me.almana.refined_oddities;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.content.ModMenus;
import me.almana.refined_oddities.recipe.CompressionRecipeReloadHandler;
import me.almana.refined_oddities.storage.CompressionStorageType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@Mod(Refined_oddities.MODID)
public class Refined_oddities {
    public static final String MODID = "refined_oddities";

    public Refined_oddities(final IEventBus modBus) {
        ModItems.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreativeItems);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
        NeoForge.EVENT_BUS.addListener(this::tagsUpdated);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> RefinedStorageApi.INSTANCE.getStorageTypeRegistry().register(
            ResourceLocation.fromNamespaceAndPath(MODID, "compression"),
            CompressionStorageType.INSTANCE
        ));
    }

    private void addCreativeItems(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(RefinedStorageApi.INSTANCE.getCreativeModeTabId())) {
            event.accept(ModItems.BULK_STORAGE_DISK.get());
            event.accept(ModItems.BULK_STORAGE_HOUSING.get());
            event.accept(ModItems.BULK_STORAGE_PART.get());
        }
    }

    private void addReloadListener(final AddReloadListenerEvent event) {
        CompressionRecipeReloadHandler.begin(event.getServerResources());
    }

    private void tagsUpdated(final TagsUpdatedEvent event) {
        CompressionRecipeReloadHandler.finish(event);
    }
}
