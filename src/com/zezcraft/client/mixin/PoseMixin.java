package com.zezcraft.client.mixin;

import com.zezcraft.client.St;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class PoseMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", at = @At("HEAD"), cancellable = true)
    private static void zez$pose(Avatar a, ItemStack s, InteractionHand h, CallbackInfoReturnable<HumanoidModel.ArmPose> ci) {
        if (h != InteractionHand.MAIN_HAND) return;
        St.Bind b = St.bind(s);
        if (b != null && (b.flags & 4) != 0) ci.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
    }
}
