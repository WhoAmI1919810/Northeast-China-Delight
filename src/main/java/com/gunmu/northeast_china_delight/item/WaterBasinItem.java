package com.gunmu.northeast_china_delight.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 水盆（装了一盆水的大脸盆）。
 *
 * <ul>
 *   <li>右键方块：把水盆放在地上（方块 {@code water_basin}，潜行空手右键能收回物品栏，见 {@code BasinBlock}）；</li>
 *   <li>潜行 + 右键：像水桶一样把水倒出去，手里剩空的大脸盆。</li>
 * </ul>
 * 2026-11-05 用户：空盆、水盆、盆装菜能直接右键放下，所以放和倒水对调了（以前右键倒水、潜行右键放）。
 * 对着炼药锅、大灶、地上的盆这些自己会接水的方块，不潜行右键照旧由那个方块处理（往里倒）。
 *
 * <p>外观分流和大脸盆同一个渲染器：物品栏是 2D 贴图，手里 / 地上 / 展示框是方块模型（见 {@code client/LargeBasinItemRenderer}）。</p>
 */
public class WaterBasinItem extends ItemNameBlockItem
{
    public WaterBasinItem(Block block, Item.Properties properties)
    {
        super(block, properties);
    }

    /** 不潜行右键方块就放下；潜行交给 {@link #use} 倒水（返回 PASS 后客户端会接着调 use） */
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive())
        {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    /** 潜行倒水：沿视线找方块，照原版水桶的 {@code emptyContents} 放一格水（下界里会蒸发，含水方块会被灌水） */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive())
        {
            return InteractionResultHolder.pass(stack);
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK)
        {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();
        BlockPos beside = pos.relative(side);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(beside, side, stack))
        {
            return InteractionResultHolder.fail(stack);
        }
        BlockState state = level.getBlockState(pos);
        BlockPos target = state.getBlock() instanceof LiquidBlockContainer ? pos : beside;
        if (!((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, target, hit))
        {
            return InteractionResultHolder.fail(stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        ItemStack result = player.getAbilities().instabuild
                ? stack
                : ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.LARGE_BASIN.get()));
        return InteractionResultHolder.sidedSuccess(result, level.isClientSide());
    }

    /*
     * 1.20.1 没有 RegisterClientExtensionsEvent，客户端渲染器只能从物品这里挂；
     * 1.21.1 那份在 NortheastChinaDelight.ClientModEvents#onRegisterClientExtensions。
     */
    //? if <1.20.2 {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(com.gunmu.northeast_china_delight.client.LargeBasinItemRenderer.WATER_BASIN_CLIENT_EXTENSION);
    }
    *///?}
}
