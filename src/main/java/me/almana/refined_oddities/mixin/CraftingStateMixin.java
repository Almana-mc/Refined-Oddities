package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import java.util.List;
import me.almana.refined_oddities.crafting.CompressionPlanningState;
import me.almana.refined_oddities.crafting.CompressionStorageHandle;
import me.almana.refined_oddities.crafting.CompressionStorageWalker;
import me.almana.refined_oddities.crafting.CraftingStateAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.refinedmods.refinedstorage.api.autocrafting.calculation.CraftingState", remap = false)
public abstract class CraftingStateMixin implements CraftingStateAccess {
    @Shadow
    @Final
    private MutableResourceList storage;

    @Unique
    private CompressionPlanningState refinedOddities$pools;

    @Inject(method = "of", at = @At("RETURN"))
    private static void initializePools(final RootStorage rootStorage,
                                        final CallbackInfoReturnable<Object> callback) {
        final List<CompressionStorageHandle> handles = CompressionStorageWalker.find(rootStorage);
        if (handles.isEmpty()) {
            return;
        }
        final CraftingStateAccess state = (CraftingStateAccess) callback.getReturnValue();
        state.refinedOddities$initialize(handles);
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void copyPools(final CallbackInfoReturnable<Object> callback) {
        if (refinedOddities$pools != null) {
            final CraftingStateAccess copy = (CraftingStateAccess) callback.getReturnValue();
            copy.refinedOddities$copyFrom(refinedOddities$pools.copy());
        }
    }

    @Override
    public void refinedOddities$initialize(final List<CompressionStorageHandle> handles) {
        refinedOddities$pools = new CompressionPlanningState(handles);
        handles.forEach(handle -> handle.storage().getAll().forEach(storage::remove));
    }

    @Override
    public void refinedOddities$copyFrom(final CompressionPlanningState pools) {
        refinedOddities$pools = pools;
    }

    @Redirect(
        method = "getResource",
        at = @At(
            value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/api/resource/list/MutableResourceList;get(Lcom/refinedmods/refinedstorage/api/resource/ResourceKey;)J",
            ordinal = 0
        )
    )
    private long includeCanonicalResources(final MutableResourceList list, final ResourceKey resource) {
        final long ordinary = list.get(resource);
        if (refinedOddities$pools == null) {
            return ordinary;
        }
        final long compressed = refinedOddities$pools.available(resource);
        return Long.MAX_VALUE - ordinary < compressed ? Long.MAX_VALUE : ordinary + compressed;
    }

    @Inject(method = "extractFromStorage", at = @At("HEAD"), cancellable = true)
    private void reserveCanonicalResource(final ResourceKey resource,
                                          final long amount,
                                          final CallbackInfo callback) {
        if (refinedOddities$pools == null) {
            return;
        }
        final long ordinary = Math.min(amount, storage.get(resource));
        if (ordinary > 0) {
            storage.remove(resource, ordinary);
        }
        final long fromCompression = amount - ordinary;
        if (fromCompression > 0 && !refinedOddities$pools.reserve(resource, fromCompression)) {
            throw new IllegalStateException("Compression allocation changed during crafting calculation");
        }
        callback.cancel();
    }
}
