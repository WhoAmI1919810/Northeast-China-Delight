package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.20.5 {
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
//?} else {
/*import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
*///?}

/**
 * 木槌：木臼「舂捣」用的工具（配方里的 {@code tool}），本身也是一把钝器。
 *
 * <ul>
 *   <li>耐久 {@link #DURABILITY}，可附魔（耐久 / 经验修补 / 消失诅咒），不能堆叠；</li>
 *   <li>攻击伤害 5 点、攻击速度 1.0 —— 工具提示显示的是「玩家基础值 + 修饰符」的合计，
 *       所以修饰符必须挂原版的 {@code base_attack_damage} / {@code base_attack_speed} id，
 *       自己另起 id 的话，提示只会显示 +4 / -3.0 这种增量；</li>
 *   <li>外观分流见 {@code client/WoodenMalletItemRenderer}：物品栏画 2D 贴图，手 / 地面 / 展示框画 3D 模型。</li>
 * </ul>
 */
public class WoodenMalletItem extends Item
{
    /** 耐久：敲一下掉 1 点，正好够捣 118 次 */
    public static final int DURABILITY = 118;
    /** 攻击伤害 5 点 = 玩家基础 1 + 4（跟剑一样，提示里显示合计值） */
    public static final double ATTACK_DAMAGE_BONUS = 4.0D;
    /** 攻击速度 1.0 = 玩家基础 4.0 - 3.0 */
    public static final double ATTACK_SPEED_BONUS = -3.0D;

    public WoodenMalletItem(Item.Properties properties)
    {
        super(properties);
    }

    /**
     * 1.21 把武器属性写进物品属性（{@code ATTRIBUTE_MODIFIERS} 数据组件），
     * 1.20.1 没有数据组件，只能覆写 {@code getDefaultAttributeModifiers}。
     * 两边判定「基础修饰符」的依据不同：1.20.1 认 UUID（{@code Item.BASE_ATTACK_DAMAGE_UUID} /
     * {@code BASE_ATTACK_SPEED_UUID}），1.21 认 id，都不能自造。
     */
    //? if <1.20.5 {
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot)
    {
        if (slot != EquipmentSlot.MAINHAND)
        {
            return super.getDefaultAttributeModifiers(slot);
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(super.getDefaultAttributeModifiers(slot))
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "base_attack_damage",
                        ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "base_attack_speed",
                        ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADDITION))
                .build();
    }
    //?} else {
    /*public static ItemAttributeModifiers createAttributes()
    {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        ATTACK_DAMAGE_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }
    *///?}

    /**
     * 可附魔的耐久工具。1.21 里「耐久 / 经验修补 / 消失诅咒」各自的可附对象写在
     * {@code minecraft:enchantable/*} 这些物品标签里（见 {@code data/minecraft/tags/item/enchantable}），
     * 1.20.1 按「可损坏物品」自动判定；两边都要本方法放行。
     */
    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return stack.getMaxStackSize() == 1 && stack.isDamageableItem();
    }

    /*
     * 1.20.1 没有 RegisterClientExtensionsEvent，客户端渲染器只能从物品这里挂；
     * 1.21.1 那份在 NortheastChinaDelight.ClientModEvents#onRegisterClientExtensions。
     */
    //? if <1.20.2 {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(com.gunmu.northeast_china_delight.client.WoodenMalletItemRenderer.CLIENT_EXTENSION);
    }
    //?}
}
