package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.block.SleepingPillowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@code ServerPlayer#bedBlocked} 会按床的 FACING 检查另一侧上方，枕头只有一格，
 * 且可睡侧由枕头自己的规则决定；这里把枕头实例的阻挡检查换成 D 方向上方空间。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerPillowSleepMixin
{
    @Inject(method = "bedBlocked", at = @At("HEAD"), cancellable = true)
    private void northeast_china_delight$checkPillowHeadspace(BlockPos pos, Direction direction,
                                                               CallbackInfoReturnable<Boolean> callback)
    {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof SleepingPillowBlock))
        {
            return;
        }

        Direction sleepSide = SleepingPillowBlock.sleepSide(state, player.level(), pos);
        callback.setReturnValue(SleepingPillowBlock.isHeadspaceBlocked(player.level(), pos, sleepSide));
    }
}