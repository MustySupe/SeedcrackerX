package kaptainwutax.seedcrackerX.ui;

import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.config.ConfigScreen;
import kaptainwutax.seedcrackerX.cracker.storage.DataStorage;
import kaptainwutax.seedcrackerX.cracker.storage.TimeMachine;
import kaptainwutax.seedcrackerX.util.StructureLocator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Touch friendly control panel: big buttons, progress bars and hints.
 * Opened with the "SeedCracker" button in the pause menu.
 */
public class SeedCrackerScreen extends Screen {

    private final Screen parent;
    private Button toggleButton;
    private Button copyButton;

    public SeedCrackerScreen(Screen parent) {
        super(Component.literal("SeedCrackerX"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int bh = this.height < 250 ? 24 : 32;
        int gap = 4;
        int bw = Math.min(this.width / 2 - 15, 200);
        int leftX = this.width / 2 - bw - gap;
        int rightX = this.width / 2 + gap;
        int row3 = this.height - bh - 6;
        int row2 = row3 - bh - gap;
        int row1 = row2 - bh - gap;

        this.copyButton = this.addRenderableWidget(Button.builder(copyLabel(), b -> copySeed())
                .bounds(leftX, row1, bw, bh).build());

        this.addRenderableWidget(Button.builder(Component.literal("Структуры рядом"), b -> openStructures())
                .bounds(rightX, row1, bw, bh).build());

        this.toggleButton = this.addRenderableWidget(Button.builder(toggleLabel(), b -> toggleActive())
                .bounds(leftX, row2, bw, bh).build());

        this.addRenderableWidget(Button.builder(Component.literal("Сбросить данные"), b -> SeedCracker.get().reset())
                .bounds(rightX, row2, bw, bh).build());

        this.addRenderableWidget(Button.builder(Component.literal("Настройки"), b -> openSettings())
                .bounds(leftX, row3, bw, bh).build());

        this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> this.onClose())
                .bounds(rightX, row3, bw, bh).build());
    }

    private Component toggleLabel() {
        return Component.literal(Config.get().active ? "Крякер: ВКЛ" : "Крякер: ВЫКЛ");
    }

    private Component copyLabel() {
        return Component.literal(getSeedToCopy() != null ? "Копировать сид" : "Сид ещё не найден");
    }

    /** World seed if known, otherwise the structure seed, otherwise null. */
    private String getSeedToCopy() {
        TimeMachine tm = SeedCracker.get().getDataStorage().getTimeMachine();
        if (!tm.worldSeeds.isEmpty()) {
            return String.valueOf(tm.worldSeeds.iterator().next());
        }
        if (!tm.structureSeeds.isEmpty()) {
            return String.valueOf(tm.structureSeeds.iterator().next());
        }
        return null;
    }

    private void copySeed() {
        String seed = getSeedToCopy();
        if (seed != null && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(seed);
        }
    }

    private void toggleActive() {
        Config.get().active = !Config.get().active;
        Config.save();
        this.toggleButton.setMessage(toggleLabel());
    }

    private void openSettings() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ConfigScreen().getConfigScreenByCloth(this));
        }
    }

    private void openStructures() {
        if (this.minecraft == null) return;
        TimeMachine tm = SeedCracker.get().getDataStorage().getTimeMachine();
        if (!tm.worldSeeds.isEmpty() && !StructureLocator.isRunning()
                && (!StructureLocator.hasResult() || StructureLocator.getSeed() != tm.worldSeeds.iterator().next())) {
            LocalPlayer player = this.minecraft.player;
            int px = player == null ? 0 : (int) player.getX();
            int pz = player == null ? 0 : (int) player.getZ();
            StructureLocator.compute(tm.worldSeeds.iterator().next(), px, pz, null);
        }
        this.minecraft.setScreen(new StructuresScreen(this));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        DataStorage storage = SeedCracker.get().getDataStorage();
        this.copyButton.setMessage(copyLabel());

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFFFF);

        int barW = Math.min(this.width - 40, 320);
        int barX = this.width / 2 - barW / 2;
        int y = 20;

        y = drawBar(graphics, "Данные структур", storage.getBaseBits(), storage.getWantedBits(), barX, y, barW);
        y = drawBar(graphics, "Для расчёта сида", storage.getLiftingBits(), 40, barX, y, barW);

        TimeMachine tm = storage.getTimeMachine();
        String status;
        int statusColor = 0xFFFFFFFF;
        if (tm.worldSeeds.size() == 1) {
            status = "Мировой сид: " + tm.worldSeeds.iterator().next();
            statusColor = 0xFF55FF55;
        } else if (!tm.worldSeeds.isEmpty()) {
            status = "Мировых сидов найдено: " + tm.worldSeeds.size();
            statusColor = 0xFFFFFF55;
        } else if (!tm.structureSeeds.isEmpty()) {
            status = "Структурный сид: " + tm.structureSeeds.iterator().next();
            statusColor = 0xFFFFFF55;
        } else {
            status = "Сид ещё не найден";
        }
        graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, y, statusColor);
        y += 14;

        int hintBottom = this.height - (this.height < 250 ? 24 : 32) * 3 - 20;
        List<String> hints = Hints.build(storage);
        for (String hint : hints) {
            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(hint), this.width - 40);
            for (FormattedCharSequence line : wrapped) {
                if (y + 9 > hintBottom) return;
                graphics.drawString(this.font, line, 20, y, 0xFFCCCCCC);
                y += 10;
            }
            y += 2;
        }
    }

    private int drawBar(GuiGraphics graphics, String label, double value, double max, int x, int y, int w) {
        double clamped = Math.max(0, Math.min(value, max));
        int filled = (int) Math.round(w * (clamped / max));
        int barH = 10;

        graphics.fill(x - 1, y - 1, x + w + 1, y + barH + 1, 0xFF000000);
        graphics.fill(x, y, x + w, y + barH, 0xFF333333);
        int color = value >= max ? 0xFF55FF55 : 0xFF3399FF;
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + barH, color);
        }
        String text = label + ": " + (int) value + " / " + (int) max;
        graphics.drawCenteredString(this.font, Component.literal(text), x + w / 2, y + 1, 0xFFFFFFFF);
        return y + barH + 4;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
