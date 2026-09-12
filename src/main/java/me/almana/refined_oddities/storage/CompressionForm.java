package me.almana.refined_oddities.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record CompressionForm(ResourceLocation itemId, long weight) {
    public static final Codec<CompressionForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("item").forGetter(CompressionForm::itemId),
        Codec.LONG.fieldOf("weight").forGetter(CompressionForm::weight)
    ).apply(instance, CompressionForm::new));
}
