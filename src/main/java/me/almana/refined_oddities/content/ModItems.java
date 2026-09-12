package me.almana.refined_oddities.content;

import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.item.CompressionStorageDiskItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Refined_oddities.MODID);
    public static final DeferredHolder<Item, Item> COMPRESSION_STORAGE_HOUSING = REGISTER.registerSimpleItem(
        "compression_storage_housing",
        new Item.Properties()
    );
    public static final DeferredHolder<Item, Item> COMPRESSION_STORAGE_PART = REGISTER.registerSimpleItem(
        "compression_storage_part",
        new Item.Properties()
    );
    public static final DeferredHolder<Item, CompressionStorageDiskItem> COMPRESSION_STORAGE_DISK = REGISTER.register(
        "compression_storage_disk",
        CompressionStorageDiskItem::new
    );

    private ModItems() {
    }
}
