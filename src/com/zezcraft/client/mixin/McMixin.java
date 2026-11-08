package com.zezcraft.client.mixin;

import com.zezcraft.client.St;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class McMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void zez$atk(CallbackInfoReturnable<Boolean> ci) {
        St.Bind b = St.held();
        if (b != null && (b.flags & 1) != 0) ci.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void zez$cont(boolean d, CallbackInfo ci) {
        St.Bind b = St.held();
        if (b != null && (b.flags & 1) != 0) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void zez$use(CallbackInfo ci) {
        St.Bind b = St.held();
        if (b != null && (b.flags & 2) != 0) ci.cancel();
    }
}
