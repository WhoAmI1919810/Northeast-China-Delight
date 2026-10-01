package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.TriState;

/**
 * 榛蘑簇：行为照抄农夫乐事的蘑菇菌簇（{@code MushroomColonyBlock}）。
 *
 * <ul>
 *   <li>四个生长阶段（0~3），随机刻会一阶一阶往上长，耕地／泥土／苔藓这类「能长菌的方块」上才行；</li>
 *   <li>拿<b>剪刀</b>右键：摘下一株蘑菇，菌簇退回一阶；</li>
 *   <li>拿<b>刀</b>右键：一次把菌簇上的蘑菇全割下来（数量＝当前阶段），菌簇退回 0 阶；</li>
 *   <li>撒骨粉：一次长 1~2 阶；玩家自己放下去的时候直接是长满的 3 阶（和农夫乐事一样）。</li>
 * </ul>
 */
public class HazelMushroomColonyBlock extends BushBlock implements BonemealableBlock
{
    public static final MapCodec<HazelMushroomColonyBlock> CODEC = simpleCodec(HazelMushroomColonyBlock::new);

    /** 原版／农夫乐事共用的规矩：亮度 13 以上就不算「菌类环境」 */
    public static final int MAX_LIGHT = 13;
    public static final int MAX_AGE = 3;
    public static final IntegerProperty COLONY_AGE = BlockStateProperties.AGE_3;

    protected static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0)
    };

    public HazelMushroomColonyBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLONY_AGE, 0));
    }

    @Override
    public MapCodec<HazelMushroomColonyBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE_BY_AGE[state.getValue(COLONY_AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    {
        return state.isSolidRender(level, pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        TriState soil = belowState.canSustainPlant(level, below, Direction.UP, state);
        if (belowState.is(BlockTags.MUSHROOM_GROW_BLOCK))
        {
            return true;
        }
        // 和榛蘑一样，只挑地面不挑亮度：亮处的菌簇不会因为方块更新而被清掉
        return soil.isDefault() ? this.mayPlaceOn(belowState, level, below) : soil.isTrue();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        int age = state.getValue(COLONY_AGE);
        if (age >= MAX_AGE)
        {
            return;
        }
        BlockState ground = level.getBlockState(pos.below());
        if (ground.is(ModBlockTags.HAZEL_MUSHROOM_COLONY_GROWABLE_ON)
                && CommonHooks.canCropGrow(level, pos, state, random.nextInt(4) == 0))
        {
            level.setBlock(pos, state.setValue(COLONY_AGE, age + 1), 2);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
    {
        return new ItemStack(ModItems.HAZEL_MUSHROOM.get());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit)
    {
        int age = state.getValue(COLONY_AGE);
        if (age <= 0)
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.canPerformAction(ItemAbilities.SHEARS_HARVEST))
        {
            level.setBlock(pos, state.setValue(COLONY_AGE, age - 1), 2);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            popResource(level, pos, new ItemStack(ModItems.HAZEL_MUSHROOM.get()));
            if (level instanceof ServerLevel serverLevel)
            {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.001);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(ModBlockTags.KNIVES))
        {
            ItemStack harvest = new ItemStack(ModItems.HAZEL_MUSHROOM.get(), age);
            level.setBlock(pos, state.setValue(COLONY_AGE, 0), 2);
            level.playSound(null, pos, this.soundType.getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            popResource(level, pos, harvest);
            if (level instanceof ServerLevel serverLevel)
            {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(COLONY_AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    {
        return state.getValue(COLONY_AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state)
    {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state)
    {
        int grown = Math.min(MAX_AGE, state.getValue(COLONY_AGE) + Mth.nextInt(level.random, 1, 2));
        level.setBlock(pos, state.setValue(COLONY_AGE, grown), 2);
    }
}
