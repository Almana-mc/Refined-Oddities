package me.almana.refined_oddities.content;

import me.almana.refined_oddities.Refined_oddities;
import me.almana.refined_oddities.item.BulkStorageDiskItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Refined_oddities.MODID);
    public static final DeferredHolder<Item, Item> BULK_STORAGE_HOUSING = REGISTER.registerSimpleItem(
        "bulk_storage_housing",
        new Item.Properties()
    );
    public static final DeferredHolder<Item, Item> BULK_STORAGE_PART = REGISTER.registerSimpleItem(
        "bulk_storage_part",
        new Item.Properties()
    );
    public static final DeferredHolder<Item, BulkStorageDiskItem> BULK_STORAGE_DISK = REGISTER.register(
        "bulk_storage_disk",
        BulkStorageDiskItem::new
    );

    static {
        REGISTER.addAlias(id("compression_storage_disk"), id("bulk_storage_disk"));
        REGISTER.addAlias(id("compression_storage_housing"), id("bulk_storage_housing"));
        REGISTER.addAlias(id("compression_storage_part"), id("bulk_storage_part"));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(Refined_oddities.MODID, path);
    }

    private ModItems() {
    }
}
