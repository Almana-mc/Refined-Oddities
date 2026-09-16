package me.almana.refined_oddities.command;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import me.almana.refined_oddities.content.ModItems;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BulkDiskCommand {
    private BulkDiskCommand() {
    }

    public static void register(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("refinedodditities")
            .requires(source -> source.hasPermission(2) && source.getEntity() instanceof Player)
            .then(Commands.literal("disk")
                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                    .executes(context -> giveDisk(
                        context.getSource(),
                        ItemArgument.getItem(context, "item").createItemStack(1, false)
                    )))));
    }

    private static int giveDisk(final CommandSourceStack source,
                                final ItemStack selected) {
        final Player player = (Player) source.getEntity();
        final ItemStack disk = new ItemStack(ModItems.BULK_STORAGE_DISK.get());
        disk.inventoryTick(player.level(), player, player.getInventory().selected, false);
        final CompressionStorage storage = (CompressionStorage) ModItems.BULK_STORAGE_DISK.get()
            .resolve(RefinedStorageApi.INSTANCE.getStorageRepository(player.level()), disk)
            .orElseThrow();
        if (CompressionConfigurationMenu.configure(storage, selected)
            != CompressionStorage.ConfigurationResult.SUCCESS) {
            source.sendFailure(Component.literal("That item cannot configure a bulk disk"));
            return 0;
        }
        storage.insert(
            storage.resource(storage.getFamily().forms().getFirst()),
            storage.getCapacity(),
            Action.EXECUTE,
            Actor.EMPTY
        );
        if (!player.getInventory().add(disk)) {
            player.drop(disk, false);
        }
        source.sendSuccess(
            () -> Component.literal("Gave a full bulk disk for ").append(selected.getHoverName()),
            true
        );
        return 1;
    }
}
