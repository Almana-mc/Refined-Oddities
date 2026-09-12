package me.almana.refined_oddities.item;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.AbstractStorageContainerItem;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageRepository;
import java.util.Optional;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BulkStorageDiskItem extends AbstractStorageContainerItem {
    private static final Component TITLE = Component.translatable(
        "menu.refined_oddities.bulk_storage_configuration"
    );

    public BulkStorageDiskItem() {
        super(
            new Item.Properties().stacksTo(1).fireResistant(),
            RefinedStorageApi.INSTANCE.getStorageContainerItemHelper()
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level,
                                                  final Player player,
                                                  final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!mayDisassemble(level, stack)) {
                return InteractionResultHolder.fail(stack);
            }
            return super.use(level, player, hand);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            inventoryTick(stack, level, player, hand == InteractionHand.MAIN_HAND
                ? player.getInventory().selected
                : 40, true);
            serverPlayer.openMenu(
                new SimpleMenuProvider(
                    (syncId, inventory, ignored) -> new CompressionConfigurationMenu(syncId, inventory, hand),
                    TITLE
                ),
                data -> data.writeEnum(hand)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private boolean mayDisassemble(final Level level, final ItemStack stack) {
        if (level.isClientSide()) {
            return true;
        }
        final StorageRepository repository = RefinedStorageApi.INSTANCE.getStorageRepository(level);
        final Optional<SerializableStorage> resolved = resolve(repository, stack);
        return resolved.isEmpty() || resolved.filter(CompressionStorage.class::isInstance)
            .map(CompressionStorage.class::cast)
            .filter(storage -> !storage.isQuarantined() && !storage.isConnected())
            .isPresent();
    }

    @Override
    protected Long getCapacity() {
        return CompressionStorage.CAPACITY;
    }

    @Override
    protected String formatAmount(final long amount) {
        return String.format("%,d", amount);
    }

    @Override
    protected SerializableStorage createStorage(final StorageRepository storageRepository) {
        return new CompressionStorage(storageRepository::markAsChanged);
    }

    @Override
    protected ItemStack createPrimaryDisassemblyByproduct(final int count) {
        return new ItemStack(ModItems.BULK_STORAGE_HOUSING.get(), count);
    }

    @Override
    protected ItemStack createSecondaryDisassemblyByproduct(final int count) {
        return new ItemStack(ModItems.BULK_STORAGE_PART.get(), count);
    }
}
