package me.almana.refined_oddities.client;

import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import com.refinedmods.refinedstorage.common.support.Sprites;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.almana.refined_oddities.menu.CompressionConfigurationMenu;
import me.almana.refined_oddities.storage.CompressionStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class CompressionConfigurationScreen
    extends AbstractBaseScreen<CompressionConfigurationMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "refinedstorage",
        "textures/gui/storage.png"
    );
    private static final ResourceLocation SCROLLBAR = ResourceLocation.fromNamespaceAndPath(
        "refinedstorage",
        "widget/small_scrollbar"
    );
    private static final ResourceLocation SCROLLBAR_CLICKED = ResourceLocation.fromNamespaceAndPath(
        "refinedstorage",
        "widget/small_scrollbar_clicked"
    );
    private static final ResourceLocation SCROLLBAR_DISABLED = ResourceLocation.fromNamespaceAndPath(
        "refinedstorage",
        "widget/small_scrollbar_disabled"
    );
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int ENABLED_COLOR = 0xFF27833A;
    private static final int ENABLED_SLOT_COLOR = 0xFF4C8F52;
    private static final int DISABLED_SLOT_COLOR = 0xFFA84C4C;
    private static final int STATE_COLUMNS = 8;
    private static final int VISIBLE_STATE_ROWS = 2;
    private static final int STATE_X = 7;
    private static final int STATE_Y = 19;
    private static final int STATE_SIZE = 18;
    private static final int STATE_WIDTH = STATE_COLUMNS * STATE_SIZE;
    private static final int STATE_HEIGHT = VISIBLE_STATE_ROWS * STATE_SIZE;
    private static final int SCROLLBAR_X = 158;
    private static final int SCROLLBAR_WIDTH = 7;
    private static final int SCROLLBAR_HEIGHT = 15;
    private static final int INVENTORY_Y = 121;
    private static final int INVENTORY_TEXTURE_Y = 141;
    private static final int INVENTORY_TEXTURE_HEIGHT = 57;

    private int scrollRow;
    private boolean draggingScrollbar;

    public CompressionConfigurationScreen(final CompressionConfigurationMenu menu,
                                          final Inventory inventory,
                                          final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 203;
        inventoryLabelY = 109;
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        scrollRow = Mth.clamp(scrollRow, 0, maxScrollRow());
    }

    @Override
    protected void renderBg(final GuiGraphics graphics,
                            final float partialTick,
                            final int mouseX,
                            final int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, INVENTORY_Y);
        graphics.blit(TEXTURE, leftPos, topPos + INVENTORY_Y, 0, INVENTORY_TEXTURE_Y,
            imageWidth, INVENTORY_TEXTURE_HEIGHT);
        graphics.blit(TEXTURE, leftPos, topPos + INVENTORY_Y + INVENTORY_TEXTURE_HEIGHT,
            0, INVENTORY_TEXTURE_Y + INVENTORY_TEXTURE_HEIGHT,
            imageWidth, imageHeight - INVENTORY_Y - INVENTORY_TEXTURE_HEIGHT);
        graphics.fill(leftPos + 7, topPos + 37, leftPos + 169, topPos + INVENTORY_Y, PANEL_COLOR);
        graphics.fill(leftPos + 151, topPos + 18, leftPos + 170, topPos + 37, PANEL_COLOR);
        graphics.blitSprite(Sprites.SLOT, leftPos + 79, topPos + 72, STATE_SIZE, STATE_SIZE);
        drawStateTiles(graphics, mouseX, mouseY);
        drawScrollbar(graphics);
    }

    private void drawStateTiles(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final int firstIndex = scrollRow * STATE_COLUMNS;
        for (int visibleIndex = 0; visibleIndex < STATE_COLUMNS * VISIBLE_STATE_ROWS; visibleIndex++) {
            final int x = leftPos + STATE_X + visibleIndex % STATE_COLUMNS * STATE_SIZE;
            final int y = topPos + STATE_Y + visibleIndex / STATE_COLUMNS * STATE_SIZE;
            graphics.blitSprite(Sprites.SLOT, x, y, STATE_SIZE, STATE_SIZE);
            final int index = firstIndex + visibleIndex;
            if (index < menu.stateCount()) {
                drawState(graphics, index, x, y, mouseX, mouseY);
            }
        }
    }

    private void drawState(final GuiGraphics graphics,
                           final int index,
                           final int x,
                           final int y,
                           final int mouseX,
                           final int mouseY) {
        graphics.fill(x + 1, y + 1, x + 17, y + 17,
            menu.isStateEnabled(index) ? ENABLED_SLOT_COLOR : DISABLED_SLOT_COLOR);
        graphics.renderItem(menu.getStateStack(index), x + 1, y + 1);
        if (mouseX >= x && mouseX < x + STATE_SIZE && mouseY >= y && mouseY < y + STATE_SIZE) {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
        }
    }

    private void drawScrollbar(final GuiGraphics graphics) {
        final int x = leftPos + SCROLLBAR_X;
        final int y = topPos + STATE_Y;
        drawInset(graphics, x - 1, y - 1, SCROLLBAR_WIDTH + 2, STATE_HEIGHT + 2);
        final int maxScroll = maxScrollRow();
        final int thumbY = y + (maxScroll == 0
            ? 0
            : scrollRow * (STATE_HEIGHT - SCROLLBAR_HEIGHT) / maxScroll);
        final ResourceLocation sprite = maxScroll == 0
            ? SCROLLBAR_DISABLED
            : draggingScrollbar ? SCROLLBAR_CLICKED : SCROLLBAR;
        graphics.blitSprite(sprite, x, thumbY, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);
    }

    private static void drawInset(final GuiGraphics graphics,
                                  final int x,
                                  final int y,
                                  final int width,
                                  final int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        drawCentered(graphics, instruction(), 59, 0xFF404040);
        if (menu.resultCode() > 0) {
            final CompressionStorage.ConfigurationResult result = CompressionStorage.ConfigurationResult.values()[
                menu.resultCode() - 1
            ];
            final String key = "gui.refined_oddities.result." + result.name().toLowerCase(Locale.ROOT);
            drawCentered(graphics, Component.translatable(key), 98,
                result == CompressionStorage.ConfigurationResult.SUCCESS ? ENABLED_COLOR : 0xFFB02020);
        }
    }

    private Component instruction() {
        final int flags = menu.flags();
        if ((flags & 8) != 0) {
            return Component.translatable("gui.refined_oddities.quarantined");
        }
        if ((flags & 2) != 0) {
            return Component.translatable("gui.refined_oddities.connected");
        }
        if ((flags & 4) == 0) {
            return Component.translatable("gui.refined_oddities.not_empty");
        }
        return Component.translatable("gui.refined_oddities.select_item");
    }

    private void drawCentered(final GuiGraphics graphics,
                              final Component text,
                              final int y,
                              final int color) {
        graphics.drawString(font, text, (imageWidth - font.width(text)) / 2, y, color, false);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (!renderStateTooltip(graphics, mouseX, mouseY)) {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private boolean renderStateTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final int index = stateAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        final ItemStack stack = menu.getStateStack(index);
        final List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, stack));
        lines.add(Component.translatable(menu.isStateEnabled(index)
            ? "gui.refined_oddities.state_enabled"
            : "gui.refined_oddities.state_disabled").withStyle(menu.isStateEnabled(index)
                ? ChatFormatting.GREEN
                : ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.refined_oddities.toggle_hint").withStyle(ChatFormatting.DARK_GRAY));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY, stack);
        return true;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        final int index = stateAt(mouseX, mouseY);
        if (index >= 0) {
            if (button == 1 && menu.canChangeSelection()) {
                sendButton(CompressionConfigurationMenu.TOGGLE_FORM_BUTTON_OFFSET - index);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX,
                                final double mouseY,
                                final int button,
                                final double dragX,
                                final double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (draggingScrollbar && button == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX,
                                 final double mouseY,
                                 final double scrollX,
                                 final double scrollY) {
        if (scrollY != 0 && isOverStateArea(mouseX, mouseY) && maxScrollRow() > 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int stateAt(final double mouseX, final double mouseY) {
        if (!isOverStateArea(mouseX, mouseY)) {
            return -1;
        }
        final int column = (int) (mouseX - leftPos - STATE_X) / STATE_SIZE;
        final int row = (int) (mouseY - topPos - STATE_Y) / STATE_SIZE;
        final int index = (scrollRow + row) * STATE_COLUMNS + column;
        return index < menu.stateCount() ? index : -1;
    }

    private boolean isOverStateArea(final double mouseX, final double mouseY) {
        return mouseX >= leftPos + STATE_X
            && mouseX < leftPos + STATE_X + STATE_WIDTH
            && mouseY >= topPos + STATE_Y
            && mouseY < topPos + STATE_Y + STATE_HEIGHT;
    }

    private boolean isOverScrollbar(final double mouseX, final double mouseY) {
        return maxScrollRow() > 0
            && mouseX >= leftPos + SCROLLBAR_X - 1
            && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH + 1
            && mouseY >= topPos + STATE_Y - 1
            && mouseY < topPos + STATE_Y + STATE_HEIGHT + 1;
    }

    private void updateScrollFromMouse(final double mouseY) {
        final double position = mouseY - topPos - STATE_Y - SCROLLBAR_HEIGHT / 2.0;
        final int travel = STATE_HEIGHT - SCROLLBAR_HEIGHT;
        scrollRow = travel == 0
            ? 0
            : Mth.clamp((int) Math.round(position * maxScrollRow() / travel), 0, maxScrollRow());
    }

    private int totalStateRows() {
        return Math.max(1, (menu.stateCount() + STATE_COLUMNS - 1) / STATE_COLUMNS);
    }

    private int maxScrollRow() {
        return Math.max(0, totalStateRows() - VISIBLE_STATE_ROWS);
    }

    public Rect2i ghostSlotBounds() {
        return new Rect2i(leftPos + 79, topPos + 72, STATE_SIZE, STATE_SIZE);
    }

    public boolean canSelectGhostItem() {
        return menu.canChangeSelection();
    }

    public boolean selectGhostItem(final ItemStack stack) {
        if (!canSelectGhostItem() || stack.isEmpty() || !stack.isComponentsPatchEmpty()) {
            return false;
        }
        sendButton(CompressionConfigurationMenu.SELECT_ITEM_BUTTON_OFFSET
            + BuiltInRegistries.ITEM.getId(stack.getItem()));
        return true;
    }

    private void sendButton(final int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
