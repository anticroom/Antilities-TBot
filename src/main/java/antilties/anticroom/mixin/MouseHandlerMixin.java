package antilties.anticroom.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void onTurn(double accumulatedDX, double accumulatedDY, CallbackInfo ci) {
        // AimAssist is currently being handled in AntiltiesClient.java via HudRenderCallback.
        // Leave this empty so they don't fight each other.
    }
}