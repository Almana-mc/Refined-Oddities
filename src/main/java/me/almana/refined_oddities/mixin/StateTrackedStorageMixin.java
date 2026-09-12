package me.almana.refined_oddities.mixin;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import java.util.HashSet;
import java.util.Set;
import me.almana.refined_oddities.storage.CompressionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = StateTrackedStorage.class, remap = false)
public abstract class StateTrackedStorageMixin implements CompositeAwareChild {
    @Shadow
    @Final
    private Storage delegate;

    @Shadow
    private void checkStateChanged() {
    }

    @Unique
    private final Set<ParentComposite> refinedOddities$parents = new HashSet<>();

    @Override
    public void onAddedIntoComposite(final ParentComposite parentComposite) {
        if (delegate instanceof CompressionStorage storage) {
            refinedOddities$parents.add(parentComposite);
            storage.onTrackedAddedIntoComposite(parentComposite);
        } else if (delegate instanceof CompositeAwareChild child) {
            child.onAddedIntoComposite(parentComposite);
        }
    }

    @Override
    public void onRemovedFromComposite(final ParentComposite parentComposite) {
        if (delegate instanceof CompressionStorage storage) {
            refinedOddities$parents.remove(parentComposite);
            storage.onTrackedRemovedFromComposite(parentComposite);
        } else if (delegate instanceof CompositeAwareChild child) {
            child.onRemovedFromComposite(parentComposite);
        }
    }

    @Override
    public Amount compositeInsert(final ResourceKey resource,
                                  final long amount,
                                  final Action action,
                                  final Actor actor) {
        if (delegate instanceof CompressionStorage storage) {
            final long before = storage.getBaseUnits();
            final long inserted = delegate.insert(resource, amount, action, actor);
            if (inserted > 0 && action == Action.EXECUTE) {
                refinedOddities$propagateChanges(storage, before, resource, true);
                checkStateChanged();
            }
            return new Amount(inserted, inserted);
        }
        final Amount result;
        if (delegate instanceof CompositeAwareChild child) {
            result = child.compositeInsert(resource, amount, action, actor);
        } else {
            final long inserted = delegate.insert(resource, amount, action, actor);
            result = new Amount(inserted, inserted);
        }
        if (result.amount() > 0 && action == Action.EXECUTE) {
            checkStateChanged();
        }
        return result;
    }

    @Override
    public Amount compositeExtract(final ResourceKey resource,
                                   final long amount,
                                   final Action action,
                                   final Actor actor) {
        if (delegate instanceof CompressionStorage storage) {
            final long before = storage.getBaseUnits();
            final long extracted = delegate.extract(resource, amount, action, actor);
            if (extracted > 0 && action == Action.EXECUTE) {
                refinedOddities$propagateChanges(storage, before, resource, true);
                checkStateChanged();
            }
            return new Amount(extracted, extracted);
        }
        final Amount result;
        if (delegate instanceof CompositeAwareChild child) {
            result = child.compositeExtract(resource, amount, action, actor);
        } else {
            final long extracted = delegate.extract(resource, amount, action, actor);
            result = new Amount(extracted, extracted);
        }
        if (result.amount() > 0 && action == Action.EXECUTE) {
            checkStateChanged();
        }
        return result;
    }

    @Override
    public boolean contains(final Storage storage) {
        return delegate == storage
            || delegate instanceof CompositeAwareChild child && child.contains(storage);
    }

    @Redirect(
        method = "insert",
        at = @At(
            value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/api/storage/Storage;insert(Lcom/refinedmods/refinedstorage/api/resource/ResourceKey;JLcom/refinedmods/refinedstorage/api/core/Action;Lcom/refinedmods/refinedstorage/api/storage/Actor;)J"
        )
    )
    private long propagateDirectInsert(final Storage storage,
                                       final ResourceKey resource,
                                       final long amount,
                                       final Action action,
                                       final Actor actor) {
        if (!(storage instanceof CompressionStorage) || action != Action.EXECUTE) {
            return storage.insert(resource, amount, action, actor);
        }
        final CompressionStorage compression = (CompressionStorage) storage;
        final long before = compression.getBaseUnits();
        final long inserted = storage.insert(resource, amount, action, actor);
        if (inserted > 0) {
            refinedOddities$propagateChanges(compression, before, resource, false);
        }
        return inserted;
    }

    @Redirect(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/api/storage/Storage;extract(Lcom/refinedmods/refinedstorage/api/resource/ResourceKey;JLcom/refinedmods/refinedstorage/api/core/Action;Lcom/refinedmods/refinedstorage/api/storage/Actor;)J"
        )
    )
    private long propagateDirectExtract(final Storage storage,
                                        final ResourceKey resource,
                                        final long amount,
                                        final Action action,
                                        final Actor actor) {
        if (!(storage instanceof CompressionStorage) || action != Action.EXECUTE) {
            return storage.extract(resource, amount, action, actor);
        }
        final CompressionStorage compression = (CompressionStorage) storage;
        final long before = compression.getBaseUnits();
        final long extracted = storage.extract(resource, amount, action, actor);
        if (extracted > 0) {
            refinedOddities$propagateChanges(compression, before, resource, false);
        }
        return extracted;
    }

    @Unique
    private void refinedOddities$propagateChanges(final CompressionStorage storage,
                                                  final long before,
                                                  final ResourceKey changedResource,
                                                  final boolean parentUpdatesResource) {
        final long after = storage.getBaseUnits();
        for (final var form : storage.getFamily().forms()) {
            if (!storage.isFormEnabled(form.itemId())) {
                continue;
            }
            final ResourceKey resource = storage.resource(form);
            if (parentUpdatesResource && resource.equals(changedResource)) {
                continue;
            }
            final long change = after / form.weight() - before / form.weight();
            if (change > 0) {
                refinedOddities$parents.forEach(parent -> parent.addToCache(resource, change));
            } else if (change < 0) {
                refinedOddities$parents.forEach(parent -> parent.removeFromCache(resource, -change));
            }
        }
    }
}
