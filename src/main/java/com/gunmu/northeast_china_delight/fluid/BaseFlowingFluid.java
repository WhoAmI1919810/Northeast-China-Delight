package com.gunmu.northeast_china_delight.fluid;

//? if <1.20.2 {
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.function.Supplier;

// 1.20.1（Forge）上给 NeoForge 的 BaseFlowingFluid 起一个同名外壳。
// Forge 里对应的类叫 ForgeFlowingFluid，API 形状几乎一样，只是
// Properties 的链式设置方法返回的是父类型 —— 直接把返回值赋给
// Properties 字段会编不过，所以这里把 6 个链式方法按协变返回值重包一遍。
// 这样 ModFluids 在两个平台上可以共用同一份代码（14 种液体 × 3 个类）。
public class BaseFlowingFluid
{
    // 静止／流动流体共用的属性。
    public static class Properties extends ForgeFlowingFluid.Properties
    {
        public Properties(Supplier<? extends FluidType> type, Supplier<? extends Fluid> still,
                          Supplier<? extends Fluid> flowing)
        {
            super(type, still, flowing);
        }

        @Override
        public Properties slopeFindDistance(int distance)
        {
            super.slopeFindDistance(distance);
            return this;
        }

        @Override
        public Properties levelDecreasePerBlock(int amount)
        {
            super.levelDecreasePerBlock(amount);
            return this;
        }

        @Override
        public Properties tickRate(int rate)
        {
            super.tickRate(rate);
            return this;
        }

        @Override
        public Properties explosionResistance(float resistance)
        {
            super.explosionResistance(resistance);
            return this;
        }

        @Override
        public Properties bucket(Supplier<? extends Item> bucket)
        {
            super.bucket(bucket);
            return this;
        }

        @Override
        public Properties block(Supplier<? extends LiquidBlock> block)
        {
            super.block(block);
            return this;
        }
    }

    // 静止的流体。
    public static class Source extends ForgeFlowingFluid.Source
    {
        public Source(Properties properties)
        {
            super(properties);
        }
    }

    // 流动的流体。
    public static class Flowing extends ForgeFlowingFluid.Flowing
    {
        public Flowing(Properties properties)
        {
            super(properties);
        }
    }
}
//?}