package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Refined_oddities.MODID)
@PrefixGameTestTemplate(false)
public final class CompressionConfigurationMenuGameTests {
    private CompressionConfigurationMenuGameTests() {
    }

    @GameTest(template = "empty")
    public static void compactInventoryLayoutMovesPlayerSlotsUp(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.BULK_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        final var menu = new CompressionConfigurationMenu(
            1,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        final int firstPlayerSlot = 1 + CompressionFamily.MAX_FORMS;

        helper.assertValueEqual(menu.slots.get(firstPlayerSlot).y, 121, "first inventory row y");
        helper.assertValueEqual(menu.slots.get(firstPlayerSlot + 27).y, 180, "hotbar row y");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void removedClearActionIsRejected(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.BULK_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        final var menu = new CompressionConfigurationMenu(
            1,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );

        helper.assertTrue(!menu.clickMenuButton(player, 0), "removed clear action was accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyCursorClearsConfiguredSelector(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.BULK_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        final var menu = new CompressionConfigurationMenu(
            1,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        menu.setCarried(new ItemStack(Items.IRON_INGOT));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);

        menu.clicked(0, 0, ClickType.PICKUP, player);

        final CompressionStorage storage = (CompressionStorage) ModItems.BULK_STORAGE_DISK.get()
            .resolve(RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel()), diskStack)
            .orElseThrow();
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "selector was not cleared");
        helper.assertValueEqual(menu.stateCount(), 0, "visible form count");
        helper.assertTrue(storage.getConfiguredResource().isEmpty(), "configured item was not cleared");
        helper.succeed();
    }
}
