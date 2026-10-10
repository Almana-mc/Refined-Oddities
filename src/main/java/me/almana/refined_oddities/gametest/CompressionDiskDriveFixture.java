package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.api.core.component.ComponentMapFactory;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.NetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.NetworkImpl;
import com.refinedmods.refinedstorage.api.network.impl.node.relay.RelayInputNetworkNode;
import com.refinedmods.refinedstorage.api.network.impl.node.relay.RelayOutputNetworkNode;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.common.support.network.component.PlatformStorageNetworkComponent;
import java.lang.reflect.Method;
import java.util.Set;
import me.almana.refined_oddities.storage.CompressionStorage;

final class CompressionDiskDriveFixture {
    private CompressionDiskDriveFixture() {
    }

    static RootStorageImpl rootWith(final CompressionStorage storage) {
        final StorageNetworkNode diskDrive = new StorageNetworkNode(0, 0, 1);
        diskDrive.setActive(true);
        ((CompositeStorage) diskDrive.getStorage()).addSource(new StateTrackedStorage(storage, null));
        final RootStorageImpl root = new RootStorageImpl();
        root.addSource(diskDrive.getStorage());
        return root;
    }

    static RootStorageImpl activeNetworkWith(final CompressionStorage storage) {
        final StorageNetworkNode diskDrive = new StorageNetworkNode(0, 0, 1);
        final RootStorageImpl root = new PlatformStorageNetworkComponent();
        diskDrive.setProvider(index -> java.util.Optional.of(storage));
        diskDrive.setNetwork(new NetworkImpl(new ComponentMapFactory<NetworkComponent, Network>()));
        root.addSource(diskDrive.getStorage());
        diskDrive.setActive(true);
        return root;
    }

    static RootStorageImpl behindRelay(final CompressionStorage storage,
                                       final AccessMode accessMode,
                                       final Set<ResourceKey> blocked) throws ReflectiveOperationException {
        final RelayOutputNetworkNode output = new RelayOutputNetworkNode(0);
        final RelayInputNetworkNode input = new RelayInputNetworkNode(0);
        input.setOutputNode(output);
        input.setAccessMode(accessMode);
        input.setFilters(blocked);
        final Method setStorageDelegate = RelayOutputNetworkNode.class
            .getDeclaredMethod("setStorageDelegate", StorageNetworkComponent.class);
        setStorageDelegate.setAccessible(true);
        setStorageDelegate.invoke(output, activeNetworkWith(storage));
        final RootStorageImpl root = new RootStorageImpl();
        root.addSource(output.getStorage());
        return root;
    }
}
