package com.gunmu.northeast_china_delight.entity;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
//? if >=1.20.5 {
/*import net.minecraft.network.syncher.SynchedEntityData;
*///?}

/**
 * 小板凳上的座位：一个看不见的实体，玩家坐上去就是骑着它。
 *
 * <p>实体放在板凳那格的底面中心。人坐的高度不走原版的「乘客挂载点」（1.20.1 和 1.21.1 算法不一样），
 * 直接在 {@link #positionRider} 里摆：坐姿时屁股大约在脚上方 0.55 格，板凳面在 6.5 像素（约 0.4 格），
 * 所以脚放在这一格底面往下 0.15。</p>
 *
 * <p>没人坐了、或者板凳没了，就自己消失；不存盘（{@code noSave}），下线再上来不会留下空座位。</p>
 */
public class SeatEntity extends Entity {

    private static final double RIDER_Y = -0.15;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty()
                || !level().getBlockState(blockPosition()).is(ModBlocks.SMALL_STOOL.get()))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (hasPassenger(passenger)) {
            callback.accept(passenger, getX(), getY() + RIDER_Y, getZ());
        }
    }

    //? if <1.20.5 {
    @Override
    protected void defineSynchedData() {
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
