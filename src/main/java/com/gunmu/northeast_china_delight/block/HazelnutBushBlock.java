package com.gunmu.northeast_china_delight.block;

import com.mojang.serialization.MapCodec;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Blocks;

/**
 * 榛子丛：行为照搬原版甜浆果丛，只有三处不同。生长阶段沿用甜浆果丛的 {@link BlockStateProperties#AGE_3}（0~3）： 0~1 是幼苗，2 开始结
 * 果（右键可摘），3 是完熟（右键能摘到最多）。破坏时的掉落写在
 * {@code data/northeast_china_delight/loot_table/blocks/hazelnut_bush.json} 里。
 */
public class HazelnutBushBlock extends BushBlock implements BonemealableBlock {

    //? if >=1.20.2 {
    /*public static final MapCodec<HazelnutBushBlock> CODEC = simpleCodec(HazelnutBushBlock::new);
    *///?}

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

    private static final VoxelShape SAPLING_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape MID_GROWTH_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public HazelnutBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(SNOWY, false));
    }

    private static boolean isSnow(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    /**
     * 这一株该不该是覆雪形态：
     * 自己这一格是雪片（也就是刚把它顶掉）或脚下实际有雪。
     * 不能按生物群系气候推断，否则无雪地面种植也会显示覆雪。
     */
    public static boolean isSnowyAt(LevelReader level, BlockPos pos) {
        return isSnow(level.getBlockState(pos))
                || isSnow(level.getBlockState(pos.below()));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(SNOWY, isSnowyAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        if (super.mayPlaceOn(state, level, pos)) {
            return true;
        }
        return (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK))
                && super.mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    //? if >=1.20.2 {
    /*@Override
    public MapCodec<HazelnutBushBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    //? if <1.20.2 {
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state)
    //?} else {
    /*public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
    *///?}
    {
        return new ItemStack(ModItems.HAZELNUT.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(AGE) == 0) {
            return SAPLING_SHAPE;
        }
        return state.getValue(AGE) < MAX_AGE ? MID_GROWTH_SHAPE : super.getShape(state, level, pos, context);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE
                && level.getRawBrightness(pos.above(), 0) >= 9
                && cropGrowPre(level, pos, state, random.nextInt(5) == 0)) {
            BlockState grown = state.setValue(AGE, age + 1);
            level.setBlock(pos, grown, 2);
            cropGrowPost(level, pos, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(grown));
        }
    }

    private static boolean cropGrowPre(ServerLevel level, BlockPos pos, BlockState state, boolean def) {
        //? if <1.20.2 {
        return net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, def);
        //?} else if <1.20.5 {
        /*return net.neoforged.neoforge.common.CommonHooks.onCropsGrowPre(level, pos, state, def);*/
        //?} else {
        /*return net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, def);
        *///?}
    }

    private static void cropGrowPost(ServerLevel level, BlockPos pos, BlockState state) {
        //? if <1.20.2 {
        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        //?} else if <1.20.5 {
        /*net.neoforged.neoforge.common.CommonHooks.onCropsGrowPost(level, pos, state);*/
        //?} else {
        /*net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
        *///?}
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity && entity.getType() != EntityType.FOX && entity.getType() != EntityType.BEE) {
            entity.makeStuckInBlock(state, new Vec3(0.8F, 0.75, 0.8F));
        }
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (state.getValue(AGE) != MAX_AGE && stack.is(Items.BONE_MEAL)) {
            return InteractionResult.PASS;
        }
        int age = state.getValue(AGE);
        boolean ripe = age == MAX_AGE;
        if (age > 1) {
            int count = 1 + level.random.nextInt(2) + (ripe ? 1 : 0);
            popResource(level, pos, new ItemStack(ModItems.HAZELNUT.get(), count));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            BlockState picked = state.setValue(AGE, 1);
            level.setBlock(pos, picked, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean ripe = state.getValue(AGE) == MAX_AGE;
        return !ripe && stack.is(Items.BONE_MEAL)
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        int age = state.getValue(AGE);
        boolean ripe = age == MAX_AGE;
        if (age > 1) {
            int count = 1 + level.random.nextInt(2) + (ripe ? 1 : 0);
            popResource(level, pos, new ItemStack(ModItems.HAZELNUT.get(), count));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            BlockState picked = state.setValue(AGE, 1);
            level.setBlock(pos, picked, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
    *///?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SNOWY);
    }

    @Override
    //? if <1.20.2 {
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient)
    //?} else {
    /*public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    *///?}
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
