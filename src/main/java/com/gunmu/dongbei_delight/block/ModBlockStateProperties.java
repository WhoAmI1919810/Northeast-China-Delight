package com.gunmu.dongbei_delight.block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ModBlockStateProperties {
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 3);
    // 默认和炼药锅一样有四档 0~3
}