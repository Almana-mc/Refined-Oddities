package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.filter.Filter;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
import me.almana.refined_oddities.crafting.RelayStorageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.refinedmods.refinedstorage.api.network.impl.node.relay.RelayOutputStorage", remap = false)
public interface RelayOutputStorageMixin extends RelayStorageAccess {
    @Override
    @Accessor("delegate")
    StorageNetworkComponent refinedOddities$getDelegate();

    @Override
    @Accessor("accessMode")
    AccessMode refinedOddities$getAccessMode();

    @Override
    @Accessor("filter")
    Filter refinedOddities$getFilter();
}
