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
