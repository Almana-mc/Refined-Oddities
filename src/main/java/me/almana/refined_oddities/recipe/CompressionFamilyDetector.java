package me.almana.refined_oddities.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        final Map<Pair, Set<Integer>> compressionRatios = collectRatios(compression);
        final Map<Pair, Set<Integer>> decompressionRatios = collectRatios(decompression);
        final Map<ResourceLocation, Edge> outgoing = new HashMap<>();
        final Map<ResourceLocation, Edge> incoming = new HashMap<>();
        final Map<ResourceLocation, Set<ResourceLocation>> neighbors = new HashMap<>();
        final Set<Pair> pairs = new HashSet<>();
        pairs.addAll(compressionRatios.keySet());
        pairs.addAll(decompressionRatios.keySet());

        for (final Pair pair : pairs) {
            final Set<Integer> compress = compressionRatios.getOrDefault(pair, Set.of());
            final Set<Integer> decompress = decompressionRatios.getOrDefault(pair, Set.of());
            if (compress.isEmpty() || decompress.isEmpty()) {
                continue;
            }
            if (compress.size() != 1 || !compress.equals(decompress)) {
                invalid.add(pair.lower());
                invalid.add(pair.higher());
                continue;
            }
            final int ratio = compress.iterator().next();
            final Edge edge = new Edge(pair.lower(), pair.higher(), ratio);
            addEdge(outgoing, edge.lower(), edge, invalid);
            addEdge(incoming, edge.higher(), edge, invalid);
            neighbors.computeIfAbsent(edge.lower(), key -> new HashSet<>()).add(edge.higher());
            neighbors.computeIfAbsent(edge.higher(), key -> new HashSet<>()).add(edge.lower());
        }

        propagateInvalid(neighbors, invalid);
        final Map<ResourceLocation, CompressionFamily> families = new HashMap<>();
        final Set<ResourceLocation> visited = new HashSet<>();
        for (final ResourceLocation item : neighbors.keySet()) {
            if (visited.contains(item)) {
                continue;
            }
            final Set<ResourceLocation> component = component(item, neighbors);
            visited.addAll(component);
            if (component.stream().anyMatch(invalid::contains)) {
                invalid.addAll(component);
                continue;
            }
            final Optional<CompressionFamily> family = buildFamily(component, outgoing, incoming);
            if (family.isEmpty()) {
                invalid.addAll(component);
                continue;
            }
            component.forEach(member -> families.put(member, family.get()));
        }
        return new Result(Map.copyOf(families), Set.copyOf(invalid));
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

    private static void addEdge(final Map<ResourceLocation, Edge> edges,
                                final ResourceLocation key,
                                final Edge edge,
                                final Set<ResourceLocation> invalid) {
        final Edge previous = edges.putIfAbsent(key, edge);
        if (previous != null && !previous.equals(edge)) {
            invalid.add(previous.lower());
            invalid.add(previous.higher());
            invalid.add(edge.lower());
            invalid.add(edge.higher());
        }
    }

    private static void propagateInvalid(final Map<ResourceLocation, Set<ResourceLocation>> neighbors,
                                         final Set<ResourceLocation> invalid) {
        final ArrayDeque<ResourceLocation> queue = new ArrayDeque<>(invalid);
        while (!queue.isEmpty()) {
            final ResourceLocation item = queue.removeFirst();
            for (final ResourceLocation neighbor : neighbors.getOrDefault(item, Set.of())) {
                if (invalid.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
    }

    private static Set<ResourceLocation> component(final ResourceLocation first,
                                                   final Map<ResourceLocation, Set<ResourceLocation>> neighbors) {
        final Set<ResourceLocation> result = new HashSet<>();
        final ArrayDeque<ResourceLocation> queue = new ArrayDeque<>();
        queue.add(first);
        while (!queue.isEmpty()) {
            final ResourceLocation item = queue.removeFirst();
            if (result.add(item)) {
                queue.addAll(neighbors.getOrDefault(item, Set.of()));
            }
        }
        return result;
    }

    private static Optional<CompressionFamily> buildFamily(final Set<ResourceLocation> component,
                                                           final Map<ResourceLocation, Edge> outgoing,
                                                           final Map<ResourceLocation, Edge> incoming) {
        if (component.size() < 2 || component.size() > CompressionFamily.MAX_FORMS) {
            return Optional.empty();
        }
        final List<ResourceLocation> roots = component.stream().filter(item -> !incoming.containsKey(item)).toList();
        if (roots.size() != 1) {
            return Optional.empty();
        }
        final List<CompressionForm> forms = new ArrayList<>(component.size());
        final Set<ResourceLocation> seen = new HashSet<>();
        ResourceLocation current = roots.getFirst();
        long weight = 1;
        while (current != null && seen.add(current)) {
            forms.add(new CompressionForm(current, weight));
            final Edge edge = outgoing.get(current);
            if (edge == null) {
                current = null;
            } else {
                if (weight > CompressionStorage.CAPACITY / edge.ratio()) {
                    return Optional.empty();
                }
                weight *= edge.ratio();
                current = edge.higher();
            }
        }
        if (seen.size() != component.size()) {
            return Optional.empty();
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

    private record Pair(ResourceLocation lower, ResourceLocation higher) {
    }

    private record Edge(ResourceLocation lower, ResourceLocation higher, int ratio) {
    }
}
