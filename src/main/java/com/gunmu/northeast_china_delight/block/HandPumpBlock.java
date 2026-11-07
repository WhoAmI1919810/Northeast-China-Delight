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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 压水井：拿空桶、空的大脸盆、玻璃瓶右键，压出一桶水 / 一盆水 / 一瓶水。水不限量（地下水）。
 *
 * <p>手里拿的不是盛东西的家伙（空手、木棍、方块……）就光压一下：出水嘴喷一股水花（只有粒子和声音，不放水方块）。
 * 和别的方块的空手右键一样只认主手。</p>
 *
 * <p>两格高（2026-11-07 用户：「压水井得变成有上下两部分的模型，大概得有一格半那么高，这样才能让给前方容器装水更自然。
 * 出水口也要延伸到前半格去」）：同门帘分上下两半（{@link #HALF}），点上半格同点下半格。
 * 模型是建模师做的（同日用户：「建模师做了一个新的水井模型，把它用上」，work/水井.rar，转格式 work/tools/hand_pump/modeler.py）：
 * 一共 25.65 像素高；出水槽从井身斜着往前下方伸，槽口在前面那一格边上 1.5 像素、离地 11.5 像素左右。</p>
 *
 * <p>光压时往出水嘴前下方那一格倒水（2026-11-06 用户，{@link #pourInto}；11-07 换了建模师的模型后用户定：
 * 「不改模型，只往前下方一格倒」——槽口低，前面同一层放容器会插进锅壁）。炼药锅水位 +1，大缸水位 +1（一层 1000 mB），地上或脸盆架上的空大脸盆变水盆，
 * 大灶上揭着盖的大铁锅倒一桶水，空木臼放一份水；各自的规矩同拿水桶倒（满了、压着石头、煮着、盖着锅盖就不收）。
 * 倒进去了，嘴口落一道水下去；没倒进去照旧光喷水花。</p>
 */
public class HandPumpBlock extends FacingDecorBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** 井身：下半格整格高，上半格到 10 像素（井帽、压把轴架）；出水槽、压把不算进选框 */
    public static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    public static final VoxelShape UPPER_SHAPE = Block.box(4, 0, 4, 12, 10, 12);
    /** 槽口（朝北放时在整体坐标约 8, 11.5, -1.6）：离井身中心往前 0.6 格、离地 0.7 格 */
    private static final double MOUTH_FORWARD = 0.6;
    private static final double MOUTH_Y = 0.7;

    public HandPumpBlock(BlockBehaviour.Properties properties) {
        super(properties, SHAPE, true);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPE : SHAPE;
    }

    /** 上面那格放得下才能放（同门帘） */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** 另一半没了这一半也没（同门帘）；掉落表只在下半给一个压水井 */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != half
                    ? state.setValue(FACING, neighborState.getValue(FACING))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    //? if <1.20.2 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeLowerHalfInCreative(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        removeLowerHalfInCreative(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    /** 同门帘：创造模式敲上半时，下半直接换成空气（35 = 通知客户端 + 不掉落），免得下半掉出一个压水井 */
    private static void removeLowerHalfInCreative(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide || !player.isCreative() || state.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(state.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, below, Block.getId(belowState));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    /** 井身那格：点上半格时是下面那格 */
    private static BlockPos base(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = pump(player.getItemInHand(hand), level, base(state, pos), player, hand);
        return result == InteractionResult.PASS && hand == InteractionHand.MAIN_HAND
                ? splash(state, level, pos, player)
                : result;
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = pump(stack, level, base(state, pos), player, hand);
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
     * 光压不接水：嘴口往下喷一股水花；嘴底下有盛水的家伙就倒进去（{@link #pourInto}）。
     * 哪只手里有盛东西的家伙就让开 —— 主手的桶自己去倒水，副手拿着空桶的轮到副手时再压水。
     */
    private static InteractionResult splash(BlockState state, Level level, BlockPos clicked, Player player) {
        if (isContainer(player.getMainHandItem()) || isContainer(player.getOffhandItem())) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockPos pos = base(state, clicked);
            Direction facing = state.getValue(FACING);
            BlockPos target = pourInto(server, pos, facing, player);
            if (target != null) {
                stream(server, pos, facing, target);
                return InteractionResult.SUCCESS;
            }
            double x = pos.getX() + 0.5 + facing.getStepX() * MOUTH_FORWARD;
            double y = pos.getY() + MOUTH_Y;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * MOUTH_FORWARD;
            server.sendParticles(ParticleTypes.SPLASH, x, y - 0.05, z, 18, 0.05, 0.03, 0.05, 0.12);
            server.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 6, 0.03, 0.0, 0.03, 0.0);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 往出水嘴前下方那一格倒水。倒进去了返回接水的那格，没倒进去返回 null */
    @Nullable
    private static BlockPos pourInto(ServerLevel level, BlockPos pos, Direction facing, Player player) {
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
        return poured ? target : null;
    }

    /**
     * 嘴口往下落一道水：从嘴口竖着一串落水滴一直到接水那格的上沿（落水滴自己会往下掉，落到东西上溅开），落点再溅一圈水花
     */
    private static void stream(ServerLevel level, BlockPos pos, Direction facing, BlockPos target) {
        double x = pos.getX() + 0.5 + facing.getStepX() * MOUTH_FORWARD;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * MOUTH_FORWARD;
        double top = pos.getY() + MOUTH_Y;
        double bottom = target.getY() + 0.9;
        int steps = Math.max(2, (int) Math.ceil((top - bottom) / 0.12));
        for (int i = 0; i <= steps; i++) {
            double y = top - (top - bottom) * i / steps;
            level.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 2, 0.01, 0.0, 0.01, 0.0);
        }
        level.sendParticles(ParticleTypes.SPLASH, x, bottom, z, 16, 0.12, 0.02, 0.12, 0.1);
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
