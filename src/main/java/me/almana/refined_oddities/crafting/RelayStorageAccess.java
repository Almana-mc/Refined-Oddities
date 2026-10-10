package me.almana.refined_oddities.crafting;

import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.filter.Filter;
import com.refinedmods.refinedstorage.api.storage.AccessMode;

public interface RelayStorageAccess {
    StorageNetworkComponent refinedOddities$getDelegate();

    AccessMode refinedOddities$getAccessMode();

    Filter refinedOddities$getFilter();
}
