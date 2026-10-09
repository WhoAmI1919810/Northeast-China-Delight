package com.gunmu.northeast_china_delight.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性修饰符的收敛层。
 *
 * <p>1.20.1 的 {@code AttributeModifier} 认 UUID（{@code new AttributeModifier(UUID, String, double, Operation)}），
 * 1.21 起改成认 {@code ResourceLocation}。这里把「拿 id 换算 / 构造 / 查询 / 摘除 / 取值」收成一套调用，
 * UUID 用名字派生（同名 id 永远得到同一个 UUID，跨存档稳定）。</p>
 */
public final class DdAttributes {

    //? if <1.20.5 {
    /*private static final Map<ResourceLocation, UUID> UUID_CACHE = new ConcurrentHashMap<>();

    private static UUID uuidOf(ResourceLocation id) {
        return UUID_CACHE.computeIfAbsent(id,
                location -> UUID.nameUUIDFromBytes(location.toString().getBytes(StandardCharsets.UTF_8)));
    }*/
    //?}

    private DdAttributes() {
    }

    /** 乘法基值运算：1.20.1 叫 MULTIPLY_BASE，1.20.5 起改叫 ADD_MULTIPLIED_BASE */
    //? if <1.20.5 {
    /*public static final AttributeModifier.Operation OP_MULTIPLY_BASE = AttributeModifier.Operation.MULTIPLY_BASE;
    *///?} else {
    public static final AttributeModifier.Operation OP_MULTIPLY_BASE = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    //?}

    /** 造一个修饰符 */
    public static AttributeModifier create(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        //? if <1.20.5 {
        /*return new AttributeModifier(uuidOf(id), id.toString(), amount, operation);*/
        //?} else {
        return new AttributeModifier(id, amount, operation);
        //?}
    }

    /** 查已经挂在这个属性上的修饰符（没有就是 null） */
    public static AttributeModifier get(AttributeInstance instance, ResourceLocation id) {
        //? if <1.20.5 {
        /*return instance.getModifier(uuidOf(id));*/
        //?} else {
        return instance.getModifier(id);
        //?}
    }

    /** 摘掉修饰符 */
    public static void remove(AttributeInstance instance, ResourceLocation id) {
        //? if <1.20.5 {
        /*instance.removeModifier(uuidOf(id));*/
        //?} else {
        instance.removeModifier(id);
        //?}
    }

    /** 修饰符的数值（1.20.1 是 getAmount()，1.21 起是 amount()） */
    public static double amount(AttributeModifier modifier) {
        //? if <1.20.5 {
        /*return modifier.getAmount();*/
        //?} else {
        return modifier.amount();
        //?}
    }
}
