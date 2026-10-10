package com.gunmu.northeast_china_delight.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 状态效果的收敛层。 1.20.1 的效果注册表就是 {@code MobEffect} 本身（{@code MobEffects.X} 是 MobEffect，
 * {@code hasEffect} / {@code MobEffectInstance} 也都要 MobEffect）；1.21 起统一换成
 * {@code Holder<MobEffect>}。这里把两种写法收成一套调用。
 */
public final class DdEffects {

    private DdEffects() {
    }

    /** 原版效果常量 → Holder（1.20.1 传 MobEffect，1.21 直接原样返回） */
    //? if <1.20.5 {
    public static Holder<MobEffect> hold(MobEffect effect) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }
    //?} else {
    /*public static Holder<MobEffect> hold(Holder<MobEffect> effect) {
        return effect;
    }
    *///?}

    @SuppressWarnings("unchecked")
    public static Holder<MobEffect> hold(Supplier<MobEffect> effect) {
        //? if <1.20.2 {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get());
        //?} else {
        /*return (Holder<MobEffect>) effect;
        *///?}
    }

    public static Optional<Holder<MobEffect>> holder(ResourceLocation id) {
        //? if <1.20.5 {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, id))
                .map(holder -> (Holder<MobEffect>) holder);
        //?} else {
        /*return BuiltInRegistries.MOB_EFFECT.getHolder(id)
                .map(holder -> (Holder<MobEffect>) holder);
        *///?}
    }

    public static MobEffectInstance instance(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        //? if <1.20.5 {
        return new MobEffectInstance(effect.value(), durationTicks, amplifier, false, true, true);
        //?} else {
        /*return new MobEffectInstance(effect, durationTicks, amplifier, false, true, true);
        *///?}
    }

    public static MobEffectCategory category(MobEffectInstance instance) {
        //? if <1.20.5 {
        return instance.getEffect().getCategory();
        //?} else {
        /*return instance.getEffect().value().getCategory();
        *///?}
    }

    public static boolean hasEffect(LivingEntity entity, Holder<MobEffect> effect) {
        //? if <1.20.5 {
        return entity.hasEffect(effect.value());
        //?} else {
        /*return entity.hasEffect(effect);
        *///?}
    }

    public static boolean hasEffect(LivingEntity entity, Supplier<MobEffect> effect) {
        //? if <1.20.5 {
        return entity.hasEffect(effect.get());
        //?} else {
        /*return entity.hasEffect(hold(effect));
        *///?}
    }

    public static MobEffectInstance getEffect(LivingEntity entity, Holder<MobEffect> effect) {
        //? if <1.20.5 {
        return entity.getEffect(effect.value());
        //?} else {
        /*return entity.getEffect(effect);
        *///?}
    }

    public static MobEffectInstance getEffect(LivingEntity entity, Supplier<MobEffect> effect) {
        //? if <1.20.5 {
        return entity.getEffect(effect.get());
        //?} else {
        /*return entity.getEffect(hold(effect));
        *///?}
    }

    public static void removeEffect(LivingEntity entity, Holder<MobEffect> effect) {
        //? if <1.20.5 {
        entity.removeEffect(effect.value());
        //?} else {
        /*entity.removeEffect(effect);
        *///?}
    }

    public static void removeEffect(LivingEntity entity, Supplier<MobEffect> effect) {
        //? if <1.20.5 {
        entity.removeEffect(effect.get());
        //?} else {
        /*entity.removeEffect(hold(effect));
        *///?}
    }
}
