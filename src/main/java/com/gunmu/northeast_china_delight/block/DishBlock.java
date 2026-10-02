package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 摆在地上的菜肴。
 *
 * <p>形状和 display-delight 的容器对齐：碗 8×8、高 4 像素；大盘 14×14、高 2 像素。
 * 不挡路、也没有完整方块大小的碰撞箱，走过去不会撞到空气。</p>
 */
public class DishBlock extends Block
{
    /** 碗装菜：占地 4~12、高 0~4（与 display-delight 的碗一致） */
    public static final VoxelShape BOWL_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0);
    /** 大盘菜：占地 1~15、高 0~2（与 display-delight 的托盘一致） */
    public static final VoxelShape TRAY_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

    private final VoxelShape shape;

    public DishBlock(BlockBehaviour.Properties properties, VoxelShape shape)
    {
        super(properties);
        this.shape = shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return this.shape;
    }
}
