package com.gunmu.northeast_china_delight.block;

import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 障子（秫秸障子 / 板障子）：东北院子的篱笆。连法、碰撞箱都和铁栏杆 / 玻璃板一样，
 * 四个方向自己接上相邻的障子、墙和实心方块。
 *
 * <p>只是为了换模型才继承：1.20.1 的 {@link IronBarsBlock} 构造器不公开。不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class FenceScreenBlock extends IronBarsBlock {

    public FenceScreenBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
