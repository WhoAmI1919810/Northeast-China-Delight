package com.gunmu.northeast_china_delight.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 养生：参鸡汤的状态效果。按当前血量占最大生命值的比例分档回血——血越少回得越勤（残血时保命用），血快满时就慢下来。分档（每 N tick 回 1 点）：血量 &gt;70% →
 * 25 tick； 50%~70% → 10 tick； 25%~50% → 6 tick； &lt;25% → 3 tick。
 */
public class NourishingEffect extends MobEffect {

    public NourishingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8B93B);
    }

    @Override
    //? if <1.20.5 {
    /*public void applyEffectTick(LivingEntity entity, int amplifier) {
        this.nourish(entity);
    }*/
    //?} else {
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        this.nourish(entity);
        return true;
    }
    //?}

    /** 真正的回血逻辑（分档在里面按血量做，两个版本共用） */
    private void nourish(LivingEntity entity) {
        float max = entity.getMaxHealth();
        float health = entity.getHealth();
        if (health >= max || max <= 0.0F) {
            return;
        }
        float ratio = health / max;
        int interval;
        if (ratio > 0.70F) {
            interval = 25;
        } else if (ratio > 0.50F) {
            interval = 10;
        } else if (ratio > 0.25F) {
            interval = 6;
        } else {
            interval = 3;
        }
        if (entity.tickCount % interval == 0) {
            entity.heal(1.0F);
        }
    }

    /** 每 tick 都触发 applyEffectTick（分档逻辑在里面按血量做） */
    //? if <1.20.2 {
    /*@Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    *///?} else {
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
    //?}
}
