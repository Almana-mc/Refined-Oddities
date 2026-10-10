package me.almana.refined_oddities.gametest;

import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import me.almana.refined_oddities.recipe.CompressionFamilyDetector;
import me.almana.refined_oddities.recipe.CompressionRecipeCatalog;
import me.almana.refined_oddities.recipe.CompressionRecipeScanner;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionForm;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

@GameTestHolder(Refined_oddities.MODID)
@PrefixGameTestTemplate(false)
public final class CompressionPackAuditGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CompressionPackAuditGameTests() {
    }

    @GameTest(template = "empty")
    public static void everyCraftableChainIsStorable(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final long start = System.currentTimeMillis();
        final Map<Edge, String> edges = new LinkedHashMap<>();
        final List<String> notes = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            try {
                collectEdges(level, item, edges, notes);
            } catch (final RuntimeException exception) {
                notes.add("ERROR " + id(item) + " " + exception);
            }
        }
        final var fresh = CompressionRecipeScanner.scan(level.getRecipeManager(), level.registryAccess());
        final List<String> missing = missingEdges(edges, fresh);
        final List<String> storage = storageFailures(edges.keySet());
        final String summary = "items=" + BuiltInRegistries.ITEM.size() + " pairs=" + edges.size()
            + " missing=" + missing.size() + " storageFailures=" + storage.size()
            + " elapsedMs=" + (System.currentTimeMillis() - start);
        final List<String> report = new ArrayList<>(List.of(summary));
        report.addAll(missing);
        report.addAll(storage);
        report.addAll(notes);
        report.addAll(phantoms(edges.keySet(), fresh));
        report.addAll(familyLines(edges.keySet()));
        write(level, report);
        helper.assertTrue(missing.isEmpty() && storage.isEmpty(), summary);
        helper.succeed();
    }

    private static void collectEdges(final ServerLevel level,
                                     final Item item,
                                     final Map<Edge, String> edges,
                                     final List<String> notes) {
        if (item == Items.AIR) {
            return;
        }
        final ItemStack stack = new ItemStack(item);
        for (final int side : new int[] {2, 3}) {
            final int ratio = side * side;
            final CraftingInput grid = CraftingInput.of(side, side, Collections.nCopies(ratio, stack));
            final Set<Item> outputs = new HashSet<>();
            for (final RecipeHolder<CraftingRecipe> up : recipesFor(level, grid)) {
                final ItemStack result = plainResult(level, up.value(), grid, 1);
                if (result.isEmpty()) {
                    continue;
                }
                outputs.add(result.getItem());
                final CraftingInput single = CraftingInput.of(1, 1, List.of(result));
                for (final RecipeHolder<CraftingRecipe> down : recipesFor(level, single)) {
                    if (plainResult(level, down.value(), single, ratio).is(item)) {
                        edges.putIfAbsent(new Edge(item, result.getItem(), ratio), describe(up) + " " + describe(down));
                    }
                }
            }
            if (outputs.size() > 1) {
                notes.add("CONFLICT " + id(item) + " x" + ratio + " -> "
                    + outputs.stream().map(CompressionPackAuditGameTests::id).toList());
            }
        }
    }

    private static List<String> missingEdges(final Map<Edge, String> edges,
                                             final CompressionFamilyDetector.Result fresh) {
        final Map<Item, Long> outgoing = edges.keySet().stream()
            .collect(Collectors.groupingBy(Edge::low, Collectors.counting()));
        final Map<Item, Long> incoming = edges.keySet().stream()
            .collect(Collectors.groupingBy(Edge::high, Collectors.counting()));
        final List<String> missing = new ArrayList<>();
        edges.forEach((edge, recipes) -> {
            final boolean low = covers(CompressionRecipeCatalog.INSTANCE.familyFor(edge.low()), edge);
            final boolean high = covers(CompressionRecipeCatalog.INSTANCE.familyFor(edge.high()), edge);
            // branch items keep one chain
            if ((low || outgoing.get(edge.low()) > 1) && (high || incoming.get(edge.high()) > 1) && (low || high)) {
                return;
            }
            final boolean freshCovers = covers(fresh.familyFor(id(edge.low())), edge)
                || covers(fresh.familyFor(id(edge.high())), edge);
            missing.add("MISSING " + edge + " | " + recipes
                + " | low=" + familyText(edge.low()) + " | high=" + familyText(edge.high())
                + " | freshScan=" + freshCovers);
        });
        return missing;
    }

    private static List<String> storageFailures(final Collection<Edge> edges) {
        final Set<Item> items = new HashSet<>();
        edges.forEach(edge -> {
            items.add(edge.low());
            items.add(edge.high());
        });
        final Set<CompressionFamily> checked = new HashSet<>();
        final List<String> failures = new ArrayList<>();
        for (final Item item : items) {
            final CompressionStorage storage = new CompressionStorage(() -> { });
            final var result = CompressionConfigurationMenu.configure(storage, new ItemStack(item));
            if (result != CompressionStorage.ConfigurationResult.SUCCESS) {
                failures.add("CONFIGURE " + id(item) + " " + result);
                continue;
            }
            final CompressionFamily family = storage.getFamily();
            if (!checked.add(family)) {
                continue;
            }
            final ItemResource top = family.resource(family.forms().getLast());
            final long inserted = storage.insert(top, 1, Action.EXECUTE, Actor.EMPTY);
            if (inserted != 1 || storage.getAll().size() != family.forms().size()) {
                failures.add("STORAGE " + id(item) + " inserted=" + inserted + " visible=" + storage.getAll().size()
                    + " forms=" + family.forms().size());
            }
        }
        return failures;
    }

    private static List<String> phantoms(final Set<Edge> edges, final CompressionFamilyDetector.Result fresh) {
        final List<String> phantoms = new ArrayList<>();
        for (final CompressionFamily family : new HashSet<>(fresh.families().values())) {
            final List<CompressionForm> forms = family.forms();
            for (int index = 1; index < forms.size(); index++) {
                final Edge edge = new Edge(
                    BuiltInRegistries.ITEM.get(forms.get(index - 1).itemId()),
                    BuiltInRegistries.ITEM.get(forms.get(index).itemId()),
                    (int) (forms.get(index).weight() / forms.get(index - 1).weight())
                );
                if (!edges.contains(edge)) {
                    phantoms.add("PHANTOM " + edge);
                }
            }
        }
        return phantoms.stream().distinct().sorted().toList();
    }

    private static List<String> familyLines(final Collection<Edge> edges) {
        return edges.stream()
            .flatMap(edge -> CompressionRecipeCatalog.INSTANCE.familyFor(edge.low()).stream())
            .distinct()
            .map(family -> "FAMILY " + text(family))
            .sorted()
            .toList();
    }

    private static boolean covers(final Optional<CompressionFamily> family, final Edge edge) {
        return family.flatMap(value -> value.find(new ItemResource(edge.low()))
                .flatMap(low -> value.find(new ItemResource(edge.high()))
                    .map(high -> high.weight() == low.weight() * edge.ratio())))
            .orElse(false);
    }

    private static List<RecipeHolder<CraftingRecipe>> recipesFor(final ServerLevel level, final CraftingInput input) {
        return level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, input, level);
    }

    private static ItemStack plainResult(final ServerLevel level,
                                         final CraftingRecipe recipe,
                                         final CraftingInput input,
                                         final int count) {
        final ItemStack result = recipe.assemble(input, level.registryAccess());
        final boolean plain = result.getCount() == count
            && result.isComponentsPatchEmpty()
            && recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty);
        return plain ? result.copyWithCount(1) : ItemStack.EMPTY;
    }

    private static String familyText(final Item item) {
        return CompressionRecipeCatalog.INSTANCE.familyFor(item)
            .map(CompressionPackAuditGameTests::text)
            .orElse("REJECTED");
    }

    private static String text(final CompressionFamily family) {
        return family.forms().stream()
            .map(form -> form.itemId() + "(" + form.weight() + ")")
            .collect(Collectors.joining(" "));
    }

    private static String describe(final RecipeHolder<CraftingRecipe> holder) {
        return holder.id() + "(" + BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer()) + ")";
    }

    private static ResourceLocation id(final Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    private static void write(final ServerLevel level, final List<String> report) {
        LOGGER.info("Compression audit: {}", report.getFirst());
        try {
            Files.write(level.getServer().getServerDirectory().resolve("compression-audit.txt"), report);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private record Edge(Item low, Item high, int ratio) {
        @Override
        public String toString() {
            return id(low) + " -> " + id(high) + " x" + ratio;
        }
    }
}
