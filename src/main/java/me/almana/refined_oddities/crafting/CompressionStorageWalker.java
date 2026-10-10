package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
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
        final List<CompressionStorageHandle> result = new ArrayList<>();
        walk(rootStorage, result);
        return List.copyOf(result);
    }

    private static void walk(final Storage source, final List<CompressionStorageHandle> result) {
        if (source instanceof RootStorageAccess root) {
            walk(root.refinedOddities$getStorage(), result);
        } else if (source instanceof RelayStorageAccess relay) {
            walkRelay(relay, result);
        } else if (source instanceof StateTrackedStorage tracked) {
            if (tracked.getDelegate() instanceof CompressionStorage compression) {
                add(tracked, compression, result);
            } else {
                walk(tracked.getDelegate(), result);
            }
        } else if (source instanceof CompressionStorage compression) {
            add(compression, compression, result);
        } else if (source instanceof CompositeStorage composite) {
            composite.getSources().forEach(child -> walk(child, result));
        }
    }

    private static void walkRelay(final RelayStorageAccess relay, final List<CompressionStorageHandle> result) {
        final StorageNetworkComponent delegate = relay.refinedOddities$getDelegate();
        if (delegate == null
            || relay.refinedOddities$getAccessMode() == AccessMode.INSERT
            || delegate.contains(delegate)) {
            return;
        }
        walk(delegate, result);
    }

    private static void add(final Storage access,
                            final CompressionStorage compression,
                            final List<CompressionStorageHandle> result) {
        if (!compression.isQuarantined() && compression.usesCompression()) {
            result.add(new CompressionStorageHandle(access, compression));
        }
    }
}
