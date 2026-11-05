package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionHand;
*///?}

import java.util.List;

/**
 * 放下来的碗装菜、盘装菜、空大脸盆、水盆（2026-11-05 用户：盆装菜、碗装菜、盘装菜、空盆、水盆的放置逻辑都统一）。
 * 盆装菜（{@link DdFeastBlock}）不是这个类，但规矩一样：
 *
 * <ul>
 *   <li>碗装菜、盘装菜潜行右键方块放下（不潜行是吃）；空盆、水盆、盆装菜直接右键放下（2026-11-05 同日用户改的）。放在哪都行，底下的方块拆了也不掉；</li>
 *   <li>朝向放的人（{@link #FACING}，同盆装菜：模型的北面对着人）；</li>
 *   <li>潜行空手右键收回背包（{@link #pickUp}），给的东西和拆掉时掉的一样，背包满了掉在脚下。</li>
 * </ul>
 *
 * <p>只给这几类用：小板凳、红灯笼、黄桃罐头也借了 {@link DishBlock} 的形状，但它们不归这套规矩
 * （小板凳有自己的朝向），所以单独开这个子类，不改 DishBlock。</p>
 */
public class PlacedDishBlock extends DishBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PlacedDishBlock(BlockBehaviour.Properties properties, VoxelShape shape)
    {
        super(properties, shape);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** 结构旋转、镜像时跟着转（原版 1.21.1 这两个是 protected，写成 public 两个版本都能用） */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        return hand == InteractionHand.MAIN_HAND ? pickUp(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit)
    {
        return pickUp(state, level, pos, player);
    }
    //?}

    /**
     * 潜行空手右键：方块收回背包。给的就是拆掉时战利品表掉的那些（盆装谜之炖菜连效果一起，见 MysteryStewPotBlock#getDrops）；
     * 不潜行或者手里有东西返回 PASS
     */
    public static InteractionResult pickUp(BlockState state, Level level, BlockPos pos, Player player)
    {
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty())
        {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server)
        {
            List<ItemStack> drops = Block.getDrops(state, server, pos, level.getBlockEntity(pos), player, ItemStack.EMPTY);
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            for (ItemStack drop : drops)
            {
                if (!player.getInventory().add(drop))
                {
                    player.drop(drop, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 换成同类另一种方块时（空盆和水盆互换、盆装菜盛完变空盆）把朝向带过去 */
    public static BlockState withFacing(BlockState next, BlockState from)
    {
        return next.hasProperty(FACING) && from.hasProperty(FACING) ? next.setValue(FACING, from.getValue(FACING)) : next;
    }
}
