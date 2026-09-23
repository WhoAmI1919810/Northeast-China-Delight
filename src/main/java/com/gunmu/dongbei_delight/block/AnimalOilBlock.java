package com.gunmu.dongbei_delight.block;

import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 动物油块：行为完全照搬原版蜂蜜块（能粘住实体、贴着墙下滑、踩上去黏脚），
 * 所以直接继承 {@link HoneyBlock}，只换一套更白的贴图。
 *
 * 注意这里**不重写 codec()** —— 原版蜂蜜块的 {@code codec()} 返回的是写死的
 * {@code MapCodec<HoneyBlock>}，子类没法换成自己的类型；本模组的烤架营火
 * （继承原版营火）也是同样的处理方式。
 */
public class AnimalOilBlock extends HoneyBlock {

    public AnimalOilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
