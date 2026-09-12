package me.almana.refined_oddities.storage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorage;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.almana.refined_oddities.crafting.CompressionActors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class CompressionStorage implements SerializableStorage, LimitedStorage, CompositeAwareChild {
    public static final long CAPACITY = 281_474_976_710_656L;

    private final Runnable listener;
    private final Set<ParentComposite> parents = new HashSet<>();
    private final Set<ParentComposite> trackedParents = new HashSet<>();
    private long baseUnits;
    private CompressionFamily family;
    private Optional<ItemResource> configuredResource;
    private final Set<ResourceLocation> enabledItemIds = new HashSet<>();
    private final boolean quarantined;

    public CompressionStorage(final Runnable listener) {
        this(0, CompressionFamily.empty(), Optional.empty(), Optional.empty(), listener);
    }

    public CompressionStorage(final long baseUnits, final CompressionFamily family, final Runnable listener) {
        this(baseUnits, family, Optional.empty(), Optional.empty(), listener);
    }

    public CompressionStorage(final long baseUnits,
                              final CompressionFamily family,
                              final Optional<ItemResource> configuredResource,
                              final Optional<List<ResourceLocation>> enabledItems,
                              final Runnable listener) {
        this.baseUnits = baseUnits;
        this.family = family;
        final List<ResourceLocation> enabled = enabledItems.orElseGet(() -> family.forms().stream()
            .map(CompressionForm::itemId)
            .toList());
        enabledItemIds.addAll(enabled);
        this.configuredResource = configuredResource.or(() -> family.forms().stream()
            .findFirst()
            .map(form -> new ItemResource(BuiltInRegistries.ITEM.get(form.itemId()))));
        this.listener = listener;
        this.quarantined = baseUnits < 0
            || baseUnits > CAPACITY
            || !family.isValidSnapshot()
            || !isConfiguredResourceValid(family, this.configuredResource)
            || !isEnabledSelectionValid(family, enabled);
    }

    @Override
    public StorageType getType() {
        return CompressionStorageType.INSTANCE;
    }

    @Override
    public long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        return insert(resource, amount, action, actor, false);
    }
    private long insert(final ResourceKey resource, final long amount, final Action action, final Actor actor, final boolean parentUpdatesResource) {
        ResourceAmount.validate(resource, amount);
        if (quarantined) {
            return 0;
        }
        final var form = findForm(resource);
        if (form.isEmpty() || !isFormEnabled(form.get().itemId())) {
            return 0;
        }
        final long accepted = Math.min(amount, (CAPACITY - baseUnits) / form.get().weight());
        if (accepted > 0 && action == Action.EXECUTE) {
            changeUnits(accepted * form.get().weight(), resource, parentUpdatesResource);
        }
        return accepted;
    }

    @Override
    public long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        return extract(resource, amount, action, actor, false);
    }
    private long extract(final ResourceKey resource, final long amount, final Action action, final Actor actor, final boolean parentUpdatesResource) {
        ResourceAmount.validate(resource, amount);
        if (quarantined || actor == CompressionActors.ORDINARY_ONLY && usesCompression()) {
            return 0;
        }
        final var form = findForm(resource);
        if (form.isEmpty() || !isFormEnabled(form.get().itemId())) {
            return 0;
        }
        final long extracted = Math.min(amount, baseUnits / form.get().weight());
        if (extracted > 0 && action == Action.EXECUTE) {
            changeUnits(-(extracted * form.get().weight()), resource, parentUpdatesResource);
        }
        return extracted;
    }

    private void changeUnits(final long change, final ResourceKey changedResource, final boolean parentUpdatesResource) {
        final long before = baseUnits;
        baseUnits += change;
        for (final CompressionForm form : family.forms()) {
            if (!isFormEnabled(form.itemId())) {
                continue;
            }
            final long previous = before / form.weight();
            final long current = baseUnits / form.weight();
            final ResourceKey resource = resource(form);
            if (!parentUpdatesResource || !resource.equals(changedResource)) {
                notifyParents(resource, current - previous);
            }
        }
        listener.run();
    }

    private void notifyParents(final ResourceKey resource, final long change) {
        if (change > 0) {
            parents.forEach(parent -> parent.addToCache(resource, change));
        } else if (change < 0) {
            parents.forEach(parent -> parent.removeFromCache(resource, -change));
        }
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        if (quarantined || baseUnits == 0) {
            return List.of();
        }
        final List<ResourceAmount> resources = new ArrayList<>(family.forms().size());
        for (final CompressionForm form : family.forms()) {
            if (!isFormEnabled(form.itemId())) {
                continue;
            }
            final long amount = baseUnits / form.weight();
            if (amount > 0) {
                resources.add(new ResourceAmount(resource(form), amount));
            }
        }
        return resources;
    }

    @Override
    public long getStored() {
        return quarantined ? 0 : baseUnits;
    }

    @Override
    public long getCapacity() {
        return CAPACITY;
    }

    public long getBaseUnits() {
        return baseUnits;
    }

    public CompressionFamily getFamily() {
        return family;
    }

    public Optional<ItemResource> getConfiguredResource() {
        return configuredResource;
    }

    public boolean isFormEnabled(final ResourceLocation itemId) {
        return enabledItemIds.contains(itemId);
    }

    public List<ResourceLocation> getEnabledItemIds() {
        return family.forms().stream()
            .map(CompressionForm::itemId)
            .filter(enabledItemIds::contains)
            .toList();
    }

    public boolean isQuarantined() {
        return quarantined;
    }

    public boolean isConnected() {
        return !parents.isEmpty() || !trackedParents.isEmpty();
    }

    public boolean usesCompression() {
        return family.forms().size() > 1;
    }

    public void onTrackedAddedIntoComposite(final ParentComposite parentComposite) {
        trackedParents.add(parentComposite);
    }

    public void onTrackedRemovedFromComposite(final ParentComposite parentComposite) {
        trackedParents.remove(parentComposite);
    }

    public ConfigurationResult configure(final CompressionFamily newFamily) {
        if (newFamily.forms().isEmpty()) {
            return ConfigurationResult.INVALID_FAMILY;
        }
        return configure(newFamily, newFamily.forms().getFirst().itemId());
    }

    public ConfigurationResult configure(final CompressionFamily newFamily,
                                         final ResourceLocation newConfiguredItemId) {
        return configure(newFamily, new ItemResource(BuiltInRegistries.ITEM.get(newConfiguredItemId)));
    }

    public ConfigurationResult configure(final CompressionFamily newFamily,
                                         final ItemResource newConfiguredResource) {
        if (quarantined) {
            return ConfigurationResult.QUARANTINED;
        }
        if (isConnected()) {
            return ConfigurationResult.CONNECTED;
        }
        if (baseUnits != 0) {
            return ConfigurationResult.NOT_EMPTY;
        }
        if (!newFamily.isValidSnapshot()
            || !newFamily.isConfigured()
            || !isConfiguredResourceValid(newFamily, Optional.of(newConfiguredResource))) {
            return ConfigurationResult.INVALID_FAMILY;
        }
        family = newFamily;
        enabledItemIds.clear();
        newFamily.forms().forEach(form -> enabledItemIds.add(form.itemId()));
        configuredResource = Optional.of(newConfiguredResource);
        listener.run();
        return ConfigurationResult.SUCCESS;
    }

    public ConfigurationResult clearFamily() {
        if (quarantined) {
            return ConfigurationResult.QUARANTINED;
        }
        if (isConnected()) {
            return ConfigurationResult.CONNECTED;
        }
        if (baseUnits != 0) {
            return ConfigurationResult.NOT_EMPTY;
        }
        family = CompressionFamily.empty();
        enabledItemIds.clear();
        configuredResource = Optional.empty();
        listener.run();
        return ConfigurationResult.SUCCESS;
    }

    public ConfigurationResult toggleForm(final ResourceLocation itemId) {
        if (quarantined) {
            return ConfigurationResult.QUARANTINED;
        }
        if (isConnected()) {
            return ConfigurationResult.CONNECTED;
        }
        if (baseUnits != 0) {
            return ConfigurationResult.NOT_EMPTY;
        }
        if (family.forms().stream().noneMatch(form -> form.itemId().equals(itemId))) {
            return ConfigurationResult.INVALID_FAMILY;
        }
        if (enabledItemIds.contains(itemId)) {
            if (enabledItemIds.size() == 1) {
                return ConfigurationResult.LAST_ENABLED;
            }
            enabledItemIds.remove(itemId);
        } else {
            enabledItemIds.add(itemId);
        }
        listener.run();
        return ConfigurationResult.SUCCESS;
    }

    private Optional<CompressionForm> findForm(final ResourceKey resource) {
        if (family.forms().size() == 1 && configuredResource.isPresent()) {
            return configuredResource.get().equals(resource) ? Optional.of(family.forms().getFirst()) : Optional.empty();
        }
        return family.find(resource);
    }

    public ResourceKey resource(final CompressionForm form) {
        if (family.forms().size() == 1 && configuredResource.isPresent()) {
            return configuredResource.get();
        }
        return family.resource(form);
    }

    private static boolean isConfiguredResourceValid(final CompressionFamily family,
                                                       final Optional<ItemResource> configuredResource) {
        if (!family.isConfigured()) {
            return configuredResource.isEmpty();
        }
        return configuredResource.filter(resource -> {
            final ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(resource.item());
            return family.forms().stream().anyMatch(form -> form.itemId().equals(itemId))
                && (resource.components().isEmpty() || family.forms().size() == 1);
        }).isPresent();
    }

    private static boolean isEnabledSelectionValid(final CompressionFamily family,
                                                   final List<ResourceLocation> enabledItems) {
        if (!family.isConfigured()) {
            return enabledItems.isEmpty();
        }
        final Set<ResourceLocation> unique = new HashSet<>(enabledItems);
        return !enabledItems.isEmpty()
            && unique.size() == enabledItems.size()
            && family.forms().stream().map(CompressionForm::itemId).toList().containsAll(unique);
    }

    @Override
    public void onAddedIntoComposite(final ParentComposite parentComposite) {
        parents.add(parentComposite);
    }
    @Override
    public void onRemovedFromComposite(final ParentComposite parentComposite) {
        parents.remove(parentComposite);
    }

    @Override
    public Amount compositeInsert(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long inserted = insert(resource, amount, action, actor, true);
        return new Amount(inserted, inserted);
    }
    @Override
    public Amount compositeExtract(final ResourceKey resource, final long amount, final Action action, final Actor actor) {
        final long extracted = extract(resource, amount, action, actor, true);
        return new Amount(extracted, extracted);
    }

    @Override
    public boolean contains(final Storage storage) {
        return storage == this;
    }

    public enum ConfigurationResult {
        SUCCESS,
        CONNECTED,
        NOT_EMPTY,
        INVALID_FAMILY,
        QUARANTINED,
        LAST_ENABLED
    }
}
