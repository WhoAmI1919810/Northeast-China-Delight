package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.entity.QuiltSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 给玩家同步「本次枕头睡眠选中的被子坐标与睡眠朝向」。
 */
@Mixin(Player.class)
public abstract class PlayerQuiltSleepMixin implements QuiltSleepData
{
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_QUILT_POS =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_SLEEP_SIDE =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V",
            at = @At("TAIL"))
    private void northeast_china_delight$defineQuiltData(SynchedEntityData.Builder builder, CallbackInfo callback)
    {
        builder.define(DATA_QUILT_POS, Optional.empty());
        builder.define(DATA_SLEEP_SIDE, -1);
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

    @Override
    public Optional<Direction> ncd$getSleepSide()
    {
        int raw = ncd$player().getEntityData().get(DATA_SLEEP_SIDE);
        return raw >= 0 && raw < 6 ? Optional.of(Direction.from3DDataValue(raw)) : Optional.empty();
    }

    @Override
    public void ncd$setSleepSide(@Nullable Direction side)
    {
        ncd$player().getEntityData().set(DATA_SLEEP_SIDE, side == null ? -1 : side.get3DDataValue());
    }

    private Player ncd$player()
    {
        return (Player) (Object) this;
    }
}
