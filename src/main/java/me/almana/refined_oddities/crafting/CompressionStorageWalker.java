package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import java.util.ArrayList;
import java.util.List;
import me.almana.refined_oddities.storage.CompressionStorage;

public final class CompressionStorageWalker {
    private CompressionStorageWalker() {
    }

    public static List<CompressionStorageHandle> find(final RootStorage rootStorage) {
        if (!(rootStorage instanceof RootStorageAccess access)) {
            return List.of();
        }
        final List<CompressionStorageHandle> result = new ArrayList<>();
        walk(access.refinedOddities$getStorage(), result);
        return List.copyOf(result);
    }

    private static void walk(final Storage source, final List<CompressionStorageHandle> result) {
        if (source instanceof StateTrackedStorage tracked) {
            if (tracked.getDelegate() instanceof CompressionStorage compression) {
                if (!compression.isQuarantined() && compression.usesCompression()) {
                    result.add(new CompressionStorageHandle(tracked, compression));
                }
            } else {
                walk(tracked.getDelegate(), result);
            }
        } else if (source instanceof CompressionStorage compression) {
            if (!compression.isQuarantined() && compression.usesCompression()) {
                result.add(new CompressionStorageHandle(compression, compression));
            }
        } else if (source instanceof CompositeStorage composite) {
            composite.getSources().forEach(child -> walk(child, result));
        }
    }
}
