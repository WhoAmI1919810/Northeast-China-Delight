package com.gunmu.northeast_china_delight.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
//? if <1.20.5 {
/*import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
*///?} else {
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
//?}

/**
 * 大脸盆（空盆）。对着水右键舀一盆水，得到水盆；潜行右键方块放在地上（潜行空手右键收回）；
 * 也能当帽子戴：右键空气扣头上，走原版头盔槽。
 *
 * <p>槽位判定两个版本同一套：本类实现 {@link Equipable} 后，1.20.1 的
 * {@code LivingEntity.getEquipmentSlotForItem}（Forge 补丁会读 Equipable）和 1.21.1 的
 * {@code IItemExtension.canEquip} 都会给出 HEAD，所以物品栏头盔格子能直接放进去。</p>
 *
 * <p>外观分流见 {@code client/LargeBasinItemRenderer}：物品栏是 2D 贴图，手 / 地面 / 头上是方块模型。</p>
 */
public class LargeBasinItem extends ItemNameBlockItem implements Equipable
{
    public LargeBasinItem(Block block, Item.Properties properties)
    {
        super(block, properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot()
    {
        return EquipmentSlot.HEAD;
    }

    /**
     * 右键：对着水舀一盆水，得到水盆（见 {@link #scoopWater}）；对着别的方块什么也不做（放盆要潜行）；
     * 对着空气扣头上 —— 原版 {@code Item#use} 自己不认 Equipable（只有盔甲、鞘翅在里面调了 swap），所以这里补上。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        InteractionResultHolder<ItemStack> scooped = scoopWater(level, player, hand);
        if (scooped != null)
        {
            return scooped;
        }
        if (getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() == HitResult.Type.BLOCK)
        {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    /**
     * 和水盆一样：潜行右键方块才放盆（地上的盆潜行空手右键收回，见 {@code BasinBlock}）；
     * 不潜行返回 PASS，客户端接着调 {@link #use} 去舀水。
     */
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player == null || player.isSecondaryUseActive())
        {
            return super.useOn(context);
        }
        return InteractionResult.PASS;
    }

    /**
     * 照原版空桶取水：沿视线找最近的水源方块，舀走它（无限水源会自己补回来），一个空盆换一个水盆。
     * 视线上先碰到的不是水源就返回 null，交给原来的逻辑（扣头上 / 什么也不做）。
     */
    @Nullable
    private InteractionResultHolder<ItemStack> scoopWater(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK)
        {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().is(FluidTags.WATER) || !state.getFluidState().isSource()
                || !(state.getBlock() instanceof BucketPickup pickup))
        {
            return null;
        }
        Direction side = hit.getDirection();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(side), side, stack))
        {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide)
        {
            //? if <1.20.2 {
            /*ItemStack picked = pickup.pickupBlock(level, pos, state);
            *///?} else {
            ItemStack picked = pickup.pickupBlock(player, level, pos, state);
            //?}
            if (picked.isEmpty())
            {
                return InteractionResultHolder.fail(stack);
            }
            pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        ItemStack filled = ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.WATER_BASIN.get()));
        return InteractionResultHolder.sidedSuccess(filled, level.isClientSide());
    }

    /**
     * 戴在头盔槽时给 1 点护甲值。
     *
     * <p>刻意不继承 {@code ArmorItem}：那会把耐久条、附魔表、盔甲贴图一起带进来，而大脸盆的定位是
     * 「方块物品 + 能扣头上」。这里只补一个属性修改器，所以没耐久、也不进附魔表
     * （{@code Item#isEnchantable} 默认要求「不可堆叠 + 有耐久组件」，本物品两条都不满足）。</p>
     */
    //? if <1.20.5 {
    /*private static final java.util.UUID ARMOR_MODIFIER_ID = java.util.UUID.fromString("6f2f5d1e-6e2a-4a3d-9f0e-1b7c4d5a8e11");

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot)
    {
        if (slot != EquipmentSlot.HEAD)
        {
            return super.getDefaultAttributeModifiers(slot);
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(super.getDefaultAttributeModifiers(slot))
                .put(Attributes.ARMOR,
                        new AttributeModifier(ARMOR_MODIFIER_ID, "large_basin_armor", 1.0D, AttributeModifier.Operation.ADDITION))
                .build();
    }
    *///?} else {
    private static final ItemAttributeModifiers HEAD_ARMOR_MODIFIERS = ItemAttributeModifiers.builder()
            .add(Attributes.ARMOR,
                    new AttributeModifier(DdIds.of(NortheastChinaDelight.MODID, "large_basin_armor"), 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD)
            .build();

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers()
    {
        return HEAD_ARMOR_MODIFIERS;
    }
    //?}

    /*
     * 1.20.1 没有 RegisterClientExtensionsEvent，客户端渲染器只能从物品这里挂；
     * 1.21.1 那份在 NortheastChinaDelight.ClientModEvents#onRegisterClientExtensions。
     */
    //? if <1.20.2 {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(com.gunmu.northeast_china_delight.client.LargeBasinItemRenderer.CLIENT_EXTENSION);
    }
    *///?}
}
