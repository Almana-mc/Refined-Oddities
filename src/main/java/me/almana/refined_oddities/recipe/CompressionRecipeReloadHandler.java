package me.almana.refined_oddities.recipe;

import net.minecraft.server.ReloadableServerResources;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

public final class CompressionRecipeReloadHandler {
    private static volatile ReloadableServerResources resources;

    private CompressionRecipeReloadHandler() {
    }

    public static void begin(final ReloadableServerResources serverResources) {
        resources = serverResources;
    }

    public static void finish(final TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }
        CompressionRecipeCatalog.INSTANCE.replace(CompressionRecipeScanner.scan(
            resources.getRecipeManager(),
            event.getRegistryAccess()
        ));
    }
}
