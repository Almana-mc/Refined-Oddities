package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import me.almana.refined_oddities.crafting.RootStorageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RootStorageImpl.class, remap = false)
public interface RootStorageImplMixin extends RootStorageAccess {
    @Override
    @Accessor("storage")
    CompositeStorageImpl refinedOddities$getStorage();
}
