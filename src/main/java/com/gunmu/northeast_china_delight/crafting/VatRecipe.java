package com.gunmu.northeast_china_delight.crafting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 一条大缸配方 —— 纯数据。这里只有"要什么、要多少、多长时间、完成后变成什么"，没有任何一句针对某个具体物品的 if。大缸的交互逻辑（{@link VatBrewing}）和
 * JEI 页面都只读这些字段，所以新增 / 修改配方 只需要在 {@link VatRecipes#REGISTRY} 里加一条或改一条，判定、计时、取货、JEI 全都会跟着变。
 */
public final class VatRecipe {

    /**
     * 开工需要的封口物
     */
    public enum Seal {
        NONE,
        PRESS,
        CLOTH,
        CARPET
    }

    /**
     * 一个投料格完成后的去向。这四种去向决定了大缸在三段时间点上的表现：投料时（能不能放、放几个）、完成时（缸里变成什么样）、取货时（玩家拿到什么）。
     */
    public enum Fate {
        KEEP,
        CONVERT,
        CONSUME,
        ABSORB
    }

    /**
     * 完成时，缸里原有的液体怎么处理
     */
    public enum LiquidAfter {
        KEEP,
        CLEAR,
        DRAIN_REQUIRED
    }

    /**
     * 大缸里能存的那几种液体（纯存储用，配方里只是引用它们的名字）
     */
    public enum Fluid {
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

    /**
     * JEI 页面上这一格画在哪一排：食材排（缸身侧面）还是配料排（缸身侧面、靠上）
     */
    public enum Layer {
        INGREDIENT,
        SEASONING
    }

    /**
     * 一个投料格在"取货"时的换算。输入永远是缸里那一份东西（一个），返回玩家应该拿到的那一份。泡菜是"大白菜→酸白菜"，腊肉是"生猪排→咸腊肉"，都用它表达。
     */
    @FunctionalInterface
    public interface Converter {
        ItemStack apply(ItemStack in);

        static Converter to(Item to) {
            return in -> new ItemStack(to);
        }

        static Converter same() {
            return in -> in;
        }
    }

    /**
     * 数量规则要用到的缸内情况
     */
    public interface Counts {
        int count(Predicate<ItemStack> matcher);

        int water();

        default int countOfSlot(int slotIndex) {
            throw new UnsupportedOperationException(
                    "Counts 要么经 VatRecipe#boundsOf 使用，要么自己实现 countOfSlot");
        }
    }

    /**
     * 数量区间
     */
    public record Bounds(int min, int max) {
        public boolean satisfied(int have) {
            return have >= this.min && have <= this.max;
        }

        public int want() {
            return this.max == Integer.MAX_VALUE ? Math.max(1, this.min) : this.max;
        }
    }

    /**
     * 一份"按格子假定份数"的缸内情况 —— JEI 展开与配方自检这类 "先假定一个缸内状态、再按数量规则算份数"的地方用。 {@link #count} 是按匹配器引用认亲的（只
     * 认这条配方里那几格的匹配器实例， {@link VatRecipe#materialsReady} 就是这么问的）； "跟着另一格走"（
     * {@link BoundsRule#sameAs}）那类跨格规则走 {@link #countOfSlot}，按槽位号取值，不存在认亲问题。
     */
    public record AssumedCounts(int water, int[] counts, VatRecipe recipe) implements Counts {

        @Override
        public int count(Predicate<ItemStack> matcher) {
            for (int i = 0; i < this.recipe.slots.size(); i++) {
                if (this.recipe.slots.get(i).matcher() == matcher) {
                    return this.counts[i];
                }
            }
            return 0;
        }

        @Override
        public int countOfSlot(int slotIndex) {
            return slotIndex >= 0 && slotIndex < this.counts.length ? this.counts[slotIndex] : 0;
        }
    }

    /**
     * 数量规则：给定缸内情况算出这一格允许的数量区间
     */
    @FunctionalInterface
    public interface BoundsRule {
        Bounds get(Counts counts);

        static BoundsRule exact(int n) {
            return counts -> new Bounds(n, n);
        }

        static BoundsRule between(int min, int max) {
            return counts -> new Bounds(min, max);
        }

        static BoundsRule perLayer(int perLayerMin, int perLayerMax) {
            return counts -> {
                int layers = Math.max(1, counts.water());
                return new Bounds(perLayerMin * layers, perLayerMax * layers);
            };
        }

        static BoundsRule perLayerExact(int per) {
            return perLayer(per, per);
        }

        static BoundsRule minWithPerLayerMax(int min, int perLayerMax) {
            return counts -> new Bounds(min, perLayerMax * Math.max(1, counts.water()));
        }

        static BoundsRule minByWater(int[] minByWaterLevel, int max) {
            return counts -> {
                int level = Math.max(0, Math.min(minByWaterLevel.length - 1, counts.water()));
                return new Bounds(minByWaterLevel[level], max);
            };
        }

        /**
         * 数量跟着另一格走（盐数 = 肉数）：下限 = ratio × 第 slotIndex 格的数量。
         *
         * <p>上限给到 ratio，好让"先放盐后放肉"也放得进去（放满 6 块肉时盐正好也是 6 份）；
         * 但"能不能开工"仍然要求上下限都满足，所以盐少了照样不开工。
         *
         * @param slotIndex 被跟随的那一格在 {@link VatRecipe#slots()} 里的下标
         */
        static BoundsRule sameAs(int slotIndex, int ratio) {
            return counts -> {
                int n = counts.countOfSlot(slotIndex) * ratio;
                return new Bounds(n, Math.max(n, ratio));
            };
        }

        static BoundsRule halfOf(int slotIndex, int max) {
            return counts -> new Bounds(
                    Math.min(max, (counts.countOfSlot(slotIndex) + 1) / 2), max);
        }
    }

    public record Slot(Predicate<ItemStack> matcher, BoundsRule bounds, Fate fate,
                       @Nullable Converter converter, @Nullable Item product, @Nullable Item refund,
                       boolean perDose, Layer layer) {

        public static Slot keep(Predicate<ItemStack> matcher, BoundsRule bounds, Converter converter) {
            return new Slot(matcher, bounds, Fate.KEEP, converter, null, null, false, Layer.INGREDIENT);
        }

        public static Slot convert(Predicate<ItemStack> matcher, BoundsRule bounds, Item product) {
            return new Slot(matcher, bounds, Fate.CONVERT, null, product, null, false, Layer.INGREDIENT);
        }

        public static Slot consume(Predicate<ItemStack> matcher, BoundsRule bounds) {
            return new Slot(matcher, bounds, Fate.CONSUME, null, null, null, false, Layer.INGREDIENT);
        }

        public static Slot absorb(Predicate<ItemStack> matcher, BoundsRule bounds) {
            return new Slot(matcher, bounds, Fate.ABSORB, null, null, null, false, Layer.INGREDIENT);
        }

        public Slot refund(Item container) {
            return new Slot(this.matcher, this.bounds, this.fate, this.converter, this.product,
                    container, this.perDose, this.layer);
        }

        public Slot dosed() {
            return new Slot(this.matcher, this.bounds, this.fate, this.converter, this.product,
                    this.refund, true, this.layer);
        }

        public Slot seasoning() {
            return new Slot(this.matcher, this.bounds, this.fate, this.converter, this.product,
                    this.refund, this.perDose, Layer.SEASONING);
        }

    }

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
    private final boolean requiresSnowyBiome;
    @Nullable
    private final java.util.function.Predicate<ItemStack> requiredSealItem;
    @Nullable
    private final java.util.EnumMap<BiomeBand, Float> biomeSecondsMultiplier;

    private VatRecipe(String id, VatRecipes.Kind kind, int priority, Seal seal, Liquid liquid, List<Slot> slots,
                      int seconds, boolean secondsPerLayer, boolean requiresSnowyBiome,
                      @Nullable java.util.function.Predicate<ItemStack> requiredSealItem,
                      @Nullable java.util.EnumMap<BiomeBand, Float> biomeSecondsMultiplier) {
        this.id = id;
        this.kind = kind;
        this.priority = priority;
        this.seal = seal;
        this.liquid = liquid;
        this.slots = List.copyOf(slots);
        this.seconds = seconds;
        this.secondsPerLayer = secondsPerLayer;
        this.requiresSnowyBiome = requiresSnowyBiome;
        this.requiredSealItem = requiredSealItem;
        this.biomeSecondsMultiplier = biomeSecondsMultiplier == null ? null : new java.util.EnumMap<>(biomeSecondsMultiplier);
    }

    /**
     * 生物群系温度带：按 {@code getBaseTemperature()} 分四档。分带的好处是 JEi / 提示里能一句话说明（"温暖群系酿得更快"），不用给玩家读一个浮点
     * 温度。
     */
    public enum BiomeBand {
        COLD,
        TEMPERATE,
        WARM,
        HOT;

        public static BiomeBand of(float baseTemperature) {
            if (baseTemperature < 0.15F) {
                return COLD;
            }
            if (baseTemperature < 0.7F) {
                return TEMPERATE;
            }
            if (baseTemperature < 0.95F) {
                return WARM;
            }
            return HOT;
        }
    }

    public String id() {
        return this.id;
    }

    public VatRecipes.Kind kind() {
        return this.kind;
    }

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

    public boolean requiresSnowyBiome() {
        return this.requiresSnowyBiome;
    }

    @Nullable
    public java.util.function.Predicate<ItemStack> requiredSealItem() {
        return this.requiredSealItem;
    }

    public int secondsFor(int waterLevel) {
        return this.secondsPerLayer ? this.seconds * Math.max(1, waterLevel) : this.seconds;
    }

    public int secondsFor(int waterLevel, BiomeBand band) {
        int base = secondsFor(waterLevel);
        if (this.biomeSecondsMultiplier == null) {
            return base;
        }
        Float mul = this.biomeSecondsMultiplier.get(band);
        return mul == null ? base : Math.max(1, Math.round(base * mul));
    }

    @Nullable
    public Float biomeMultiplier(BiomeBand band) {
        return this.biomeSecondsMultiplier == null ? null : this.biomeSecondsMultiplier.get(band);
    }

    /**
     * "这一档水位下，每一格应该放几份" —— 数量规则之间会互相引用（盐数 = 肉数），
     * 所以从"每格 1 份"出发多跑几遍让它收敛。
     *
     * <p>JEI 页面拿它显示投料量，配方自检（{@link VatRecipeValidator}）反过来用它验一遍
     * "照这个数投料到底能不能开工"，所以两边不会各算各的。
     */
    public int[] displayedCounts(int waterLevel) {
        int size = this.slots.size();
        int[] counts = new int[size];
        Arrays.fill(counts, 1);
        Counts view = new AssumedCounts(waterLevel, counts, this);
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < size; i++) {
                Bounds bounds = this.slots.get(i).bounds().get(view);
                int want = bounds.want();
                counts[i] = Math.max(0, Math.min(bounds.max(), Math.max(bounds.min(), want)));
            }
        }
        return counts;
    }

    public int productMbFor(int waterLevel) {
        return this.liquid.productPerLayer
                ? this.liquid.productMb * Math.max(1, waterLevel)
                : this.liquid.productMb;
    }

    @Nullable
    public Fluid productFluid() {
        return this.liquid.product();
    }

    public Bounds boundsOf(int slotIndex, Counts counts) {
        return this.slots.get(slotIndex).bounds().get(new SlotCounts(counts, this));
    }

    /**
     * 把"缸内情况"补上"按槽位号取数量"的能力。 "盐数 = 肉数"这类规则要跨格引用，靠匹配器实例认亲不可靠（lambda 在不同调用点是不同对象， JEI 那份
     * {@code Counts} 就是因此把肉数算成 0 的），所以统一在这里翻译：槽位号 → 那一格的匹配器 → 数量。
     */
    private record SlotCounts(Counts delegate, VatRecipe recipe) implements Counts {

        @Override
        public int count(Predicate<ItemStack> matcher) {
            return this.delegate.count(matcher);
        }

        @Override
        public int water() {
            return this.delegate.water();
        }

        @Override
        public int countOfSlot(int slotIndex) {
            if (slotIndex < 0 || slotIndex >= this.recipe.slots.size()) {
                throw new IllegalArgumentException("配方 " + this.recipe.id
                        + " 的数量规则引用了不存在的槽位 " + slotIndex);
            }
            return this.delegate.count(this.recipe.slots.get(slotIndex).matcher());
        }
    }

    public boolean accepts(int slotIndex, ItemStack stack) {
        return this.slots.get(slotIndex).matcher().test(stack);
    }

    public boolean liquidFits(int water, int sourWaterMb) {
        return this.liquid.waterFits(water) && sourWaterMb >= this.liquid.sourWaterMb();
    }

    /**
     * 这缸现在的液体还"救得回来"吗 —— 判断现在能不能继续往缸里投料。
     *
     * <p>和 {@link #liquidFits} 的区别：这里只要求没有超上限的水、酸引水不少于配方要求。
     * 于是"先放酱块后倒水""酿完酱只剩酱渣时先加谷物再倒水"这些玩法仍然成立。
     */
    public boolean canStillMatch(int water, int sourWaterMb) {
        return water <= this.liquid.waterMax() && sourWaterMb >= this.liquid.sourWaterMb();
    }

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

    public int satisfiedSlots(Counts counts) {
        int n = 0;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.boundsOf(i, counts).satisfied(counts.count(this.slots.get(i).matcher()))) {
                n++;
            }
        }
        return n;
    }

    public boolean allSlotsExact(Counts counts) {
        for (int i = 0; i < this.slots.size(); i++) {
            Bounds b = this.boundsOf(i, counts);
            int have = counts.count(this.slots.get(i).matcher());
            if (!b.satisfied(have) || have != b.min()) {
                return false;
            }
        }
        return true;
    }

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
        // 必须经 boundsOf 包装（SlotCounts），sameAs 这类跨格规则才能拿到 countOfSlot
        Bounds bounds = boundsOf(slotIndex, counts);
        return counts.count(slot.matcher()) < bounds.max();
    }

    public int findSlotFor(ItemStack stack, Counts counts) {
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.hasRoomFor(i, stack, counts)) {
                return i;
            }
        }
        return -1;
    }

    public List<Slot> keepSlots() {
        List<Slot> list = new ArrayList<>();
        for (Slot slot : this.slots) {
            if (slot.fate() == Fate.KEEP) {
                list.add(slot);
            }
        }
        return list;
    }

    public static Builder of(VatRecipes.Kind kind, String id) {
        return new Builder(kind, id);
    }

    /**
     * 配方的构建器 —— 纯数据装配，写配方时只会用到这些方法
     */
    public static final class Builder {
        private final String id;
        private final VatRecipes.Kind kind;
        private int priority;
        private Seal seal = Seal.NONE;
        private Liquid liquid = Liquid.NONE;
        private final List<Slot> slots = new ArrayList<>();
        private int seconds = 2;
        private boolean secondsPerLayer;
        private boolean requiresSnowyBiome;
        private java.util.function.Predicate<ItemStack> requiredSealItem;
        private java.util.EnumMap<BiomeBand, Float> biomeSecondsMultiplier;

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

        public Builder dry() {
            this.liquid = Liquid.NONE;
            return this;
        }

        public Builder water(int min, int max, LiquidAfter waterAfter) {
            this.liquid = new Liquid(min, max, 0, null, 0, false, waterAfter, LiquidAfter.KEEP);
            return this;
        }

        public Builder sourWater(int mb, LiquidAfter sourAfter) {
            this.liquid = new Liquid(0, 0, mb, null, 0, false, LiquidAfter.KEEP, sourAfter);
            return this;
        }

        public Builder product(Fluid fluid, int mb, boolean perLayer) {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    fluid, mb, perLayer, old.waterAfter(), old.sourWaterAfter());
            return this;
        }

        public Builder clearWater() {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    old.product(), old.productMb(), old.productPerLayer(),
                    LiquidAfter.CLEAR, old.sourWaterAfter());
            return this;
        }

        public Builder clearSourWater() {
            Liquid old = this.liquid;
            this.liquid = new Liquid(old.waterMin(), old.waterMax(), old.sourWaterMb(),
                    old.product(), old.productMb(), old.productPerLayer(),
                    old.waterAfter(), LiquidAfter.CLEAR);
            return this;
        }

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

        public Builder requiresSnowyBiome() {
            this.requiresSnowyBiome = true;
            return this;
        }

        /** 缸顶封口物必须是指定的那一种（冻梨要压雪块） */
        public Builder requiredSealItem(java.util.function.Predicate<ItemStack> matcher) {
            this.requiredSealItem = matcher;
            return this;
        }

        public Builder biomeSeconds(java.util.function.Consumer<java.util.EnumMap<BiomeBand, Float>> fill) {
            java.util.EnumMap<BiomeBand, Float> map = new java.util.EnumMap<>(BiomeBand.class);
            fill.accept(map);
            this.biomeSecondsMultiplier = map;
            return this;
        }

        public Builder slot(Slot slot) {
            this.slots.add(slot);
            return this;
        }

        public VatRecipe build() {
            return new VatRecipe(this.id, this.kind, this.priority, this.seal, this.liquid,
                    this.slots, this.seconds, this.secondsPerLayer, this.requiresSnowyBiome,
                    this.requiredSealItem, this.biomeSecondsMultiplier);
        }
    }
}
