package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.autocrafting.task.TaskImpl;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskState;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.root.RootStorage;
import me.almana.refined_oddities.crafting.CompressionInitialExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TaskImpl.class, remap = false)
public abstract class TaskImplMixin {
    @Shadow
    @Final
    private MutableResourceList initialRequirements;

    @Shadow
    @Final
    private MutableResourceList internalStorage;

    @Shadow
    private TaskState state;

    @Inject(method = "extractInitialResourcesAndTryStartRunningTask", at = @At("HEAD"), cancellable = true)
    private void extractCanonicalRequirements(final RootStorage rootStorage,
                                              final CallbackInfoReturnable<Boolean> callback) {
        final CompressionInitialExtractor.Result result = CompressionInitialExtractor.extract(
            rootStorage,
            initialRequirements,
            internalStorage
        );
        if (result.complete()) {
            state = TaskState.RUNNING;
        }
        callback.setReturnValue(result.changed());
    }
}
