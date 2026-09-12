package me.almana.refined_oddities.recipe;

import net.minecraft.resources.ResourceLocation;

public record CompressionLink(ResourceLocation lower, ResourceLocation higher, int ratio) {
}
