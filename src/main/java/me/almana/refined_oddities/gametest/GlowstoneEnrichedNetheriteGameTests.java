package me.almana.refined_oddities.gametest;

import java.util.List;
import me.almana.refined_oddities.Refined_oddities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Refined_oddities.MODID)
@PrefixGameTestTemplate(false)
public final class GlowstoneEnrichedNetheriteGameTests {
    private GlowstoneEnrichedNetheriteGameTests() {
    }

    @GameTest(template = "empty")
    public static void glowstoneEnrichedNetheriteProgression(final GameTestHelper helper) {
        final Item enrichedNetherite = addonItem("glowstone_enriched_netherite");
        final Item rawProcessor = addonItem("raw_glowstone_enriched_netherite_processor");
        final Item processor = addonItem("glowstone_enriched_netherite_processor");

        final ItemStack enrichedResult = craft(helper, 3, 3, List.of(
            ItemStack.EMPTY, new ItemStack(Items.NETHERITE_INGOT), ItemStack.EMPTY,
            new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.GLOWSTONE), new ItemStack(Items.NETHERITE_INGOT),
            ItemStack.EMPTY, new ItemStack(Items.NETHERITE_INGOT), ItemStack.EMPTY
        ));
        helper.assertTrue(enrichedResult.is(enrichedNetherite), "Glowstone enriched netherite recipe output");
        helper.assertValueEqual(enrichedResult.getCount(), 4, "Glowstone enriched netherite output count");

        final ItemStack rawResult = craft(helper, 2, 2, List.of(
            new ItemStack(refinedStorageItem("processor_binding")),
            new ItemStack(enrichedNetherite),
            new ItemStack(refinedStorageItem("silicon")),
            new ItemStack(Items.REDSTONE)
        ));
        helper.assertTrue(rawResult.is(rawProcessor), "Raw processor recipe output");
        helper.assertValueEqual(rawResult.getCount(), 1, "Raw processor output count");

        final ItemStack processorResult = smelt(helper, rawProcessor);
        helper.assertTrue(processorResult.is(processor), "Smelted processor output");
        helper.assertValueEqual(processorResult.getCount(), 1, "Smelted processor output count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bulkStorageCraftingProgression(final GameTestHelper helper) {
        final Item bulkStoragePart = addonItem("bulk_storage_part");
        final Item bulkStorageDisk = addonItem("bulk_storage_disk");
        final Item enrichedNetherite = addonItem("glowstone_enriched_netherite");
        final Item processor = addonItem("glowstone_enriched_netherite_processor");
        final Item storagePart = refinedStorageItem("64k_storage_part");

        final ItemStack partResult = craft(helper, 3, 3, List.of(
            new ItemStack(processor), new ItemStack(enrichedNetherite), new ItemStack(processor),
            new ItemStack(storagePart), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(storagePart),
            new ItemStack(processor), new ItemStack(storagePart), new ItemStack(processor)
        ));
        helper.assertTrue(partResult.is(bulkStoragePart), "Bulk storage part recipe output");

        final ItemStack diskResult = craft(helper, 2, 1, List.of(
            new ItemStack(bulkStoragePart),
            new ItemStack(refinedStorageItem("storage_housing"))
        ));
        helper.assertTrue(diskResult.is(bulkStorageDisk), "Bulk storage disk recipe output");
        helper.succeed();
    }

    private static ItemStack craft(final GameTestHelper helper,
                                   final int width,
                                   final int height,
                                   final List<ItemStack> ingredients) {
        final CraftingInput input = CraftingInput.of(width, height, ingredients);
        return helper.getLevel().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
            .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    private static ItemStack smelt(final GameTestHelper helper, final Item inputItem) {
        final SingleRecipeInput input = new SingleRecipeInput(new ItemStack(inputItem));
        return helper.getLevel().getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, input, helper.getLevel())
            .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    private static Item addonItem(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Refined_oddities.MODID, path));
    }

    private static Item refinedStorageItem(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("refinedstorage", path));
    }
}
