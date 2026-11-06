package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 压水井：拿空桶、空的大脸盆、玻璃瓶右键，压出一桶水 / 一盆水 / 一瓶水。水不限量（地下水）。
 *
 * <p>手里拿的不是盛东西的家伙（空手、木棍、方块……）就光压一下：出水嘴喷一股水花（只有粒子和声音，不放水方块）。
 * 和别的方块的空手右键一样只认主手。</p>
 *
 * <p>光压时出水嘴前下方那一格（出水嘴朝的那一格再往下一格）是盛水的家伙，就往里倒水（2026-11-06 用户，{@link #pourInto}）：
 * 炼药锅水位 +1，大缸水位 +1（一层 1000 mB），地上或脸盆架上的空大脸盆变水盆，大灶上揭着盖的大铁锅倒一桶水，
 * 空木臼放一份水；各自的规矩同拿水桶倒（满了、压着石头、煮着、盖着锅盖就不收）。倒进去了，出水嘴流一道水下去；
 * 没倒进去照旧光喷水花。</p>
 */
public class HandPumpBlock extends FacingDecorBlock {

    public static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(5, 0, 5, 11, 15, 11);

    public HandPumpBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE, true);
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = pump(player.getItemInHand(hand), level, pos, player, hand);
        return result == InteractionResult.PASS && hand == InteractionHand.MAIN_HAND
                ? splash(state, level, pos, player)
                : result;
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = pump(stack, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return splash(state, level, pos, player);
    }
    *///?}

    private static InteractionResult pump(ItemStack held, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack filled;
        if (held.is(Items.BUCKET)) {
            filled = new ItemStack(Items.WATER_BUCKET);
        } else if (held.is(ModItems.LARGE_BASIN.get())) {
            filled = new ItemStack(ModItems.WATER_BASIN.get());
        } else if (held.is(Items.GLASS_BOTTLE)) {
            filled = waterBottle();
        } else {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Item used = held.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
            player.awardStat(Stats.ITEM_USED.get(used));
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 光压不接水：出水嘴（朝北放时在 8, 7, 2）往下喷一股水花。
     * 哪只手里有盛东西的家伙就让开 —— 主手的桶自己去倒水，副手拿着空桶的轮到副手时再压水。
     */
    private static InteractionResult splash(BlockState state, Level level, BlockPos pos, Player player) {
        if (isContainer(player.getMainHandItem()) || isContainer(player.getOffhandItem())) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            Direction facing = state.getValue(FACING);
            if (pourInto(server, pos, facing, player)) {
                stream(server, pos, facing);
                return InteractionResult.SUCCESS;
            }
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.375;
            double y = pos.getY() + 0.42;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.375;
            server.sendParticles(ParticleTypes.SPLASH, x, y - 0.05, z, 18, 0.05, 0.03, 0.05, 0.12);
            server.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 6, 0.03, 0.0, 0.03, 0.0);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 往出水嘴前下方那一格倒水；倒进去了返回 true */
    private static boolean pourInto(ServerLevel level, BlockPos pos, Direction facing, Player player) {
        BlockPos target = pos.relative(facing).below();
        BlockState state = level.getBlockState(target);
        boolean poured = false;
        // 大锅自己倒水时就有倒水声、水花和事件（BigPotCooking#pour），这里不再出一遍
        boolean quiet = false;
        if (state.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(target, Blocks.WATER_CAULDRON.defaultBlockState());
            poured = true;
        } else if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < 3) {
            level.setBlockAndUpdate(target, state.cycle(LayeredCauldronBlock.LEVEL));
            poured = true;
        } else if (state.is(ModBlocks.LARGE_BASIN.get())) {
            level.setBlock(target, PlacedDishBlock.withFacing(ModBlocks.WATER_BASIN.get().defaultBlockState(), state),
                    Block.UPDATE_ALL);
            poured = true;
        } else if (state.getBlock() instanceof BasinStandBlock
                && state.getValue(BasinStandBlock.BASIN) == BasinStandBlock.Basin.BASIN) {
            level.setBlock(target, state.setValue(BasinStandBlock.BASIN, BasinStandBlock.Basin.WATER),
                    Block.UPDATE_ALL);
            poured = true;
        } else if (state.getBlock() instanceof BigStoveBlock) {
            // 同漏斗往锅里送一桶水：要放着锅、揭着盖、没在煮、没煮好、锅里没满
            BigPotBlockEntity pot = BigStoveBlock.potAt(level, target, state);
            poured = pot != null && BigPotCooking.dropIn(pot, state, level, target,
                    new ItemStack(Items.WATER_BUCKET), player, false) != null;
            quiet = true;
        } else if (state.getBlock() instanceof Vat vat) {
            poured = vat.pumpWater(state, level, target);
        } else if (state.getBlock() instanceof WoodenMortarBlock) {
            poured = WoodenMortarBlock.pumpWater(state, level, target);
        }
        if (poured && !quiet) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PLACE, target);
        }
        return poured;
    }

    /**
     * 出水嘴往前下方流一道水：从嘴口（离中心 0.375、高 0.42）抛出去，落到前面那一格的顶上（离中心 0.75、高 0），
     * 沿抛物线一串落水滴（落水滴自己会往下掉，落到东西上溅开），落点再溅一圈水花
     */
    private static void stream(ServerLevel level, BlockPos pos, Direction facing) {
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double forward = 0.375 + 0.375 * t;
            double up = 0.42 - 0.47 * t * t;
            level.sendParticles(ParticleTypes.FALLING_WATER,
                    pos.getX() + 0.5 + facing.getStepX() * forward, pos.getY() + up,
                    pos.getZ() + 0.5 + facing.getStepZ() * forward, 2, 0.01, 0.0, 0.01, 0.0);
        }
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5 + facing.getStepX() * 0.75, pos.getY() - 0.05,
                pos.getZ() + 0.5 + facing.getStepZ() * 0.75, 16, 0.08, 0.02, 0.08, 0.1);
    }

    /** 盛东西的家伙：各种桶、玻璃瓶 / 药水瓶、大脸盆 / 水盆、调料瓶 */
    private static boolean isContainer(ItemStack stack) {
        return stack.getItem() instanceof BucketItem || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.GLASS_BOTTLE) || stack.is(Items.POTION)
                || stack.is(ModItems.LARGE_BASIN.get()) || stack.is(ModItems.WATER_BASIN.get())
                || SeasoningBottleItem.isBottle(stack);
    }

    private static ItemStack waterBottle() {
        //? if <1.20.5 {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),
                net.minecraft.world.item.alchemy.Potions.WATER);
        //?} else {
        /*return net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                net.minecraft.world.item.alchemy.Potions.WATER);
        *///?}
    }
}
