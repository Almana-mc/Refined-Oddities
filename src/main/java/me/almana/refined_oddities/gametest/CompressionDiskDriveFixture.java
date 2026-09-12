package me.almana.refined_oddities.gametest;

import com.refinedmods.refinedstorage.api.core.component.ComponentMapFactory;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.NetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.NetworkImpl;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.common.support.network.component.PlatformStorageNetworkComponent;
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
}
