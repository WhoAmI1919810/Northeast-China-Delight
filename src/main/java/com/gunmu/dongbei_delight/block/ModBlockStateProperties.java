package com.gunmu.dongbei_delight.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** 本模组自定义的方块状态属性 */
public class ModBlockStateProperties {

    /** 大缸发酵一共分多少步（进度条用） */
    public static final int VAT_MAX_PROGRESS = 20;

    /** 大缸水位上限，和炼药锅一样分 0~3 四档 */
    public static final int VAT_MAX_WATER = 3;

    /** 大缸水位，和炼药锅一样分 0~3 四档 */
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, VAT_MAX_WATER);

    /** 大缸内的发酵是否已完成 */
    public static final BooleanProperty VAT_FERMENTED = BooleanProperty.create("fermented");

    /** 大缸里的水是否已经加够盐（用来决定水面是否发白） */
    public static final BooleanProperty VAT_SALTED = BooleanProperty.create("salted");

    /** 大缸的发酵进度：0 ~ VAT_MAX_PROGRESS */
    public static final IntegerProperty VAT_PROGRESS =
            IntegerProperty.create("progress", 0, VAT_MAX_PROGRESS);

    /** 烧烤架烤一串东西一共分几步（用来做烤制进度） */
    public static final int GRILL_MAX_PROGRESS = 4;

    /** 烤架上的烤制进度：0 ~ GRILL_MAX_PROGRESS */
    public static final IntegerProperty GRILL_PROGRESS =
            IntegerProperty.create("grill_progress", 0, GRILL_MAX_PROGRESS);

}
