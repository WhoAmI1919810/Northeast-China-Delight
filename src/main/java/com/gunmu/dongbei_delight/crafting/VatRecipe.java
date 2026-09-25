package com.gunmu.dongbei_delight.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 一条大缸配方 —— **纯数据**。
 *
 * <p>这里只有"要什么、要多少、多长时间、完成后变成什么"，没有任何一句针对某个具体物品的 if。
 * 大缸的交互逻辑（{@link VatBrewing}）和 JEI 页面都只读这些字段，所以新增 / 修改配方
 * **只需要在 {@link VatRecipes#REGISTRY} 里加一条或改一条**，判定、计时、取货、JEI 全都会跟着变。
 *
 * <p>一条配方由四部分组成：
 * <ol>
 *   <li>{@link Liquid}：要不要水（几层）、要不要酸引水（多少 mB）；</li>
 *   <li>{@link Slot}：投料格 —— 匹配器 + 数量规则 + 完成后的去向；</li>
 *   <li>{@link Seal}：开工要压缸石 / 蒙粗布毯 / 蒙羊毛地毯；</li>
 *   <li>完成后的变化：产出什么液体、水位和酸引水怎么变（都在 {@link Liquid} 里）。</li>
 * </ol>
 *
 * <p>数量规则（{@link Bounds}）支持"跟着水位走"和"跟着另一格走"，
 * 所以像"一层水配两份菜""盐数等于肉数"这种关系也都是数据，不是代码。
 */
public final class VatRecipe {

    /** 开工需要的封口物 */
    public enum Seal {
        /** 不需要封口 */
        NONE,
        /** 压缸石 */
        PRESS,
        /** 农夫乐事的粗布毯 */
        CLOTH,
        /** 羊毛地毯 */
        CARPET
    }

    /**
     * 一个投料格完成后的去向。
     *
     * <p>这四种去向决定了大缸在三段时间点上的表现：
     * 投料时（能不能放、放几个）、完成时（缸里变成什么样）、取货时（玩家拿到什么）。
     */
    public enum Fate {
        /**
         * 原样留在缸里；玩家取出来时按 {@link Slot#converter()} 换成成品。
         * 泡菜 / 腊肉 / 咸鱼走这条 —— 发酵完缸里还是菜和肉，取出来才是腌好的。
         */
        KEEP,
        /**
         * 完成时在原地变成 {@link Slot#product()}（酱块→酱渣、黄豆→豆芽、玉米粒→酸玉米粒），
         * 之后玩家取出的是这个产物。
         */
        CONVERT,
        /**
         * 完成时就被吸收 / 消耗掉（水里的盐、酱油用的小麦、酿醋的谷物、格瓦斯的面包）。
         * 缸里看不见了，也不需要玩家取。
         */
        CONSUME,
        /**
         * 完成时**仍然留在缸里**，因为它还要影响外观（盐水让肉发白、辣椒酱让水面和菜发红），
         * 玩家取货时才消失。容器（瓶 / 碗）在投料时就已经还给玩家。
         */
        ABSORB
    }

    /** 完成时，缸里原有的液体怎么处理 */
    public enum LiquidAfter {
        /** 保持不动 */
        KEEP,
        /** 全部清空（水被吸收、或者变成了成品液体） */
        CLEAR,
        /** 只扣掉配方要求的那一份（例如白醋只喝掉 750 mB 酸引水，剩下的是剩下的） */
        DRAIN_REQUIRED
    }

    /** 大缸里能存的那几种液体（纯存储用，配方里只是引用它们的名字） */
    public enum Fluid {
        /** 清水：不占成品槽，按 mB 记在 VatBlockEntity#waterMb */
        WATER,
        SOUR_WATER,
        PASTE,
        SOY_SAUCE,
        VINEGAR,
        WHITE_VINEGAR,
        FISH_SAUCE,
        SHRIMP_PASTE,
        KVASS
    }

    /** JEI 页面上这一格画在哪一排：食材排（缸身侧面）还是配料排（缸身侧面、靠上） */
    public enum Layer {
        INGREDIENT,
        SEASONING
    }

    /**
     * 一个投料格在"取货"时的换算。
     *
     * <p>输入永远是缸里那一份东西（一个），返回玩家应该拿到的那一份。
     * 泡菜是"大白菜→酸白菜"，腊肉是"生猪排→咸腊肉"，都用它表达。
     */
    @FunctionalInterface
    public interface Converter {
        ItemStack apply(ItemStack in);

        /** 固定换成某一种东西 */
        static Converter to(Item to) {
            return in -> new ItemStack(to);
        }

        /** 原样 */
        static Converter same() {
            return in -> in;
        }
    }

    /** 数量规则要用到的缸内情况 */
    public interface Counts {
        /** 缸里匹配某个条件的物品总数 */
        int count(Predicate<ItemStack> matcher);

        /** 缸里的清水层数（一层 = 1000 mB） */
        int water();
    }

    /** 数量区间 */
    public record Bounds(int min, int max) {
        public boolean satisfied(int have) {
            return have >= this.min && have <= this.max;
        }

        /** 这一格至少要几份（JEI 展示用） */
        public int want() {
            return this.max == Integer.MAX_VALUE ? this.min : this.max;
        }
    }

    /** 数量规则：给定缸内情况算出这一格允许的数量区间 */
    @FunctionalInterface
    public interface BoundsRule {
        Bounds get(Counts counts);

        /** 固定数量（下限 = 上限） */
        static BoundsRule exact(int n) {
            return counts -> new Bounds(n, n);
        }

        /** 固定区间 */
        static BoundsRule between(int min, int max) {
            return counts -> new Bounds(min, max);
        }

        /** 跟着水位走：每层水对应 [perLayerMin, perLayerMax] 份 */
        static BoundsRule perLayer(int perLayerMin, int perLayerMax) {
            return counts -> {
                int layers = Math.max(1, counts.water());
                return new Bounds(perLayerMin * layers, perLayerMax * layers);
            };
        }

        /** 跟着水位走，而且上下限相同 */
        static BoundsRule perLayerExact(int per) {
            return perLayer(per, per);
        }

        /** 下限固定、上限按水位放大（泡菜：至少 1 份菜，每层最多 2 份） */
        static BoundsRule minWithPerLayerMax(int min, int perLayerMax) {
            return counts -> new Bounds(min, perLayerMax * Math.max(1, counts.water()));
        }

        /** 下限按水位查表（比如辣白菜的调味品：1、2 层要 1 份，3 层要 2 份） */
        static BoundsRule minByWater(int[] minByWaterLevel, int max) {
            return counts -> {
                int level = Math.max(0, Math.min(minByWaterLevel.length - 1, counts.water()));
                return new Bounds(minByWaterLevel[level], max);
            };
        }

        /**
         * 数量跟着**另一格**走（盐数 = 肉数）：下限 = ratio × 那一格的数量。
         *
         * <p>上限给到 ratio，好让"先放盐后放肉"也放得进去（放满 6 块肉时盐正好也是 6 份）；
         * 但"能不能开工"仍然要求上下限都满足，所以盐少了照样不开工。
         */
        static BoundsRule sameAs(Predicate<ItemStack> other, int ratio) {
            return counts -> {
                int n = counts.count(other) * ratio;
                return new Bounds(n, Math.max(n, ratio));
            };
        }
    }

    /**
     * 一个投料格。
     *
     * @param matcher   这一格收什么
     * @param bounds    数量规则
     * @param fate      完成后的去向
     * @param converter {@code KEEP} 时：取货换算（null = 原样）
     * @param product   {@code CONVERT} 时：变成什么
     * @param refund    连瓶 / 连碗扔进缸的那种调料：投料时先把空容器还给玩家
     * @param layer     JEI 画在食材排还是配料排
     */
    public record Slot(Predicate<ItemStack> matcher, BoundsRule bounds, Fate fate,
                       @Nullable Converter converter, @Nullable Item product, @Nullable Item refund,
                       Layer layer) {

        /** 留着，取货时换成 converter 的结果 */
        public static Slot keep(Predicate<ItemStack> matcher, BoundsRule bounds, Converter converter) {
            return new Slot(matcher, bounds, Fate.KEEP, converter, null, null, Layer.INGREDIENT);
        }

        /** 完成时变成 product，之后玩家取出的就是 product */
        public static Slot convert(Predicate<ItemStack> matcher, BoundsRule bounds, Item product) {
            return new Slot(matcher, bounds, Fate.CONVERT, null, product, null, Layer.INGREDIENT);
        }

        /** 完成时被吸收 / 消耗 */
        public static Slot consume(Predicate<ItemStack> matcher, BoundsRule bounds) {
            return new Slot(matcher, bounds, Fate.CONSUME, null, null, null, Layer.INGREDIENT);
        }

        /** 完成时仍留在缸里影响外观，玩家取货时才消失 */
        public static Slot absorb(Predicate<ItemStack> matcher, BoundsRule bounds) {
            return new Slot(matcher, bounds, Fate.ABSORB, null, null, null, Layer.INGREDIENT);
        }

        /** 这种调料是连瓶 / 连碗一起扔进缸的：投料时先把空容器还给玩家 */
        public Slot refund(Item container) {
            return new Slot(this.matcher, this.bounds, this.fate, this.converter, this.product,
                    container, this.layer);
        }

        /** 这一格算「配料」（盐、小麦、辣椒酱、鱼露…），JEI 里画在食材上面那一排 */
        public Slot seasoning() {
            return new Slot(this.matcher, this.bounds, this.fate, this.converter, this.product,
                    this.refund, Layer.SEASONING);
        }

    }

    /**
     * 液体要求 + 完成后的液体变化。
     *
     * @param waterMin        需要的最少清水层数
     * @param waterMax        允许的最多清水层数（超过就说明这缸不是给它准备的）
     * @param sourWaterMb     需要的酸引水量（mB）
     * @param product         完成后产出的液体（null = 没有液体产物）
     * @param productMb       产出的量（mB）
     * @param productPerLayer 产出的量按水位翻倍（泡菜：几层水就出几层酸引水）
     * @param waterAfter      完成后清水怎么处理
     * @param sourWaterAfter  完成后酸引水怎么处理
     */
    public record Liquid(int waterMin, int waterMax, int sourWaterMb, @Nullable Fluid product,
                         int productMb, boolean productPerLayer,
                         LiquidAfter waterAfter, LiquidAfter sourWaterAfter) {

        public static final Liquid NONE = new Liquid(0, 0, 0, null, 0, false,
                LiquidAfter.KEEP, LiquidAfter.KEEP);

        public boolean waterFits(int water) {
            return water >= this.waterMin && water <= this.waterMax;
        }
    }

    private final String id;
    private final VatRecipes.Kind kind;
    private final int priority;
    private final Seal seal;
    private final Liquid liquid;
    private final List<Slot> slots;
    private final int seconds;
    private final boolean secondsPerLayer;

    private VatRecipe(String id, VatRecipes.Kind kind, int priority, Seal seal, Liquid liquid, List<Slot> slots,
                      int seconds, boolean secondsPerLayer) {
        this.id = id;
        this.kind = kind;
        this.priority = priority;
        this.seal = seal;
        this.liquid = liquid;
        this.slots = List.copyOf(slots);
        this.seconds = seconds;
        this.secondsPerLayer = secondsPerLayer;
    }

    // ===== 只读访问 =====

    public String id() {
        return this.id;
    }

    /** 这条配方对应大缸的哪种加工状态（存储 / 渲染 / Jade 文案用） */
    public VatRecipes.Kind kind() {
        return this.kind;
    }

    /** 同时匹配多条配方时，priority 大的优先 */
    public int priority() {
        return this.priority;
    }

    public Seal seal() {
        return this.seal;
    }

    public Liquid liquid() {
        return this.liquid;
    }

    public List<Slot> slots() {
        return this.slots;
    }

    public int seconds() {
        return this.seconds;
    }

    public boolean secondsPerLayer() {
        return this.secondsPerLayer;
    }

    /** 时长（秒）：按水位配比的配方会乘上水位 */
    public int secondsFor(int waterLevel) {
        return this.secondsPerLayer ? this.seconds * Math.max(1, waterLevel) : this.seconds;
    }

    /** 完成后产出的液体量（mB），按水位配比的配方会乘上水位 */
    public int productMbFor(int waterLevel) {
        return this.liquid.productPerLayer
                ? this.liquid.productMb * Math.max(1, waterLevel)
                : this.liquid.productMb;
    }

    /** 这条配方的液体产物（不是"缸底要放什么液体"，那是 {@link #liquid()}） */
    @Nullable
    public Fluid productFluid() {
        return this.liquid.product();
    }

    // ===== 通用判定（不认识任何具体物品）=====

    /** 这一格现在允许放几个 */
    public Bounds boundsOf(int slotIndex, Counts counts) {
        return this.slots.get(slotIndex).bounds().get(counts);
    }

    /** 这一格收不收这种东西 */
    public boolean accepts(int slotIndex, ItemStack stack) {
        return this.slots.get(slotIndex).matcher().test(stack);
    }

    /** 液体条件是否满足（水层 / 酸引水） */
    public boolean liquidFits(int water, int sourWaterMb) {
        return this.liquid.waterFits(water) && sourWaterMb >= this.liquid.sourWaterMb();
    }

    /**
     * 这缸现在的液体还"救得回来"吗 —— 判断现在能不能继续往缸里投料。
     *
     * <p>和 {@link #liquidFits} 的区别：这里只要求**没有超上限的水**、酸引水不少于配方要求。
     * 于是"先放酱块后倒水""酿完酱只剩酱渣时先加谷物再倒水"这些玩法仍然成立。
     */
    public boolean canStillMatch(int water, int sourWaterMb) {
        return water <= this.liquid.waterMax() && sourWaterMb >= this.liquid.sourWaterMb();
    }

    /** 材料是否配齐（不含封口物） */
    public boolean materialsReady(Counts counts, int sourWaterMb) {
        if (!this.liquidFits(counts.water(), sourWaterMb)) {
            return false;
        }
        for (int i = 0; i < this.slots.size(); i++) {
            if (!this.boundsOf(i, counts).satisfied(counts.count(this.slots.get(i).matcher()))) {
                return false;
            }
        }
        return true;
    }

    /** 已经满足的格子数（用来在重叠的配方之间挑最贴切的那一条） */
    public int satisfiedSlots(Counts counts) {
        int n = 0;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.boundsOf(i, counts).satisfied(counts.count(this.slots.get(i).matcher()))) {
                n++;
            }
        }
        return n;
    }

    /** 所有格子"最少要几份"的合计（越小说明这条配方越容易被满足） */
    public int minimumTotal(Counts counts) {
        int n = 0;
        for (int i = 0; i < this.slots.size(); i++) {
            n += Math.max(1, this.boundsOf(i, counts).min());
        }
        return n;
    }

    /**
     * 这一格还能不能再放一份这种东西（只看这一格的数量）。
     */
    public boolean hasRoomFor(int slotIndex, ItemStack stack, Counts counts) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.matcher().test(stack)) {
            return false;
        }
        Bounds bounds = slot.bounds().get(counts);
        return counts.count(slot.matcher()) < bounds.max();
    }

    /** 找到第一个收得下这种东西、且还有位置的格子；没有就返回 -1 */
    public int findSlotFor(ItemStack stack, Counts counts) {
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.hasRoomFor(i, stack, counts)) {
                return i;
            }
        }
        return -1;
    }

    /** 取货用的格子（KEEP 的成品格） */
    public List<Slot> keepSlots() {
        List<Slot> list = new ArrayList<>();
        for (Slot slot : this.slots) {
            if (slot.fate() == Fate.KEEP) {
                list.add(slot);
            }
        }
        return list;
    }

    /** 组装用 */
    public static Builder of(VatRecipes.Kind kind, String id) {
        return new Builder(kind, id);
    }

    /** 配方的构建器 —— 纯数据装配，写配方时只会用到这些方法 */
    public static final class Builder {
        private final String id;
        private final VatRecipes.Kind kind;
        private int priority;
        private Seal seal = Seal.NONE;
        private Liquid liquid = Liquid.NONE;
        private final List<Slot> slots = new ArrayList<>();
        private int seconds = 2;
        private boolean secondsPerLayer;

        private Builder(VatRecipes.Kind kind, String id) {
            this.kind = kind;
            this.id = id;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder seal(Seal seal) {
            this.seal = seal;
            return this;
        }

        /** 不要任何液体（也不允许缸里有水、有酸引水） */
        public Builder dry() {
            this.liquid = Liquid.NONE;
            return this;
        }

        /** 要水：水位在 [min,max] 层之间；完成后水位怎么变由 waterAfter 决定 */
        public Builder water(int min, int max, LiquidAfter waterAfter) {
            this.liquid = new Liquid(min, max, 0, null, 0, false, waterAfter, LiquidAfter.KEEP);
            return this;
        }

        /** 要酸引水：至少 mb 毫升；完成后怎么变由 sourAfter 决定 */
        public Builder sourWater(int mb, LiquidAfter sourAfter) {
            this.liquid = new Liquid(0, 0, mb, null, 0, false, LiquidAfter.KEEP, sourAfter);
            return this;
        }

        /** 产出液体 */
        public Builder product(Fluid fluid, int mb, boolean perLayer) {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    fluid, mb, perLayer, old.waterAfter(), old.sourWaterAfter());
            return this;
        }

        /** 完成后水位清 0 */
        public Builder clearWater() {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    old.product(), old.productMb(), old.productPerLayer(),
                    LiquidAfter.CLEAR, old.sourWaterAfter());
            return this;
        }

        /** 完成后酸引水清 0 */
        public Builder clearSourWater() {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    old.product(), old.productMb(), old.productPerLayer(),
                    old.waterAfter(), LiquidAfter.CLEAR);
            return this;
        }

        /** 完成后把水位等量变成酸引水（泡菜用）：一层水 = 1000 mB 酸引水 */
        public Builder waterBecomesSourWater() {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    Fluid.SOUR_WATER, VatRecipes.WATER_MB_PER_LEVEL, true,
                    LiquidAfter.CLEAR, LiquidAfter.KEEP);
            return this;
        }

        public Builder seconds(int seconds) {
            this.seconds = seconds;
            return this;
        }

        public Builder perLayerSeconds() {
            this.secondsPerLayer = true;
            return this;
        }

        public Builder slot(Slot slot) {
            this.slots.add(slot);
            return this;
        }

        public VatRecipe build() {
            return new VatRecipe(this.id, this.kind, this.priority, this.seal, this.liquid,
                    this.slots, this.seconds, this.secondsPerLayer);
        }
    }
}
