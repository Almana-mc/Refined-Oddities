package me.almana.refined_oddities.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionForm;
import net.minecraft.resources.ResourceLocation;

public final class CompressionPoolPlanner {
    private CompressionPoolPlanner() {
    }

    public static <T> Optional<List<Allocation<T>>> plan(final List<Pool<T>> pools,
                                                        final Map<ResourceLocation, Long> requirements) {
        final Map<ResourceLocation, Long> remaining = new HashMap<>(requirements);
        final Map<CompressionFamily, List<Pool<T>>> grouped = new LinkedHashMap<>();
        pools.forEach(pool -> grouped.computeIfAbsent(pool.family(), key -> new ArrayList<>()).add(pool));
        final List<Allocation<T>> allocations = new ArrayList<>();
        for (final Map.Entry<CompressionFamily, List<Pool<T>>> entry : grouped.entrySet()) {
            allocateFamily(entry.getKey(), entry.getValue(), remaining, allocations);
        }
        return remaining.values().stream().allMatch(amount -> amount == 0)
            ? Optional.of(List.copyOf(allocations))
            : Optional.empty();
    }

    private static <T> void allocateFamily(final CompressionFamily family,
                                           final List<Pool<T>> pools,
                                           final Map<ResourceLocation, Long> remaining,
                                           final List<Allocation<T>> allocations) {
        final long[] units = pools.stream().mapToLong(Pool::units).toArray();
        final List<CompressionForm> forms = family.forms().reversed();
        for (final CompressionForm form : forms) {
            long needed = remaining.getOrDefault(form.itemId(), 0L);
            while (needed > 0) {
                final int index = fullestPool(units, pools, form.itemId(), form.weight());
                if (index < 0) {
                    break;
                }
                final long available = units[index] / form.weight();
                final long taken = Math.min(needed, available);
                allocations.add(new Allocation<>(pools.get(index).key(), form.itemId(), taken));
                units[index] -= taken * form.weight();
                needed -= taken;
            }
            remaining.put(form.itemId(), needed);
        }
    }

    private static <T> int fullestPool(final long[] units,
                                       final List<Pool<T>> pools,
                                       final ResourceLocation itemId,
                                       final long weight) {
        int result = -1;
        for (int i = 0; i < units.length; i++) {
            if (pools.get(i).enabledItemIds().contains(itemId)
                && units[i] >= weight
                && (result < 0 || units[i] > units[result])) {
                result = i;
            }
        }
        return result;
    }

    public static long maximum(final List<? extends Pool<?>> pools,
                               final Map<ResourceLocation, Long> existingRequirements,
                               final ResourceLocation itemId) {
        long high = 0;
        for (final Pool<?> pool : pools) {
            if (!pool.enabledItemIds().contains(itemId)) {
                continue;
            }
            final long weight = pool.family().forms().stream()
                .filter(form -> form.itemId().equals(itemId))
                .mapToLong(CompressionForm::weight)
                .findFirst()
                .orElse(0);
            if (weight > 0) {
                final long available = pool.units() / weight;
                high = Long.MAX_VALUE - high < available ? Long.MAX_VALUE : high + available;
            }
        }
        long low = 0;
        while (low < high) {
            final long middle = (low + high + 1) >>> 1;
            final Map<ResourceLocation, Long> attempt = new HashMap<>(existingRequirements);
            attempt.merge(itemId, middle, CompressionPoolPlanner::saturatedAdd);
            if (canPlan(pools, attempt)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private static boolean canPlan(final List<? extends Pool<?>> pools,
                                   final Map<ResourceLocation, Long> requirements) {
        final List<Pool<Object>> erased = pools.stream()
            .map(pool -> new Pool<>((Object) pool.key(), pool.family(), pool.enabledItemIds(), pool.units()))
            .toList();
        return plan(erased, requirements).isPresent();
    }

    private static long saturatedAdd(final long first, final long second) {
        return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
    }

    public record Pool<T>(T key,
                          CompressionFamily family,
                          List<ResourceLocation> enabledItemIds,
                          long units) {
        public Pool {
            enabledItemIds = List.copyOf(enabledItemIds);
            if (units < 0) {
                throw new IllegalArgumentException("Units must not be negative");
            }
        }
    }

    public record Allocation<T>(T key, ResourceLocation itemId, long amount) {
    }
}
