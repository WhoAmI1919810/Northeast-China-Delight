package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 贴墙放的摆件（炕围画、暖气片）：模型背面贴着 facing 对面那堵墙（facing=north 时正面朝北、背面在南边 z=16）。
 *
 * <p>点墙的侧面放，就贴在点的那面墙上；点地面放，背面朝人看的方向、正面朝着放的人（同压水井）。
 * 形状照 facing=north 的盒子跟着转，见 {@link FacingDecorBlock#rotatedShapes}。</p>
 */
public class WallDecorBlock extends FacingDecorBlock {

    public WallDecorBlock(BlockBehaviour.Properties properties,
                          double x0, double y0, double z0, double x1, double y1, double z1) {
        super(properties, rotatedShapes(x0, y0, z0, x1, y1, z1), true);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return face.getAxis().isHorizontal()
                ? defaultBlockState().setValue(FACING, face)
                : super.getStateForPlacement(context);
    }
}
