package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class PatternGridBlockEntityMixin {
    @Inject(method = "getProcessingPatternLimit", at = @At("HEAD"), cancellable = true)
    private static void raiseProcessingPatternLimit(final ResourceKey resource,
                                                    final CallbackInfoReturnable<Long> callback) {
        // int cap avoids ItemStack truncation
        callback.setReturnValue((long) Integer.MAX_VALUE);
    }
}
