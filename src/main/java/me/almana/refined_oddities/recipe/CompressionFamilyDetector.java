package me.almana.refined_oddities.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionForm;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.resources.ResourceLocation;

public final class CompressionFamilyDetector {
    private CompressionFamilyDetector() {
    }

    public static Result detect(final Collection<CompressionLink> compression,
                                final Collection<CompressionLink> decompression,
                                final Collection<ResourceLocation> rejectedItems) {
        final Set<ResourceLocation> invalid = new HashSet<>(rejectedItems);
        final Map<ResourceLocation, List<Edge>> upward = new HashMap<>();
        final Map<ResourceLocation, List<Edge>> downward = new HashMap<>();
        for (final Edge edge : candidates(compression, decompression, invalid)) {
            upward.computeIfAbsent(edge.lower(), key -> new ArrayList<>()).add(edge);
            downward.computeIfAbsent(edge.higher(), key -> new ArrayList<>()).add(edge);
        }
        final Walk up = new Walk(upward, Edge::higher, new HashMap<>());
        final Walk down = new Walk(downward, Edge::lower, new HashMap<>());
        final Set<ResourceLocation> items = new HashSet<>(upward.keySet());
        items.addAll(downward.keySet());
        final Map<ResourceLocation, CompressionFamily> families = new HashMap<>();
        for (final ResourceLocation item : items) {
            buildFamily(item, up, down).ifPresentOrElse(family -> families.put(item, family), () -> invalid.add(item));
        }
        return new Result(Map.copyOf(families), Set.copyOf(invalid));
    }

    private static List<Edge> candidates(final Collection<CompressionLink> compression,
                                         final Collection<CompressionLink> decompression,
                                         final Set<ResourceLocation> invalid) {
        final Map<Pair, Set<Integer>> compressionRatios = collectRatios(compression);
        final Map<Pair, Set<Integer>> decompressionRatios = collectRatios(decompression);
        final List<Edge> candidates = new ArrayList<>();
        compressionRatios.forEach((pair, compress) -> {
            final Set<Integer> decompress = decompressionRatios.getOrDefault(pair, Set.of());
            if (compress.size() != 1
                || !compress.equals(decompress)
                || invalid.contains(pair.lower())
                || invalid.contains(pair.higher())) {
                return;
            }
            candidates.add(new Edge(pair.lower(), pair.higher(), compress.iterator().next()));
        });
        return candidates;
    }

    private static Map<Pair, Set<Integer>> collectRatios(final Collection<CompressionLink> links) {
        final Map<Pair, Set<Integer>> result = new LinkedHashMap<>();
        for (final CompressionLink link : links) {
            if (link.ratio() != 4 && link.ratio() != 9) {
                continue;
            }
            result.computeIfAbsent(new Pair(link.lower(), link.higher()), key -> new HashSet<>()).add(link.ratio());
        }
        return result;
    }

    private static Optional<CompressionFamily> buildFamily(final ResourceLocation item,
                                                           final Walk up,
                                                           final Walk down) {
        final Set<ResourceLocation> seen = new HashSet<>(Set.of(item));
        final List<Edge> below = new ArrayList<>();
        final List<Edge> above = new ArrayList<>();
        if (!up.collect(item, seen, above) || !down.collect(item, seen, below)) {
            return Optional.empty();
        }
        final List<Edge> chain = new ArrayList<>(below.reversed());
        chain.addAll(above);
        final List<CompressionForm> forms = new ArrayList<>(List.of(new CompressionForm(chain.getFirst().lower(), 1)));
        long weight = 1;
        for (final Edge edge : chain) {
            if (weight > CompressionStorage.CAPACITY / edge.ratio()) {
                return Optional.empty();
            }
            weight *= edge.ratio();
            forms.add(new CompressionForm(edge.higher(), weight));
        }
        return Optional.of(new CompressionFamily(forms));
    }

    public record Result(Map<ResourceLocation, CompressionFamily> families,
                         Set<ResourceLocation> rejectedItems) {
        public Optional<CompressionFamily> familyFor(final ResourceLocation itemId) {
            if (rejectedItems.contains(itemId)) {
                return Optional.empty();
            }
            return Optional.ofNullable(families.get(itemId)).or(() -> Optional.of(CompressionFamily.single(itemId)));
        }
    }

    private record Walk(Map<ResourceLocation, List<Edge>> edges,
                        Function<Edge, ResourceLocation> next,
                        Map<ResourceLocation, Integer> reach) {
        private boolean collect(final ResourceLocation start, final Set<ResourceLocation> seen, final List<Edge> path) {
            for (Optional<Edge> edge = preferred(start); edge.isPresent(); edge = preferred(next.apply(edge.get()))) {
                if (!seen.add(next.apply(edge.get())) || seen.size() > CompressionFamily.MAX_FORMS) {
                    return false;
                }
                path.add(edge.get());
            }
            return true;
        }

        // longest chain wins, then smallest id
        private Optional<Edge> preferred(final ResourceLocation item) {
            return edges.getOrDefault(item, List.of()).stream().max(Comparator
                .comparingInt((Edge edge) -> reach(next.apply(edge)))
                .thenComparing(next, Comparator.reverseOrder()));
        }

        private int reach(final ResourceLocation item) {
            final Integer known = reach.get(item);
            if (known != null) {
                return known;
            }
            final int limit = CompressionFamily.MAX_FORMS + 1;
            reach.put(item, limit);
            final int value = 1 + edges.getOrDefault(item, List.of()).stream()
                .mapToInt(edge -> reach(next.apply(edge)))
                .max()
                .orElse(0);
            reach.put(item, Math.min(value, limit));
            return reach.get(item);
        }
    }

    private record Pair(ResourceLocation lower, ResourceLocation higher) {
    }

    private record Edge(ResourceLocation lower, ResourceLocation higher, int ratio) {
    }
}
