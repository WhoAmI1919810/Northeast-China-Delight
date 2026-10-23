package com.gunmu.northeast_china_delight.compat.ali;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * ALI 展示用的补录数据：反射读不到的那几项。
 *
 * <p>各 GLM 里凡是「数组形式的权重表」，都由 {@link AliGlmReflection#tablesOf} 直接读，不在这里重复；
 * 这里只放反射读不到的东西：</p>
 *
 * <ul>
 *   <li>写在 {@code doApply}/{@code rollProcessed} 里的字面量概率（每张表「多久抽一次」）；</li>
 *   <li>猪副产物的内联候选表（{@code PigOffalLootModifier.doApply} 里现场 new 的 List，不是字段）。</li>
 * </ul>
 *
 * <p><b>这些数字只影响 ALI 里的显示，改了对应 GLM 的概率请顺手同步这里。</b>
 * 出处标注在每个常量上，改的时候按图索骥即可；不同步不会影响游戏实际掉落。</p>
 */
final class AliGlmData {
    /** {@code ButcherLootModifier.doApply}：生料一次 18% */
    static final float BUTCHER_RAW = 0.18F;
    /** {@code ButcherLootModifier.doApply}：肉菜一次 10% */
    static final float BUTCHER_DISH = 0.10F;
    /** {@code FishermanLootModifier.doApply}：生鲜一次 18% */
    static final float FISHERMAN_RAW = 0.18F;
    /** {@code FishermanLootModifier.doApply}：海味菜一次 10% */
    static final float FISHERMAN_DISH = 0.10F;
    /** {@code StructureSeedsLootModifier.doApply}：15% */
    static final float STRUCTURE_SEEDS = 0.15F;
    /** {@code VillageSeedsLootModifier.doApply}：种子抽两次（40% + 15%） */
    static final float VILLAGE_SEEDS = 0.55F;
    /** {@code VillageSeedsLootModifier.doApply}：加工品一次 15% */
    static final float VILLAGE_SEEDS_PROCESSED = 0.15F;
    /** {@code VillageSeedsLootModifier.rollProcessed}：nextInt(14) 的权重 */
    static final int VILLAGE_PROCESSED_TOTAL = 14;
    static final int VILLAGE_PROCESSED_CORN_FLOUR = 5;
    static final int VILLAGE_PROCESSED_WATER_DOUGH = 4;
    static final int VILLAGE_PROCESSED_PEANUT_BUTTER = 3;
    static final int VILLAGE_PROCESSED_PICKLED_SNOW_MUSTARD = 2;

    /**
     * {@code PigOffalLootModifier.doApply} 里的内联候选表（每种基础概率 10%，每级抢夺 +5%，
     * 一次最多出 {@code MAX_KINDS} 种）。
     */
    static List<Item> pigOffalItems() {
        return List.of(
                ModItems.PORK_RIBS.get(),
                ModItems.PORK_INTESTINE.get(),
                ModItems.PORK_HOCK.get(),
                ModItems.PIG_BLOOD.get(),
                ModItems.PIG_LIVER.get(),
                ModItems.PORK_FAT.get(),
                ModItems.OIL_EDGE.get());
    }

    /** {@code PigOffalLootModifier.getSmokedMap}：着火的猪把这些换成熏制版 */
    static List<Item[]> pigOffalSmoked() {
        return List.of(
                new Item[]{ModItems.PORK_HOCK.get(), ModItems.SMOKED_PORK_HOCK.get()},
                new Item[]{ModItems.PIG_LIVER.get(), ModItems.SMOKED_PIG_LIVER.get()});
    }

    private AliGlmData() {}
}
