package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.storage.DiskInventory;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import me.almana.refined_oddities.recipe.CompressionRecipeCatalog;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionForm;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.core.registries.BuiltInRegistries;
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
public final class CompressionStorageGameTests {
    private CompressionStorageGameTests() {
    }

    @GameTest(template = "empty")
    public static void diskResolvesThroughDiskDriveInventory(final GameTestHelper helper) {
        final ItemStack diskStack = new ItemStack(ModItems.COMPRESSION_STORAGE_DISK.get());
        diskStack.inventoryTick(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL), 0, false);
        final var repository = RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel());
        final CompressionStorage storage = (CompressionStorage) ModItems.COMPRESSION_STORAGE_DISK.get()
            .resolve(repository, diskStack)
            .orElseThrow();
        storage.configure(ironFamily());
        storage.insert(new ItemResource(Items.IRON_INGOT), 1, Action.EXECUTE, Actor.EMPTY);
        final DiskInventory inventory = new DiskInventory((ignored, slot) -> { }, 8);
        inventory.setStorageRepository(repository);
        inventory.setItem(0, diskStack);
        helper.assertTrue(inventory.resolve(0).orElseThrow() == storage, "Disk Drive resolved another storage");
        helper.assertValueEqual(inventory.resolve(0).orElseThrow().getStored(), 9L, "stored base units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskDriveCacheCountsEachFormOnce(final GameTestHelper helper) {
        final CompressionStorage storage = new CompressionStorage(0, ironFamily(), () -> { });
        final RootStorageImpl root = CompressionDiskDriveFixture.rootWith(storage);
        final ItemResource nugget = new ItemResource(Items.IRON_NUGGET);
        final ItemResource ingot = new ItemResource(Items.IRON_INGOT);
        final ItemResource block = new ItemResource(Items.IRON_BLOCK);

        helper.assertValueEqual(root.insert(block, 64, Action.EXECUTE, Actor.EMPTY), 64L, "inserted blocks");
        helper.assertValueEqual(root.get(block), 64L, "blocks after insertion");
        helper.assertValueEqual(root.get(ingot), 576L, "ingots after insertion");
        helper.assertValueEqual(root.get(nugget), 5_184L, "nuggets after insertion");

        helper.assertValueEqual(root.extract(ingot, 10, Action.EXECUTE, Actor.EMPTY), 10L, "crafted ingots");
        helper.assertValueEqual(root.extract(nugget, 20, Action.EXECUTE, Actor.EMPTY), 20L, "crafted nuggets");
        helper.assertValueEqual(root.get(block), 62L, "blocks after crafting");
        helper.assertValueEqual(root.get(ingot), 563L, "ingots after crafting");
        helper.assertValueEqual(root.get(nugget), 5_074L, "nuggets after crafting");

        helper.assertValueEqual(root.extract(block, 126, Action.EXECUTE, Actor.EMPTY), 62L, "remaining blocks");
        helper.assertValueEqual(root.get(block), 0L, "phantom blocks");
        helper.assertValueEqual(root.get(ingot), 5L, "remaining ingots");
        helper.assertValueEqual(root.get(nugget), 52L, "remaining nuggets");

        helper.assertValueEqual(root.insert(block, 62, Action.EXECUTE, Actor.EMPTY), 62L, "reinserted blocks");
        helper.assertValueEqual(root.get(block), 62L, "blocks after reinsertion");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void activeNetworkCacheReceivesEveryForm(final GameTestHelper helper) {
        final CompressionStorage storage = new CompressionStorage(0, ironFamily(), () -> { });
        final RootStorageImpl root = CompressionDiskDriveFixture.activeNetworkWith(storage);
        final ItemResource nugget = new ItemResource(Items.IRON_NUGGET);
        final ItemResource ingot = new ItemResource(Items.IRON_INGOT);
        final ItemResource block = new ItemResource(Items.IRON_BLOCK);
        final Map<ResourceKey, Long> updates = new HashMap<>();
        root.addListener(change -> updates.merge(change.resource(), change.change(), Long::sum));

        helper.assertValueEqual(root.insert(block, 64, Action.EXECUTE, Actor.EMPTY), 64L, "inserted blocks");
        helper.assertValueEqual(root.get(block), 64L, "network blocks");
        helper.assertValueEqual(root.get(ingot), 576L, "network ingots");
        helper.assertValueEqual(root.get(nugget), 5_184L, "network nuggets");
        helper.assertValueEqual(updates.get(block), 64L, "block grid update");
        helper.assertValueEqual(updates.get(ingot), 576L, "ingot grid update");
        helper.assertValueEqual(updates.get(nugget), 5_184L, "nugget grid update");

        updates.clear();
        helper.assertValueEqual(root.extract(ingot, 1, Action.EXECUTE, Actor.EMPTY), 1L, "extracted ingot");
        helper.assertValueEqual(root.get(block), 63L, "network blocks after extraction");
        helper.assertValueEqual(root.get(ingot), 575L, "network ingots after extraction");
        helper.assertValueEqual(root.get(nugget), 5_175L, "network nuggets after extraction");
        helper.assertValueEqual(updates.get(block), -1L, "block extraction grid update");
        helper.assertValueEqual(updates.get(ingot), -1L, "ingot extraction grid update");
        helper.assertValueEqual(updates.get(nugget), -9L, "nugget extraction grid update");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskDriveExposesOnlyEnabledForms(final GameTestHelper helper) {
        final CompressionStorage storage = new CompressionStorage(0, ironFamily(), () -> { });
        storage.toggleForm(BuiltInRegistries.ITEM.getKey(Items.IRON_NUGGET));
        final RootStorageImpl root = CompressionDiskDriveFixture.rootWith(storage);
        final ItemResource nugget = new ItemResource(Items.IRON_NUGGET);
        final ItemResource ingot = new ItemResource(Items.IRON_INGOT);
        final ItemResource block = new ItemResource(Items.IRON_BLOCK);

        helper.assertValueEqual(
            storage.toggleForm(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT)),
            CompressionStorage.ConfigurationResult.CONNECTED,
            "connected toggle result"
        );
        helper.assertValueEqual(root.insert(block, 1, Action.EXECUTE, Actor.EMPTY), 1L, "inserted block");
        helper.assertValueEqual(root.get(block), 1L, "visible blocks");
        helper.assertValueEqual(root.get(ingot), 9L, "visible ingots");
        helper.assertValueEqual(root.get(nugget), 0L, "disabled nuggets");
        helper.assertValueEqual(root.insert(nugget, 9, Action.EXECUTE, Actor.EMPTY), 0L, "inserted disabled nuggets");
        helper.assertValueEqual(root.extract(nugget, 9, Action.EXECUTE, Actor.EMPTY), 0L, "extracted disabled nuggets");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reloadCatalogFindsVanillaIronFamily(final GameTestHelper helper) {
        final var family = CompressionRecipeCatalog.INSTANCE.familyFor(Items.IRON_INGOT).orElseThrow();
        helper.assertValueEqual(family.forms().size(), 3, "iron family size");
        helper.assertValueEqual(family.forms().getFirst().weight(), 1L, "nugget weight");
        helper.assertValueEqual(family.forms().get(1).weight(), 9L, "ingot weight");
        helper.assertValueEqual(family.forms().get(2).weight(), 81L, "block weight");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ghostSelectionDoesNotConsumeCarriedStack(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.COMPRESSION_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        final var menu = new CompressionConfigurationMenu(
            1,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        menu.setCarried(new ItemStack(Items.IRON_BLOCK, 32));

        menu.clicked(0, 0, ClickType.PICKUP, player);

        helper.assertValueEqual(menu.getCarried().getCount(), 32, "carried stack count");
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.IRON_BLOCK), "Ghost slot did not show iron block");
        final CompressionStorage storage = (CompressionStorage) ModItems.COMPRESSION_STORAGE_DISK.get()
            .resolve(RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel()), diskStack)
            .orElseThrow();
        helper.assertValueEqual(storage.getConfiguredItemId().orElseThrow(),
            BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK), "configured item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftClickSelectsWithoutMovingInventoryStack(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.COMPRESSION_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 24));
        final var menu = new CompressionConfigurationMenu(
            2,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );

        menu.quickMoveStack(player, 1 + CompressionFamily.MAX_FORMS);

        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 24, "inventory stack count");
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.IRON_INGOT), "Ghost slot did not show iron ingot");
        final CompressionStorage storage = (CompressionStorage) ModItems.COMPRESSION_STORAGE_DISK.get()
            .resolve(RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel()), diskStack)
            .orElseThrow();
        helper.assertValueEqual(storage.getConfiguredItemId().orElseThrow(),
            BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), "configured item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jeiSelectionUsesMenuButtonPath(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.COMPRESSION_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        final var menu = new CompressionConfigurationMenu(
            3,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        final int buttonId = CompressionConfigurationMenu.SELECT_ITEM_BUTTON_OFFSET
            + BuiltInRegistries.ITEM.getId(Items.IRON_BLOCK);

        helper.assertTrue(menu.clickMenuButton(player, buttonId), "JEI selection was rejected");

        final CompressionStorage storage = (CompressionStorage) ModItems.COMPRESSION_STORAGE_DISK.get()
            .resolve(RefinedStorageApi.INSTANCE.getStorageRepository(helper.getLevel()), diskStack)
            .orElseThrow();
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.IRON_BLOCK), "Ghost slot did not show JEI item");
        helper.assertValueEqual(storage.getConfiguredItemId().orElseThrow(),
            BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK), "configured item");
        final var reopened = new CompressionConfigurationMenu(
            4,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        helper.assertTrue(reopened.getSlot(0).getItem().is(Items.IRON_BLOCK), "Reopened slot lost exact item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void normalInventoryClickStillMovesStack(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.COMPRESSION_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 5));
        final var menu = new CompressionConfigurationMenu(
            5,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );

        menu.clicked(1 + CompressionFamily.MAX_FORMS, 0, ClickType.PICKUP, player);

        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Inventory slot was not emptied");
        helper.assertValueEqual(menu.getCarried().getCount(), 5, "carried stack count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void configurationMenuShowsAndTogglesEveryForm(final GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack diskStack = new ItemStack(ModItems.COMPRESSION_STORAGE_DISK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, diskStack);
        diskStack.inventoryTick(helper.getLevel(), player, 0, true);
        final var menu = new CompressionConfigurationMenu(
            6,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        menu.setCarried(new ItemStack(Items.IRON_INGOT));

        menu.clicked(0, 0, ClickType.PICKUP, player);

        helper.assertValueEqual(menu.stateCount(), 3, "visible form count");
        helper.assertTrue(menu.getStateStack(0).is(Items.IRON_NUGGET), "missing nugget form");
        helper.assertTrue(menu.getStateStack(1).is(Items.IRON_INGOT), "missing ingot form");
        helper.assertTrue(menu.getStateStack(2).is(Items.IRON_BLOCK), "missing block form");
        helper.assertTrue(menu.isStateEnabled(0), "nugget form started disabled");
        helper.assertTrue(menu.clickMenuButton(
            player,
            CompressionConfigurationMenu.TOGGLE_FORM_BUTTON_OFFSET
        ), "nugget toggle was rejected");
        helper.assertTrue(!menu.isStateEnabled(0), "nugget form stayed enabled");

        final var reopened = new CompressionConfigurationMenu(
            7,
            player.getInventory(),
            InteractionHand.MAIN_HAND
        );
        helper.assertTrue(!reopened.isStateEnabled(0), "disabled form was not persisted");
        helper.succeed();
    }

    private static CompressionFamily ironFamily() {
        return new CompressionFamily(List.of(
            new CompressionForm(BuiltInRegistries.ITEM.getKey(Items.IRON_NUGGET), 1),
            new CompressionForm(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), 9),
            new CompressionForm(BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK), 81)
        ));
    }
}
