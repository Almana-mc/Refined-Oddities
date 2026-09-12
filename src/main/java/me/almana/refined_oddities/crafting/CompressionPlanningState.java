package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.almana.refined_oddities.crafting.CompressionPoolPlanner.Pool;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class CompressionPlanningState {
    private final List<Pool<Integer>> pools;
    private final Map<ResourceLocation, Long> requirements;

    public CompressionPlanningState(final List<CompressionStorageHandle> handles) {
        this.pools = new ArrayList<>(handles.size());
        for (int i = 0; i < handles.size(); i++) {
            final var storage = handles.get(i).storage();
            pools.add(new Pool<>(i, storage.getFamily(), storage.getEnabledItemIds(), storage.getBaseUnits()));
        }
        this.requirements = new HashMap<>();
    }

    private CompressionPlanningState(final List<Pool<Integer>> pools,
                                     final Map<ResourceLocation, Long> requirements) {
        this.pools = List.copyOf(pools);
        this.requirements = new HashMap<>(requirements);
    }

    public long available(final ResourceKey resource) {
        final ResourceLocation itemId = itemId(resource);
        return itemId == null ? 0 : CompressionPoolPlanner.maximum(pools, requirements, itemId);
    }

    public boolean reserve(final ResourceKey resource, final long amount) {
        final ResourceLocation itemId = itemId(resource);
        if (itemId == null || amount <= 0) {
            return false;
        }
        final Map<ResourceLocation, Long> attempt = new HashMap<>(requirements);
        attempt.merge(itemId, amount, (current, added) -> Long.MAX_VALUE - current < added
            ? Long.MAX_VALUE
            : current + added);
        if (CompressionPoolPlanner.plan(pools, attempt).isEmpty()) {
            return false;
        }
        requirements.clear();
        requirements.putAll(attempt);
        return true;
    }

    public CompressionPlanningState copy() {
        return new CompressionPlanningState(pools, requirements);
    }

    private static ResourceLocation itemId(final ResourceKey resource) {
        if (!(resource instanceof ItemResource itemResource) || !itemResource.components().isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(itemResource.item());
    }
}
