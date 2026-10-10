package com.gunmu.northeast_china_delight.fluid;

//? if <1.20.2 {
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.function.Supplier;

public class BaseFlowingFluid
{
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

    public static class Source extends ForgeFlowingFluid.Source
    {
        public Source(Properties properties)
        {
            super(properties);
        }
    }

    public static class Flowing extends ForgeFlowingFluid.Flowing
    {
        public Flowing(Properties properties)
        {
            super(properties);
        }
    }
}
//?}