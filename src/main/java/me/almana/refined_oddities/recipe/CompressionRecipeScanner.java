package me.almana.refined_oddities.recipe;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.slf4j.Logger;

public final class CompressionRecipeScanner {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CompressionRecipeScanner() {
    }

    public static CompressionFamilyDetector.Result scan(final RecipeManager recipeManager,
                                                        final HolderLookup.Provider registries) {
        final List<CompressionLink> compression = new ArrayList<>();
        final List<CompressionLink> decompression = new ArrayList<>();
        for (final RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }
            // third-party recipes may throw
            try {
                scanRecipe(recipe, registries, compression, decompression);
            } catch (final RuntimeException exception) {
                LOGGER.debug("Skipping recipe {} during compression scan", holder.id(), exception);
            }
        }
        return CompressionFamilyDetector.detect(compression, decompression, Set.of());
    }

    private static void scanRecipe(final CraftingRecipe recipe,
                                   final HolderLookup.Provider registries,
                                   final List<CompressionLink> compression,
                                   final List<CompressionLink> decompression) {
        if (recipe instanceof ShapedRecipe shaped && shaped.getWidth() == shaped.getHeight()) {
            if (shaped.getWidth() == 2 || shaped.getWidth() == 3) {
                scanCompression(recipe, shaped.getWidth(), registries, compression);
            } else if (shaped.getWidth() == 1) {
                scanDecompression(recipe, registries, decompression);
            }
        } else if (recipe instanceof ShapelessRecipe) {
            final int size = recipe.getIngredients().size();
            if (size == 4 || size == 9) {
                scanCompression(recipe, size == 4 ? 2 : 3, registries, compression);
            } else if (size == 1) {
                scanDecompression(recipe, registries, decompression);
            }
        }
    }

    private static void scanCompression(final CraftingRecipe recipe,
                                        final int side,
                                        final HolderLookup.Provider registries,
                                        final List<CompressionLink> links) {
        final int ratio = side * side;
        final List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.stream().anyMatch(ingredient -> ingredient.isEmpty() || ingredient.hasNoItems())) {
            return;
        }
        Arrays.stream(ingredients.getFirst().getItems())
            .filter(ItemStack::isComponentsPatchEmpty)
            .filter(input -> ingredients.stream().allMatch(ingredient -> ingredient.test(input)))
            .map(input -> input.copyWithCount(1))
            .forEach(input -> {
                final ItemStack output = plainResult(recipe,
                    CraftingInput.of(side, side, Collections.nCopies(ratio, input)), registries);
                if (output.getCount() == 1) {
                    links.add(new CompressionLink(id(input.getItem()), id(output.getItem()), ratio));
                }
            });
    }

    private static void scanDecompression(final CraftingRecipe recipe,
                                          final HolderLookup.Provider registries,
                                          final List<CompressionLink> links) {
        Arrays.stream(recipe.getIngredients().getFirst().getItems())
            .filter(ItemStack::isComponentsPatchEmpty)
            .map(input -> input.copyWithCount(1))
            .forEach(input -> {
                final ItemStack output = plainResult(recipe, CraftingInput.of(1, 1, List.of(input)), registries);
                if (output.getCount() == 4 || output.getCount() == 9) {
                    links.add(new CompressionLink(id(output.getItem()), id(input.getItem()), output.getCount()));
                }
            });
    }

    private static ItemStack plainResult(final CraftingRecipe recipe,
                                         final CraftingInput input,
                                         final HolderLookup.Provider registries) {
        final ItemStack output = recipe.assemble(input, registries);
        final boolean plain = output.isComponentsPatchEmpty()
            && recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty);
        return plain ? output : ItemStack.EMPTY;
    }

    private static ResourceLocation id(final Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
