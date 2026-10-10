package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Storage;
import java.util.List;
import java.util.function.Predicate;
import me.almana.refined_oddities.storage.CompressionForm;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.resources.ResourceLocation;

public record CompressionStorageHandle(Storage access, CompressionStorage storage, Predicate<ResourceKey> allowed) {
    public List<ResourceLocation> enabledItemIds() {
        return storage.getFamily().forms().stream()
            .filter(form -> storage.isFormEnabled(form.itemId()) && allowed.test(storage.resource(form)))
            .map(CompressionForm::itemId)
            .toList();
    }
}
