package me.almana.refined_oddities.recipe;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.almana.refined_oddities.storage.CompressionFamily;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class CompressionRecipeCatalog {
    public static final CompressionRecipeCatalog INSTANCE = new CompressionRecipeCatalog();

    private volatile CompressionFamilyDetector.Result result = CompressionFamilyDetector.detect(
        List.of(),
        List.of(),
        Set.of()
    );

    private CompressionRecipeCatalog() {
    }

    public void replace(final CompressionFamilyDetector.Result newResult) {
        result = newResult;
    }

    public Optional<CompressionFamily> familyFor(final Item item) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return result.familyFor(id);
    }
}
