package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.List;
import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.content.ModDataComponents;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Refined_oddities.MODID)
@PrefixGameTestTemplate(false)
public final class BulkStorageItemGameTests {
    private BulkStorageItemGameTests() {
    }

    @GameTest(template = "empty")
    public static void diskDisassemblesIntoPart(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack disk = new ItemStack(ModItems.BULK_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, disk);
        player.setShiftKeyDown(true);

        final ItemStack result = ModItems.BULK_STORAGE_DISK.get()
            .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
            .getObject();

        helper.assertTrue(result.is(ModItems.BULK_STORAGE_PART.get()), "disk did not return a part");
        helper.assertValueEqual(result.getCount(), 1, "returned part count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskStackTracksEnabledFilter(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack disk = new ItemStack(ModItems.BULK_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, disk);
        disk.inventoryTick(helper.getLevel(), player, 0, true);
        final CompressionStorage storage = (CompressionStorage) ModItems.BULK_STORAGE_DISK.get()
            .resolve(RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel()), disk)
            .orElseThrow();

        storage.configure(CompressionFamily.single(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT)));
        disk.inventoryTick(helper.getLevel(), player, 0, true);
        helper.assertTrue(
            List.of(new ItemResource(Items.IRON_INGOT)).equals(disk.get(ModDataComponents.BULK_FILTER.get())),
            "filter not synced to stack"
        );

        storage.clearFamily();
        disk.inventoryTick(helper.getLevel(), player, 0, true);
        helper.assertTrue(!disk.has(ModDataComponents.BULK_FILTER.get()), "cleared filter still on stack");
        helper.succeed();
    }
}
