package me.almana.refined_oddities.menu;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import java.util.Optional;
import me.almana.refined_oddities.item.CompressionStorageDiskItem;
import me.almana.refined_oddities.storage.CompressionFamily;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

final class CompressionConfigurationData implements ContainerData {
    static final int DATA_SIZE = 1 + CompressionFamily.MAX_FORMS;

    private final Player player;
    private final InteractionHand hand;

    CompressionConfigurationData(final Player player, final InteractionHand hand) {
        this.player = player;
        this.hand = hand;
    }

    @Override
    public int get(final int index) {
        return resolve().map(storage -> value(storage, index)).orElse(0);
    }

    private static int value(final CompressionStorage storage, final int index) {
        if (index == 0) {
            return flags(storage);
        }
        final int formIndex = index - 1;
        if (formIndex >= storage.getFamily().forms().size()) {
            return 0;
        }
        return storage.isFormEnabled(storage.getFamily().forms().get(formIndex).itemId()) ? 1 : 0;
    }

    private Optional<CompressionStorage> resolve() {
        final ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CompressionStorageDiskItem disk)) {
            return Optional.empty();
        }
        return disk.resolve(RefinedStorageApi.INSTANCE.getStorageRepository(player.level()), stack)
            .filter(CompressionStorage.class::isInstance)
            .map(CompressionStorage.class::cast);
    }

    private static int flags(final CompressionStorage storage) {
        int value = storage.getFamily().isConfigured() ? 1 : 0;
        value |= storage.isConnected() ? 2 : 0;
        value |= storage.getBaseUnits() == 0 ? 4 : 0;
        value |= storage.isQuarantined() ? 8 : 0;
        return value;
    }

    @Override
    public void set(final int index, final int value) {
    }

    @Override
    public int getCount() {
        return DATA_SIZE;
    }
}
