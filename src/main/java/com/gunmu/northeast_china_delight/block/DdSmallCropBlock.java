package com.gunmu.northeast_china_delight.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * **4 个生长阶段**的小作物（原版 {@code CropBlock} 写死了 8 个阶段 AGE_7，改不了，
 * 所以这里单独写一个 AGE_3 的版本）。
 *
 * <p>行为照抄原版作物：只能种在耕地上、随机刻按「生长速度」推进、骨粉催一阶。
 * 目前只有**大葱**用它 —— 大葱只画了 4 个阶段，而且生长本来就快。
 *
 * <p>注意：这个方块的 {@code age} 只有 0~3；老存档里如果是这个方块、又存着 age≥4，
 * 读档时那个属性会被忽略、退回 age 0（作物变回幼苗），不会影响别的方块。
 */
public class DdSmallCropBlock extends BushBlock implements BonemealableBlock {

    //? if >=1.20.2 {
    public static final MapCodec<DdSmallCropBlock> CODEC = simpleCodec(DdSmallCropBlock::new);
    //?}

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[] {
            Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    };

    /**
     * 种子物品；只有 codec 反序列化出来的实例才会是 null
     * （本模组永远用带种子的那个构造器注册方块）。
     */
    @org.jetbrains.annotations.Nullable
    private final Supplier<? extends ItemLike> seed;

    /** 给 codec 用的构造器（simpleCodec 只认「只有 Properties」的那种） */
    public DdSmallCropBlock(BlockBehaviour.Properties properties) {
        this(properties, null);
    }

    public DdSmallCropBlock(BlockBehaviour.Properties properties,
                            @org.jetbrains.annotations.Nullable Supplier<? extends ItemLike> seed) {
        super(properties);
        this.seed = seed;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    //? if >=1.20.2 {
    @Override
    public MapCodec<DdSmallCropBlock> codec() {
        return CODEC;
    }
    //?}

    /** 选取方块时给回种子 */
    @Override
    //? if <1.20.2 {
    /*public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state)
    *///?} else {
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
    //?}
    {
        return this.seed == null ? ItemStack.EMPTY : new ItemStack(this.seed.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // 和原版作物一样只能种在耕地上（农夫乐事的富饶农田继承自耕地，所以也能种）
        return state.getBlock() instanceof FarmBlock;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        int age = state.getValue(AGE);
        if (age >= MAX_AGE) {
            return;
        }
        float speed = growthSpeed(state, level, pos);
        if (cropGrowPre(level, pos, state, random.nextInt((int) (25.0F / speed) + 1) == 0)) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            cropGrowPost(level, pos, state);
        }
    }

    private static boolean cropGrowPre(ServerLevel level, BlockPos pos, BlockState state, boolean def) {
        //? if <1.20.2 {
        /*return net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, def);
        *///?} else if <1.20.5 {
        /*return net.neoforged.neoforge.common.CommonHooks.onCropsGrowPre(level, pos, state, def);*/
        //?} else {
        return net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, def);
        //?}
    }

    private static void cropGrowPost(ServerLevel level, BlockPos pos, BlockState state) {
        //? if <1.20.2 {
        /*net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        *///?} else if <1.20.5 {
        /*net.neoforged.neoforge.common.CommonHooks.onCropsGrowPost(level, pos, state);*/
        //?} else {
        net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
        //?}
    }

    /**
     * 生长速度：和原版作物同一套算法 —— 看脚下 3×3 的耕地（湿润的更快），
     * 周围同样种着这种作物的格子会拖慢生长。
     */
    private float growthSpeed(BlockState state, BlockGetter level, BlockPos pos) {
        float speed = 1.0F;
        BlockPos below = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                float soil = 0.0F;
                BlockState belowState = level.getBlockState(below.offset(dx, 0, dz));
                if (belowState.getBlock() instanceof FarmBlock) {
                    soil = 1.0F;
                    if (belowState.getValue(FarmBlock.MOISTURE) > 0) {
                        soil = 3.0F;
                    }
                }
                if (dx != 0 || dz != 0) {
                    soil /= 4.0F;
                }
                speed += soil;
            }
        }

        boolean west = level.getBlockState(pos.west()).is(this);
        boolean east = level.getBlockState(pos.east()).is(this);
        boolean north = level.getBlockState(pos.north()).is(this);
        boolean south = level.getBlockState(pos.south()).is(this);
        boolean alongX = west || east;
        boolean alongZ = north || south;
        if (alongX && alongZ) {
            speed /= 2.0F;
        } else {
            boolean diagonal = level.getBlockState(pos.west().north()).is(this)
                    || level.getBlockState(pos.east().north()).is(this)
                    || level.getBlockState(pos.west().south()).is(this)
                    || level.getBlockState(pos.east().south()).is(this);
            if (diagonal) {
                speed /= 2.0F;
            }
        }
        return speed;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    //? if <1.20.2 {
    /*public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient)
    *///?} else {
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    //?}
    {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), 2);
    }
}
