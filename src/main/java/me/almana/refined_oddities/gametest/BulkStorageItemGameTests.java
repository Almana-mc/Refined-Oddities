package me.almana.refined_oddities.gametest;

import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.content.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
}
