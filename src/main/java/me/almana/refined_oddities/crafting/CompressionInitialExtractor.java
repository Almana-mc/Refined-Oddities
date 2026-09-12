package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import me.almana.refined_oddities.crafting.CompressionPoolPlanner.Pool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class CompressionInitialExtractor {
    private CompressionInitialExtractor() {
    }

    public static Result extract(final RootStorage rootStorage,
                                 final MutableResourceList requirements,
                                 final MutableResourceList internalStorage) {
        boolean changed = extractOrdinary(rootStorage, requirements, internalStorage);
        final List<CompressionStorageHandle> handles = CompressionStorageWalker.find(rootStorage);
        if (!handles.isEmpty()) {
            changed |= extractCompression(handles, requirements, internalStorage);
        }
        return new Result(changed, requirements.isEmpty());
    }

    private static boolean extractOrdinary(final RootStorage rootStorage,
                                           final MutableResourceList requirements,
                                           final MutableResourceList internalStorage) {
        boolean changed = false;
        for (final ResourceKey resource : new HashSet<>(requirements.getAll())) {
            final long needed = requirements.get(resource);
            final long extracted = rootStorage.extract(
                resource,
                needed,
                Action.EXECUTE,
                CompressionActors.ORDINARY_ONLY
            );
            if (extracted > 0) {
                requirements.remove(resource, extracted);
                internalStorage.add(resource, extracted);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean extractCompression(final List<CompressionStorageHandle> handles,
                                              final MutableResourceList requirements,
                                              final MutableResourceList internalStorage) {
        final Map<ResourceLocation, ResourceKey> resources = new HashMap<>();
        final Map<ResourceLocation, Long> amounts = new HashMap<>();
        for (final ResourceKey resource : requirements.getAll()) {
            if (resource instanceof ItemResource itemResource && itemResource.components().isEmpty()) {
                final ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemResource.item());
                if (isStoredBy(handles, id)) {
                    resources.put(id, resource);
                    amounts.put(id, requirements.get(resource));
                }
            }
        }
        if (amounts.isEmpty()) {
            return false;
        }
        final List<Pool<CompressionStorageHandle>> pools = handles.stream()
            .map(handle -> new Pool<>(
                handle,
                handle.storage().getFamily(),
                handle.storage().getEnabledItemIds(),
                handle.storage().getBaseUnits()
            ))
            .toList();
        final var plan = CompressionPoolPlanner.plan(pools, amounts);
        if (plan.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (final var allocation : plan.get()) {
            final ResourceKey resource = resources.get(allocation.itemId());
            final long extracted = allocation.key().access().extract(
                resource,
                allocation.amount(),
                Action.EXECUTE,
                Actor.EMPTY
            );
            if (extracted > 0) {
                requirements.remove(resource, extracted);
                internalStorage.add(resource, extracted);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isStoredBy(final List<CompressionStorageHandle> handles, final ResourceLocation itemId) {
        return handles.stream().anyMatch(handle -> handle.storage().isFormEnabled(itemId));
    }

    public record Result(boolean changed, boolean complete) {
    }
}
