package com.gunmu.northeast_china_delight.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 本模组自定义的方块状态属性
 */
public class ModBlockStateProperties {

    public static final int VAT_MAX_PROGRESS = 20;

    public static final int VAT_MAX_WATER = 3;

    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, VAT_MAX_WATER);

    public static final BooleanProperty VAT_FERMENTED = BooleanProperty.create("fermented");

    public static final BooleanProperty VAT_SALTED = BooleanProperty.create("salted");

    public static final IntegerProperty VAT_PROGRESS =
            IntegerProperty.create("progress", 0, VAT_MAX_PROGRESS);

    public static final int GRILL_MAX_PROGRESS = 4;

    public static final IntegerProperty GRILL_PROGRESS =
            IntegerProperty.create("grill_progress", 0, GRILL_MAX_PROGRESS);

}
