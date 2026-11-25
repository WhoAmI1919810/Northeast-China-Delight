package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.block.SleepingPillowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版 {@code LivingEntity#setPosToBed} 按原版床高度（9 像素）把入睡位置写死在 {@code y + 11/16}；
 * 枕头只有 4.5 像素高，继续沿用原版偏移会让头部悬空在枕头上方。这里改为按枕头高度对应的偏移落位。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPillowSleepMixin {

    @Inject(method = "setPosToBed", at = @At("HEAD"), cancellable = true)
    private void northeast_china_delight$adjustPillowSleepHeight(BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().getBlockState(pos).getBlock() instanceof SleepingPillowBlock) {
            self.setPos(pos.getX() + 0.5D, pos.getY() + SleepingPillowBlock.SLEEP_POS_Y_OFFSET, pos.getZ() + 0.5D);
            ci.cancel();
        }
    }
}
