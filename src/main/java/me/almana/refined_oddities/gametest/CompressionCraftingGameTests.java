package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.StepBehavior;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskImpl;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskListener;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskPlan;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskSnapshot;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskState;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionForm;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Refined_oddities.MODID)
@PrefixGameTestTemplate(false)
public final class CompressionCraftingGameTests {
    private static final ItemResource NUGGET = new ItemResource(Items.IRON_NUGGET);
    private static final ItemResource INGOT = new ItemResource(Items.IRON_INGOT);
    private static final ItemResource BLOCK = new ItemResource(Items.IRON_BLOCK);

    private CompressionCraftingGameTests() {
    }

    @GameTest(template = "empty")
    public static void craftingStateUsesOneCanonicalPool(final GameTestHelper helper) throws ReflectiveOperationException {
        final RootStorageImpl root = rootWithCompression(90);
        final Class<?> type = Class.forName(
            "com.refinedmods.refinedstorage.api.autocrafting.calculation.CraftingState"
        );
        final Method of = accessible(type.getDeclaredMethod("of", RootStorage.class));
        final Method copy = accessible(type.getDeclaredMethod("copy"));
        final Method getResource = accessible(type.getDeclaredMethod("getResource", ResourceKey.class));
        final Method extract = accessible(type.getDeclaredMethod("extractFromStorage", ResourceKey.class, long.class));

        final Object state = of.invoke(null, root);
        final Object copied = copy.invoke(state);
        helper.assertValueEqual(amount(getResource.invoke(state, BLOCK)), 1L, "block preview");
        helper.assertValueEqual(amount(getResource.invoke(state, INGOT)), 10L, "ingot preview");
        helper.assertValueEqual(amount(getResource.invoke(state, NUGGET)), 90L, "nugget preview");

        extract.invoke(copied, BLOCK, 1L);
        helper.assertValueEqual(amount(getResource.invoke(copied, INGOT)), 1L, "copied pool remainder");
        helper.assertValueEqual(amount(getResource.invoke(state, BLOCK)), 1L, "original pool unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taskExtractsOrdinaryBeforeMixedCompressionAndCancels(final GameTestHelper helper) {
        final RootStorageImpl root = rootWithCompression(90);
        final StorageImpl ordinary = new StorageImpl();
        ordinary.insert(INGOT, 2, Action.EXECUTE, Actor.EMPTY);
        root.addSource(ordinary);
        root.sortSources();

        final TaskImpl task = new TaskImpl(plan(List.of(
            new ResourceAmount(INGOT, 3),
            new ResourceAmount(BLOCK, 1)
        )), Actor.EMPTY, false);
        helper.assertTrue(task.step(root, null, StepBehavior.DEFAULT, TaskListener.EMPTY), "task did not extract");
        helper.assertValueEqual(task.getState(), TaskState.RUNNING, "task state");
        assertTaskStorage(helper, task.createSnapshot().copyInternalStorage(), 3, 1);
        helper.assertValueEqual(ordinary.getStored(), 0L, "ordinary stock reserved first");
        helper.assertValueEqual(root.get(INGOT), 0L, "canonical ingots after extraction");
        helper.assertValueEqual(root.get(BLOCK), 0L, "blocks after extraction");
        helper.assertValueEqual(root.get(NUGGET), 0L, "base units after extraction");

        task.cancel();
        helper.assertTrue(task.step(root, null, StepBehavior.DEFAULT, TaskListener.EMPTY), "task did not return stock");
        helper.assertValueEqual(task.getState(), TaskState.COMPLETED, "cancelled task state");
        helper.assertValueEqual(root.get(INGOT), 12L, "returned canonical ingots");
        helper.assertValueEqual(root.get(BLOCK), 1L, "returned blocks");
        helper.assertValueEqual(root.get(NUGGET), 108L, "returned base units");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void restoredTaskUsesCanonicalExtraction(final GameTestHelper helper) {
        final RootStorageImpl root = rootWithCompression(90);
        final MutableResourceList requirements = MutableResourceListImpl.create();
        requirements.add(INGOT, 1);
        requirements.add(BLOCK, 1);
        final TaskSnapshot snapshot = new TaskSnapshot(
            TaskId.create(), BLOCK, 1, Actor.EMPTY, false, System.currentTimeMillis(),
            Map.of(), List.of(), requirements, MutableResourceListImpl.create(),
            TaskState.EXTRACTING_INITIAL_RESOURCES, false
        );
        final TaskImpl restored = new TaskImpl(snapshot);

        helper.assertTrue(restored.step(root, null, StepBehavior.DEFAULT, TaskListener.EMPTY), "restored task did not extract");
        helper.assertValueEqual(restored.getState(), TaskState.RUNNING, "restored task state");
        assertTaskStorage(helper, restored.createSnapshot().copyInternalStorage(), 1, 1);
        helper.assertValueEqual(root.getStored(), 0L, "compression pool remainder");
        helper.succeed();
    }

    private static TaskPlan plan(final List<ResourceAmount> requirements) {
        return new TaskPlan(BLOCK, 1, null, Map.<Pattern, TaskPlan.PatternPlan>of(), requirements);
    }

    private static RootStorageImpl rootWithCompression(final long baseUnits) {
        final CompressionStorage compression = new CompressionStorage(baseUnits, ironFamily(), () -> { });
        return CompressionDiskDriveFixture.rootWith(compression);
    }

    private static void assertTaskStorage(final GameTestHelper helper,
                                          final MutableResourceList storage,
                                          final long ingots,
                                          final long blocks) {
        helper.assertValueEqual(storage.get(INGOT), ingots, "task ingots");
        helper.assertValueEqual(storage.get(BLOCK), blocks, "task blocks");
    }

    private static CompressionFamily ironFamily() {
        return new CompressionFamily(List.of(
            new CompressionForm(BuiltInRegistries.ITEM.getKey(Items.IRON_NUGGET), 1),
            new CompressionForm(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT), 9),
            new CompressionForm(BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK), 81)
        ));
    }

    private static Method accessible(final Method method) {
        method.setAccessible(true);
        return method;
    }

    private static long amount(final Object resourceState) throws ReflectiveOperationException {
        final Method method = accessible(resourceState.getClass().getDeclaredMethod("inStorage"));
        return (long) method.invoke(resourceState);
    }
}
