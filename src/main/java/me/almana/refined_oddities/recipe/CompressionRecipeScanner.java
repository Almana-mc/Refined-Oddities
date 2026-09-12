package me.almana.refined_oddities.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class CompressionRecipeScanner {
    private CompressionRecipeScanner() {
    }

    public static CompressionFamilyDetector.Result scan(final RecipeManager recipeManager,
                                                        final HolderLookup.Provider registries) {
        final List<CompressionLink> compression = new ArrayList<>();
        final List<CompressionLink> decompression = new ArrayList<>();
        final Set<ResourceLocation> rejected = new HashSet<>();
        for (final RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) {
                continue;
            }
            if (recipe instanceof ShapedRecipe shaped && shaped.getWidth() == shaped.getHeight()) {
                if (shaped.getWidth() == 2 || shaped.getWidth() == 3) {
                    scanCompression(shaped, registries, compression, rejected);
                } else if (shaped.getWidth() == 1) {
                    scanDecompression(shaped, registries, decompression, rejected);
                }
            } else if (recipe instanceof ShapelessRecipe shapeless && shapeless.getIngredients().size() == 1) {
                scanDecompression(shapeless, registries, decompression, rejected);
            }
        }
        return CompressionFamilyDetector.detect(compression, decompression, rejected);
    }

    private static void scanCompression(final ShapedRecipe recipe,
                                        final HolderLookup.Provider registries,
                                        final List<CompressionLink> links,
                                        final Set<ResourceLocation> rejected) {
        final ItemStack output = recipe.getResultItem(registries);
        final int ratio = recipe.getWidth() * recipe.getHeight();
        final List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.stream().anyMatch(ingredient -> ingredient.isEmpty() || ingredient.hasNoItems())) {
            return;
        }
        final List<ItemStack> inputs = resolvedInputs(ingredients);
        if (inputs.size() != ratio || !allSameItem(inputs)) {
            return;
        }
        if (recipe.getSerializer() != RecipeSerializer.SHAPED_RECIPE
            || output.getCount() != 1
            || !output.isComponentsPatchEmpty()
            || ingredients.stream().anyMatch(Ingredient::isCustom)
            || inputs.stream().anyMatch(stack -> !stack.isComponentsPatchEmpty())
            || hasRemainder(recipe, CraftingInput.of(recipe.getWidth(), recipe.getHeight(), inputs))) {
            rejectKnownItems(ingredients, output, rejected);
            return;
        }
        links.add(new CompressionLink(id(inputs.getFirst().getItem()), id(output.getItem()), ratio));
    }

    private static void scanDecompression(final CraftingRecipe recipe,
                                          final HolderLookup.Provider registries,
                                          final List<CompressionLink> links,
                                          final Set<ResourceLocation> rejected) {
        final ItemStack output = recipe.getResultItem(registries);
        final int ratio = output.getCount();
        final List<ItemStack> inputs = exactInputs(recipe.getIngredients());
        final boolean standardRecipe = recipe.getSerializer() == RecipeSerializer.SHAPED_RECIPE
            || recipe.getSerializer() == RecipeSerializer.SHAPELESS_RECIPE;
        if (!standardRecipe
            || ratio != 4 && ratio != 9
            || !output.isComponentsPatchEmpty()
            || inputs.size() != 1
            || hasRemainder(recipe, CraftingInput.of(1, 1, inputs))) {
            if (ratio == 4 || ratio == 9) {
                rejectKnownItems(recipe.getIngredients(), output, rejected);
            }
            return;
        }
        links.add(new CompressionLink(id(output.getItem()), id(inputs.getFirst().getItem()), ratio));
    }

    private static List<ItemStack> exactInputs(final List<Ingredient> ingredients) {
        final List<ItemStack> inputs = resolvedInputs(ingredients);
        if (ingredients.stream().anyMatch(Ingredient::isCustom)
            || inputs.stream().anyMatch(stack -> !stack.isComponentsPatchEmpty())) {
            return List.of();
        }
        return inputs;
    }

    private static List<ItemStack> resolvedInputs(final List<Ingredient> ingredients) {
        final List<ItemStack> inputs = new ArrayList<>(ingredients.size());
        for (final Ingredient ingredient : ingredients) {
            final ItemStack[] matches = ingredient.getItems();
            if (matches.length != 1) {
                return List.of();
            }
            inputs.add(matches[0].copyWithCount(1));
        }
        return inputs;
    }

    private static boolean allSameItem(final List<ItemStack> inputs) {
        final Item item = inputs.getFirst().getItem();
        return inputs.stream().allMatch(stack -> stack.is(item));
    }

    private static boolean hasRemainder(final CraftingRecipe recipe, final CraftingInput input) {
        return recipe.getRemainingItems(input).stream().anyMatch(stack -> !stack.isEmpty());
    }

    private static void rejectKnownItems(final List<Ingredient> ingredients,
                                         final ItemStack output,
                                         final Set<ResourceLocation> rejected) {
        final List<ResourceLocation> inputs = ingredients.stream()
            .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
            .filter(stack -> !stack.isEmpty())
            .map(ItemStack::getItem)
            .map(CompressionRecipeScanner::id)
            .toList();
        if (!output.isEmpty()) {
            rejected.add(id(output.getItem()));
        }
        rejected.addAll(inputs);
    }

    private static ResourceLocation id(final Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

}
