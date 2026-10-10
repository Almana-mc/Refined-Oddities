package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceContainer;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.lang.reflect.Method;
import me.almana.refined_oddities.Refined_oddities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Refined_oddities.MODID)
@PrefixGameTestTemplate(false)
public final class PatternGridGameTests {
    private PatternGridGameTests() {
    }

    @GameTest(template = "empty")
    public static void processingSlotsAcceptMoreThanAStack(final GameTestHelper helper) {
        final PatternGridBlockEntity grid = new PatternGridBlockEntity(
            BlockPos.ZERO,
            Blocks.INSTANCE.getPatternGrid().getDefault().defaultBlockState()
        );
        final ItemResource cobblestone = new ItemResource(Items.COBBLESTONE);

        for (final String getter : new String[] {"getProcessingInput", "getProcessingOutput"}) {
            final ResourceContainer container = container(grid, getter);
            container.set(0, new ResourceAmount(cobblestone, 64));
            container.setAmount(0, 1000);
            helper.assertValueEqual(container.getAmount(0), 1000L, getter + " amount");
        }
        helper.succeed();
    }

    private static ResourceContainer container(final PatternGridBlockEntity grid, final String getter) {
        try {
            final Method method = PatternGridBlockEntity.class.getDeclaredMethod(getter);
            method.setAccessible(true);
            return (ResourceContainer) method.invoke(grid);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
