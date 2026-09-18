package com.gunmu.dongbei_delight.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** 本模组自定义的方块状态属性 */
public class ModBlockStateProperties {

    /** 大缸发酵一共分多少步（进度条用） */
    public static final int VAT_MAX_PROGRESS = 20;

    /** 大缸水位，和炼药锅一样分 0~3 四档 */
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 3);

    /** 大缸里正在发酵的内容物 */
    public static final EnumProperty<VatContent> VAT_CONTENT =
            EnumProperty.create("content", VatContent.class);

    /** 大缸内的发酵是否已完成 */
    public static final BooleanProperty VAT_FERMENTED = BooleanProperty.create("fermented");

    /** 大缸的发酵进度：0 ~ VAT_MAX_PROGRESS */
    public static final IntegerProperty VAT_PROGRESS =
            IntegerProperty.create("progress", 0, VAT_MAX_PROGRESS);

    /** 大缸可容纳的发酵物 */
    public enum VatContent implements StringRepresentable {
        /** 空缸 */
        EMPTY("empty"),
        /** 黄豆 → 大酱 */
        SOYBEAN("soybean"),
        /** 大白菜 → 酸菜 */
        NAPA_CABBAGE("napa_cabbage"),
        /** 黄瓜 → 酸黄瓜 */
        CUCUMBER("cucumber"),
        /** 胡萝卜 → 酸胡萝卜 */
        CARROT("carrot"),
        /** 碎玉米粒 → 水面团 */
        CRUSHED_CORN("crushed_corn"),
        /** 已放盐，等待放入猪肉 */
        SALT("salt"),
        /** 盐渍猪肉（不需要水） */
        CURING_MEAT("curing_meat");

        private final String name;

        VatContent(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
