package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.entity.QuiltSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * 给玩家同步一份「本次枕头睡眠选中的被子坐标」。
 */
@Mixin(Player.class)
public abstract class PlayerQuiltSleepMixin implements QuiltSleepData
{
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_QUILT_POS =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void northeast_china_delight$defineQuiltData(CallbackInfo callback)
    {
        ncd$player().getEntityData().define(DATA_QUILT_POS, Optional.empty());
    }

    @Override
    public Optional<BlockPos> ncd$getQuiltPos()
    {
        return ncd$player().getEntityData().get(DATA_QUILT_POS);
    }

    @Override
    public void ncd$setQuiltPos(@Nullable BlockPos pos)
    {
        ncd$player().getEntityData().set(DATA_QUILT_POS,
                pos == null ? Optional.empty() : Optional.of(pos.immutable()));
    }

    private Player ncd$player()
    {
        return (Player) (Object) this;
    }
}
