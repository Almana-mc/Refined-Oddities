package me.almana.refined_oddities.menu;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.Optional;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.content.ModMenus;
import me.almana.refined_oddities.recipe.CompressionRecipeCatalog;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CompressionConfigurationMenu extends AbstractContainerMenu {
    public static final int CLEAR_BUTTON = 0;
    public static final int SELECT_ITEM_BUTTON_OFFSET = 1;
    public static final int TOGGLE_FORM_BUTTON_OFFSET = -1;
    private static final int FIRST_PLAYER_SLOT = 1 + CompressionFamily.MAX_FORMS;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack openedStack;
    private final ContainerData data;
    private final DataSlot result = DataSlot.standalone();
    private final SimpleContainer selector = new SimpleContainer(1);
    private final SimpleContainer states = new SimpleContainer(CompressionFamily.MAX_FORMS);

    public CompressionConfigurationMenu(final int syncId,
                                        final Inventory inventory,
                                        final InteractionHand hand) {
        this(syncId, inventory, hand, createData(inventory.player, hand));
    }

    private CompressionConfigurationMenu(final int syncId,
                                         final Inventory inventory,
                                         final InteractionHand hand,
                                         final ContainerData data) {
        super(ModMenus.COMPRESSION_CONFIGURATION.get(), syncId);
        this.player = inventory.player;
        this.hand = hand;
        this.openedStack = player.getItemInHand(hand).copy();
        this.data = data;
        addSlot(new SelectorSlot(selector, 80, 73));
        for (int index = 0; index < CompressionFamily.MAX_FORMS; index++) {
            addSlot(new StateSlot(states, index));
        }
        initializeConfiguration();
        addDataSlots(data);
        addDataSlot(result);
        addPlayerSlots(inventory);
    }

    private static ContainerData createData(final Player player, final InteractionHand hand) {
        return player.level().isClientSide()
            ? new SimpleContainerData(CompressionConfigurationData.DATA_SIZE)
            : new CompressionConfigurationData(player, hand);
    }

    private void initializeConfiguration() {
        if (player.level().isClientSide()) {
            return;
        }
        storage().ifPresent(value -> {
            value.getConfiguredResource().ifPresent(resource -> selector.setItem(0, resource.toItemStack()));
            syncStateSlots(value.getFamily());
        });
    }

    private void addPlayerSlots(final Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 141 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 200));
        }
    }

    @Override
    public void clicked(final int slotId,
                        final int button,
                        final ClickType clickType,
                        final Player clickingPlayer) {
        if (slotId >= FIRST_PLAYER_SLOT) {
            super.clicked(slotId, button, clickType, clickingPlayer);
            return;
        }
        if (slotId == 0
            && !clickingPlayer.level().isClientSide()
            && clickType == ClickType.PICKUP
            && !getCarried().isEmpty()) {
            configureSelection(getCarried());
        }
    }

    static CompressionStorage.ConfigurationResult configure(final CompressionStorage storage,
                                                            final ItemStack selected) {
        if (selected.isEmpty()) {
            return CompressionStorage.ConfigurationResult.INVALID_FAMILY;
        }
        final ItemResource resource = ItemResource.ofItemStack(selected.copyWithCount(1));
        final CompressionFamily single = CompressionFamily.single(BuiltInRegistries.ITEM.getKey(selected.getItem()));
        final CompressionFamily family = selected.isComponentsPatchEmpty()
            ? CompressionRecipeCatalog.INSTANCE.familyFor(selected.getItem()).orElse(single)
            : single;
        return storage.configure(family, resource);
    }

    @Override
    public boolean clickMenuButton(final Player clickingPlayer, final int id) {
        if (clickingPlayer.level().isClientSide()) {
            return false;
        }
        if (id == CLEAR_BUTTON) {
            return clearSelection();
        }
        if (id <= TOGGLE_FORM_BUTTON_OFFSET) {
            return toggleForm(TOGGLE_FORM_BUTTON_OFFSET - id);
        }
        final int itemId = id - SELECT_ITEM_BUTTON_OFFSET;
        if (itemId < 0) {
            return false;
        }
        return configureSelection(new ItemStack(BuiltInRegistries.ITEM.byId(itemId)));
    }

    private boolean clearSelection() {
        final CompressionStorage.ConfigurationResult configuration = storage()
            .map(CompressionStorage::clearFamily)
            .orElse(CompressionStorage.ConfigurationResult.INVALID_FAMILY);
        result.set(configuration.ordinal() + 1);
        if (configuration == CompressionStorage.ConfigurationResult.SUCCESS) {
            selector.clearContent();
            states.clearContent();
        }
        broadcastChanges();
        return configuration == CompressionStorage.ConfigurationResult.SUCCESS;
    }

    private boolean configureSelection(final ItemStack selected) {
        final CompressionStorage.ConfigurationResult configuration = storage()
            .map(value -> configure(value, selected))
            .orElse(CompressionStorage.ConfigurationResult.INVALID_FAMILY);
        result.set(configuration.ordinal() + 1);
        if (configuration == CompressionStorage.ConfigurationResult.SUCCESS) {
            selector.setItem(0, selected.copyWithCount(1));
            storage().ifPresent(value -> syncStateSlots(value.getFamily()));
        }
        broadcastChanges();
        return configuration == CompressionStorage.ConfigurationResult.SUCCESS;
    }

    private boolean toggleForm(final int index) {
        final CompressionStorage.ConfigurationResult configuration = storage()
            .map(value -> toggleForm(value, index))
            .orElse(CompressionStorage.ConfigurationResult.INVALID_FAMILY);
        result.set(configuration.ordinal() + 1);
        broadcastChanges();
        return configuration == CompressionStorage.ConfigurationResult.SUCCESS;
    }

    private static CompressionStorage.ConfigurationResult toggleForm(final CompressionStorage storage,
                                                                      final int index) {
        if (index < 0 || index >= storage.getFamily().forms().size()) {
            return CompressionStorage.ConfigurationResult.INVALID_FAMILY;
        }
        return storage.toggleForm(storage.getFamily().forms().get(index).itemId());
    }

    private void syncStateSlots(final CompressionFamily family) {
        states.clearContent();
        for (int index = 0; index < family.forms().size(); index++) {
            states.setItem(index, new ItemStack(BuiltInRegistries.ITEM.get(family.forms().get(index).itemId())));
        }
    }

    private Optional<CompressionStorage> storage() {
        final ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof me.almana.refined_oddities.item.BulkStorageDiskItem disk)) {
            return Optional.empty();
        }
        return disk.resolve(RefinedStorageApi.INSTANCE.getStorageRepository(player.level()), stack)
            .filter(CompressionStorage.class::isInstance)
            .map(CompressionStorage.class::cast);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        if (player.level().isClientSide() || index < FIRST_PLAYER_SLOT || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        configureSelection(slots.get(index).getItem());
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        final ItemStack current = player.getItemInHand(hand);
        return current.is(ModItems.BULK_STORAGE_DISK.get())
            && ItemStack.isSameItemSameComponents(openedStack, current);
    }

    public int flags() {
        return data.get(0);
    }

    public boolean canChangeSelection() {
        final int flags = flags();
        return (flags & 2) == 0 && (flags & 4) != 0 && (flags & 8) == 0;
    }

    public boolean hasSelection() {
        return (flags() & 1) != 0;
    }

    public int resultCode() {
        return result.get();
    }

    public int stateCount() {
        int count = 0;
        while (count < CompressionFamily.MAX_FORMS && !states.getItem(count).isEmpty()) {
            count++;
        }
        return count;
    }

    public ItemStack getStateStack(final int index) {
        return index >= 0 && index < CompressionFamily.MAX_FORMS ? states.getItem(index) : ItemStack.EMPTY;
    }

    public boolean isStateEnabled(final int index) {
        return !getStateStack(index).isEmpty() && data.get(index + 1) != 0;
    }

    private static final class SelectorSlot extends Slot {
        private SelectorSlot(final SimpleContainer container, final int x, final int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(final Player player) {
            return false;
        }
    }

    private static final class StateSlot extends Slot {
        private StateSlot(final SimpleContainer container, final int index) {
            super(container, index, -10_000, -10_000);
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(final Player player) {
            return false;
        }
    }
}
