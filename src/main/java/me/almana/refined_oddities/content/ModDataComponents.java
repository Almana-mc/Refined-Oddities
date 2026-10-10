package me.almana.refined_oddities.content;

import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import java.util.List;
import me.almana.refined_oddities.Refined_oddities;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE,
        Refined_oddities.MODID
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemResource>>> BULK_FILTER =
        REGISTER.registerComponentType("bulk_filter", builder -> builder
            .persistent(ResourceCodecs.ITEM_CODEC.listOf())
            .networkSynchronized(ResourceCodecs.ITEM_STREAM_CODEC.apply(ByteBufCodecs.list())));

    private ModDataComponents() {
    }
}
