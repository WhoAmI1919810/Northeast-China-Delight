package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 脸盆架：四条腿、托盆的方框，后两根立柱上搭毛巾、顶上一面小镜子（镜子朝着放的人）。
 *
 * <p>拿着大脸盆 / 水盆右键空架子，把盆放上去；空手右键把盆拿下来（背包满了就掉在脚下）。
 * 架子上的盆和地上的一样能倒水、舀水（照 {@link BasinBlock}）：空盆拿水桶 / 水盆右键倒满，水盆拿空桶 / 空盆右键舀走。
 * 拆架子时盆一起掉出来（掉落表按 basin 属性给）。</p>
 */
public class BasinStandBlock extends FacingDecorBlock {

    public enum Basin implements StringRepresentable {
        NONE("none"), BASIN("basin"), WATER("water");

        private final String name;

        Basin(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        ItemStack toItem() {
            return new ItemStack(this == WATER ? ModItems.WATER_BASIN.get() : ModItems.LARGE_BASIN.get());
        }
    }

    public static final EnumProperty<Basin> BASIN = EnumProperty.create("basin", Basin.class);

    /** 架子本身（立柱、镜子高出一格的部分不算进选框）；放了盆再加上盆（盆底在 9.5，盆沿 5.5 高，见 DishBlock.BASIN_SHAPE） */
    private static final VoxelShape STAND_SHAPE = Block.box(2.5, 0, 2.5, 13.5, 16, 13.5);
    private static final VoxelShape WITH_BASIN_SHAPE = Shapes.or(STAND_SHAPE, Block.box(1, 9.5, 1, 15, 15, 15));

    public BasinStandBlock(BlockBehaviour.Properties properties) {
        super(properties, STAND_SHAPE, true);
        registerDefaultState(defaultBlockState().setValue(BASIN, Basin.NONE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BASIN) == Basin.NONE ? STAND_SHAPE : WITH_BASIN_SHAPE;
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            // 和 1.21.1 一样只认主手空着：主手拿着东西没用上时还会再拿副手问一遍，那时不该把盆拿下来
            return hand == InteractionHand.MAIN_HAND ? takeBasin(state, level, pos, player) : InteractionResult.PASS;
        }
        return useBasin(held, state, level, pos, player, hand);
    }
    *///?} else {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = useBasin(stack, state, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 空手右键：把盆拿下来（只认主手空着，手里拿着东西时让给物品自己的用法） */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return player.getMainHandItem().isEmpty() ? takeBasin(state, level, pos, player) : InteractionResult.PASS;
    }
    //?}

    private static InteractionResult takeBasin(BlockState state, Level level, BlockPos pos, Player player) {
        Basin basin = state.getValue(BASIN);
        if (basin == Basin.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BASIN, Basin.NONE), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            ItemStack item = basin.toItem();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult useBasin(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand) {
        Basin basin = state.getValue(BASIN);
        if (basin == Basin.NONE) {
            Basin put = held.is(ModItems.LARGE_BASIN.get()) ? Basin.BASIN
                    : held.is(ModItems.WATER_BASIN.get()) ? Basin.WATER : null;
            if (put == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(BASIN, put), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // 桶和盆都行：倒进去剩空桶 / 空盆，舀出来得水桶 / 水盆（同 BasinBlock）
        boolean pour = basin == Basin.BASIN && (held.is(Items.WATER_BUCKET) || held.is(ModItems.WATER_BASIN.get()));
        boolean scoop = basin == Basin.WATER && (held.is(Items.BUCKET) || held.is(ModItems.LARGE_BASIN.get()));
        if (!pour && !scoop) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean withBasin = held.is(ModItems.WATER_BASIN.get()) || held.is(ModItems.LARGE_BASIN.get());
            ItemStack result = new ItemStack(withBasin
                    ? (pour ? ModItems.LARGE_BASIN.get() : ModItems.WATER_BASIN.get())
                    : (pour ? Items.BUCKET : Items.WATER_BUCKET));
            player.awardStat(Stats.ITEM_USED.get(held.getItem()));
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
            level.setBlock(pos, state.setValue(BASIN, pour ? Basin.WATER : Basin.BASIN), Block.UPDATE_ALL);
            level.playSound(null, pos, pour ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, pour ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BASIN);
    }
}
