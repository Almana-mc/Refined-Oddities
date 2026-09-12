package me.almana.refined_oddities.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class CompressionStorageType implements StorageType {
    public static final CompressionStorageType INSTANCE = new CompressionStorageType();

    private CompressionStorageType() {
    }

    @Override
    public SerializableStorage create(@Nullable final Long capacity, final Runnable listener) {
        return new CompressionStorage(listener);
    }

    @Override
    public MapCodec<SerializableStorage> getMapCodec(final Runnable listener) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.fieldOf("base_units").forGetter(storage -> ((CompressionStorage) storage).getBaseUnits()),
            CompressionForm.CODEC.listOf().fieldOf("forms")
                .forGetter(storage -> ((CompressionStorage) storage).getFamily().forms()),
            ResourceCodecs.ITEM_CODEC.optionalFieldOf("configured_resource")
                .forGetter(storage -> ((CompressionStorage) storage).getConfiguredResource()),
            ResourceLocation.CODEC.optionalFieldOf("configured_item")
                .forGetter(storage -> Optional.empty()),
            ResourceLocation.CODEC.listOf().optionalFieldOf("enabled_items")
                .forGetter(storage -> Optional.of(((CompressionStorage) storage).getEnabledItemIds()))
        ).apply(instance, (baseUnits, forms, configuredResource, configuredItemId, enabledItems) -> new CompressionStorage(
            baseUnits,
            new CompressionFamily(List.copyOf(forms)),
            configuredResource.or(() -> configuredItemId.map(itemId ->
                new ItemResource(BuiltInRegistries.ITEM.get(itemId)))),
            enabledItems,
            listener
        )));
    }

    @Override
    public boolean isAllowed(final ResourceKey resource) {
        return resource instanceof ItemResource;
    }

    @Override
    public long getDiskInterfaceTransferQuota(final boolean stackUpgrade) {
        return stackUpgrade ? 64 : 1;
    }
}
