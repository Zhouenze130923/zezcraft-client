package com.zezcraft.client.mixin;

import com.zezcraft.client.St;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomModelDataProperty.class)
public class CmdMixin {
    @Shadow @Final private int index;

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void zez$get(ItemStack s, ClientLevel l, ItemOwner o, int seed, CallbackInfoReturnable<Float> ci) {
        if (index == 5) {
            if (o instanceof net.minecraft.world.entity.LivingEntity le && le.getMainHandItem() == s) {
                St.Bind b = St.bind(s);
                if (b != null && (b.flags & 4) != 0) ci.setReturnValue(1f);
            }
            return;
        }
        var p = Minecraft.getInstance().player;
        if (p == null || o != p || s != p.getMainHandItem()) return;
        St.Bind b = St.bind(s);
        if (b == null) return;
        ci.setReturnValue(St.value(b, index, ci.getReturnValueF()));
    }
}
