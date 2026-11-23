package kaptainwutax.seedcrackerX.ui;

import kaptainwutax.seedcrackerX.util.StructureLocator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Shows the nearest villages, strongholds and ruined portals for the found world seed.
 */
public class StructuresScreen extends Screen {

    private final Screen parent;

    public StructuresScreen(Screen parent) {
        super(Component.literal("Структуры рядом"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int bw = Math.min(this.width / 2 - 15, 200);
        int bh = this.height < 250 ? 24 : 32;
        int y = this.height - bh - 8;

        this.addRenderableWidget(Button.builder(Component.literal("Копировать список"), b -> {
            if (this.minecraft != null && StructureLocator.hasResult()) {
                this.minecraft.keyboardHandler.setClipboard(StructureLocator.asText());
            }
        }).bounds(this.width / 2 - bw - 5, y, bw, bh).build());

        this.addRenderableWidget(Button.builder(Component.literal("Назад"), b -> this.onClose())
                .bounds(this.width / 2 + 5, y, bw, bh).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int y = 30;
        if (StructureLocator.isRunning()) {
            graphics.drawCenteredString(this.font, Component.literal("Считаю..."), this.width / 2, y, 0xFFFFFF55);
            return;
        }

        if (StructureLocator.getError() != null) {
            graphics.drawCenteredString(this.font, Component.literal("Не удалось посчитать: " + StructureLocator.getError()), this.width / 2, y, 0xFFFF5555);
            return;
        }

        if (!StructureLocator.hasResult()) {
            graphics.drawCenteredString(this.font, Component.literal("Сначала найдите мировой сид"), this.width / 2, y, 0xFFFFFF55);
            return;
        }

        List<StructureLocator.Found> list = StructureLocator.getResult();
        if (list.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("Рядом ничего не найдено"), this.width / 2, y, 0xFFFFFF55);
            return;
        }

        graphics.drawCenteredString(this.font, Component.literal("В скобках расстояние в блоках от вас"), this.width / 2, y, 0xFFAAAAAA);
        y += 16;
        int maxLines = Math.max(1, (this.height - y - 50) / 12);
        int shown = 0;
        for (StructureLocator.Found f : list) {
            if (shown >= maxLines) break;
            graphics.drawString(this.font, Component.literal(f.format()), 20, y, 0xFFFFFFFF);
            y += 12;
            shown++;
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
