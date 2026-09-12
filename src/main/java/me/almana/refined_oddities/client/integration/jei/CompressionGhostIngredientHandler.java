package me.almana.refined_oddities.client.integration.jei;

import java.util.List;
import me.almana.refined_oddities.client.CompressionConfigurationScreen;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

public final class CompressionGhostIngredientHandler
    implements IGhostIngredientHandler<CompressionConfigurationScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(final CompressionConfigurationScreen screen,
                                               final ITypedIngredient<I> ingredient,
                                               final boolean doStart) {
        final ItemStack stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        if (!screen.canSelectGhostItem() || stack.isEmpty() || !stack.isComponentsPatchEmpty()) {
            return List.of();
        }
        return List.of(new Target<>() {
            @Override
            public Rect2i getArea() {
                return screen.ghostSlotBounds();
            }

            @Override
            public void accept(final I accepted) {
                VanillaTypes.ITEM_STACK.castIngredient(accepted).ifPresent(screen::selectGhostItem);
            }
        });
    }

    @Override
    public <I> boolean quickMove(final CompressionConfigurationScreen screen,
                                 final ITypedIngredient<I> ingredient) {
        return ingredient.getIngredient(VanillaTypes.ITEM_STACK)
            .filter(screen::selectGhostItem)
            .isPresent();
    }

    @Override
    public void onComplete() {
    }
}
