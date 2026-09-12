package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.storage.Storage;
import me.almana.refined_oddities.storage.CompressionStorage;

public record CompressionStorageHandle(Storage access, CompressionStorage storage) {
}
