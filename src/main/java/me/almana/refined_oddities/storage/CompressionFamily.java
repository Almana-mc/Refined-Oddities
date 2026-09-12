package me.almana.refined_oddities.storage;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record CompressionFamily(List<CompressionForm> forms) {
    public static final int MAX_FORMS = 25;

    public CompressionFamily {
        forms = List.copyOf(forms);
    }

    public static CompressionFamily empty() {
        return new CompressionFamily(List.of());
    }

    public static CompressionFamily single(final ResourceLocation itemId) {
        return new CompressionFamily(List.of(new CompressionForm(itemId, 1)));
    }

    public boolean isValidSnapshot() {
        if (forms.isEmpty()) {
            return true;
        }
        if (forms.size() > MAX_FORMS || forms.getFirst().weight() != 1) {
            return false;
        }
        final Set<ResourceLocation> ids = new HashSet<>();
        long previous = 0;
        for (final CompressionForm form : forms) {
            if (form.weight() <= previous
                || form.weight() > CompressionStorage.CAPACITY
                || !ids.add(form.itemId())
                || !isRegisteredItem(form.itemId())) {
                return false;
            }
            if (previous > 0) {
                final long ratio = form.weight() / previous;
                if (form.weight() % previous != 0 || ratio != 4 && ratio != 9) {
                    return false;
                }
            }
            previous = form.weight();
        }
        return true;
    }

    private static boolean isRegisteredItem(final ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR).isPresent();
    }

    public Optional<CompressionForm> find(final ResourceKey resource) {
        if (!(resource instanceof ItemResource itemResource) || !itemResource.components().isEmpty()) {
            return Optional.empty();
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemResource.item());
        return forms.stream().filter(form -> form.itemId().equals(id)).findFirst();
    }

    public ItemResource resource(final CompressionForm form) {
        final Item item = BuiltInRegistries.ITEM.get(form.itemId());
        return new ItemResource(item);
    }

    public boolean isConfigured() {
        return !forms.isEmpty();
    }
}
