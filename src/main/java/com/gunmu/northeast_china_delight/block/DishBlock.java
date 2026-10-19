package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DishBlock extends Block
{
    public static final VoxelShape BOWL_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0);
    public static final VoxelShape TRAY_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);
    /** 大脸盆盆体（放地上的空盆）：与 block/large_basin 的实体模型对齐，盆沿 5.5 像素。 */
    public static final VoxelShape BASIN_SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 5.5, 15.0);
    /** 黄桃罐头瓶身：模型外轮廓 3..13 × 0..16 × 3..13。 */
    public static final VoxelShape CANNED_YELLOW_PEACH_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    private final VoxelShape shape;

    public DishBlock(BlockBehaviour.Properties properties, VoxelShape shape)
    {
        super(properties);
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return this.shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return this.shape;
    }
}
