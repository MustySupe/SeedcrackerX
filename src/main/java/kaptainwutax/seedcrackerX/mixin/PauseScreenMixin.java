package kaptainwutax.seedcrackerX.mixin;

import kaptainwutax.seedcrackerX.ui.SeedCrackerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a big "SeedCracker" button to the pause menu, so that the panel can be opened without chat commands.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedcracker$addButton(CallbackInfo ci) {
        int w = Math.min(this.width / 3, 160);
        int h = this.height < 250 ? 24 : 32;
        this.addRenderableWidget(Button.builder(Component.literal("SeedCracker"),
                        b -> Minecraft.getInstance().setScreen(new SeedCrackerScreen(this)))
                .bounds(6, 6, w, h).build());
    }
}
