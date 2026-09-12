package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CompositeStorageImpl.class, remap = false)
public abstract class CompositeStorageImplMixin {
    @Shadow
    @Final
    private Set<ParentComposite> parentComposites;

    @Inject(method = "addToCache", at = @At("TAIL"))
    private void propagateAddition(final ResourceKey resource, final long amount, final CallbackInfo callback) {
        parentComposites.forEach(parent -> parent.addToCache(resource, amount));
    }

    @Inject(method = "removeFromCache", at = @At("TAIL"))
    private void propagateRemoval(final ResourceKey resource, final long amount, final CallbackInfo callback) {
        parentComposites.forEach(parent -> parent.removeFromCache(resource, amount));
    }
}
