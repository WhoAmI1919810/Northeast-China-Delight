package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 木臼「舂捣」配方的规则核心。
 *
 * <p>装料可以分多次右键完成；每放一样都按臼里全部的料重挑配方（{@link #choose}），正好凑齐某条就可以开舂，
 * 开舂那一下才定是哪条（2026-11-06 用户：几条配方共用一样料时，先放哪样都行）。加工满后按 {@link Result}
 * 逐项掷概率和数量，结果在最后一次敲击时快照进方块实体，之后重载数据包不会重掷。
 */
public final class PoundingRecipes {

    /** 加工过程中模型状态怎么切换 */
    public enum Transition {
        /** 按 {@code stage_models} 顺序依次推进 */
        SEQUENTIAL,
        /** 每捣一次在加工中的模型里随机切一个（默认） */
        RANDOM,
        /** 按条件切（预留：比如捣到特定次数才换模型） */
        CONDITIONAL;

        public static Transition parse(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return RANDOM;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "sequential", "sequence", "ordered" -> SEQUENTIAL;
                case "conditional", "condition" -> CONDITIONAL;
                default -> RANDOM;
            };
        }
    }

    /** 阶段模型画成什么样 */
    public enum StageModelMode {
        /** 阶段模型就是一整只木臼（旧写法，默认） */
        FULL,
        /** 阶段模型只有臼里的内容物，底下由渲染器另画一只空木臼 */
        OVERLAY;

        /** 认不出的写法返回 null，由解析处报错 */
        @Nullable
        public static StageModelMode parse(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return FULL;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "full" -> FULL;
                case "overlay" -> OVERLAY;
                default -> null;
            };
        }
    }

    /**
     * 一份水：水桶、水盆或一瓶水（配方 inputs 里写 {@code {"water": true}}，2026-11-05 舂湿粉团）。原料在这里用代码拼，不写进 JSON：
     * 认水瓶得看药水瓶上的「水」，这种带组件的原料（neoforge:components）里的物品列表要有注册表才解析得了，
     * 舂捣配方是用普通 JsonOps 解析的，写进 JSON 整条配方加载不上（JEI 里看不到配方，用户报了两次）
     */
    public static Ingredient water() {
        Ingredient containers = Ingredient.of(Items.WATER_BUCKET, ModItems.WATER_BASIN.get());
        //? if <1.20.5 {
        /*net.minecraft.nbt.CompoundTag potion = new net.minecraft.nbt.CompoundTag();
        potion.putString("Potion", "minecraft:water");
        return net.minecraftforge.common.crafting.CompoundIngredient.of(containers,
                net.minecraftforge.common.crafting.PartialNBTIngredient.of(Items.POTION, potion));
        *///?} else {
        return net.neoforged.neoforge.common.crafting.CompoundIngredient.of(containers,
                net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false,
                        net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                                net.minecraft.world.item.alchemy.Potions.WATER)));
        //?}
    }

    /**
     * 一种投入物需要多少份，以及每消耗一份返还什么容器（不写就按 {@link #leftover} 还）。
     *
     * @param model 料还没放齐时，这一样放进去了臼里画的模型（overlay，同 stage_models；2026-11-05 用户：红薯和水得都放进去才有模型，
     *              要各加载各的）。几样都放了就几个一起画；放齐以后照常画 stage_models 的第一个。null = 放齐前不画
     */
    public record Input(Ingredient ingredient, int count, @Nullable ItemStack returnContainer,
                        @Nullable ResourceLocation model) {
        public Input {
            if (count <= 0) {
                throw new IllegalArgumentException("舂捣输入数量必须为正整数");
            }
            if (returnContainer != null) {
                returnContainer = returnContainer.copy();
            }
        }

        public Input(Ingredient ingredient, int count, @Nullable ItemStack returnContainer) {
            this(ingredient, count, returnContainer, null);
        }

        /**
         * 投进去的一份用完剩下的容器：配方写了 {@code return_container} 就还它；没写就看这样东西自己剩什么
         * （2026-11-05 加：湿粉团的水可以是一桶、一盆或一瓶，各还各的）——药水瓶（水瓶）还空玻璃瓶，
         * 有合成剩余物的还剩余物（水桶 → 空桶、水盆 → 空大脸盆），别的什么都不还
         */
        public ItemStack leftover(ItemStack inserted) {
            if (returnContainer != null) {
                return returnContainer.copy();
            }
            if (inserted.is(Items.POTION)) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            return inserted.hasCraftingRemainingItem() ? inserted.getCraftingRemainingItem().copy() : ItemStack.EMPTY;
        }
    }

    /**
     * 一项产物。
     *
     * @param minCount 最小数量
     * @param maxCount 最大数量，和 minCount 相同表示固定数量
     * @param chance   独立出现概率，1 表示必定出现
     * @param byproduct 仅用于 JEI 标记，发奖逻辑与普通产物相同
     * @param pool     随机池 id；非空时同一池的条目只会选中一个
     * @param weight   池内权重
     * @param poolChance 整个随机池出现的概率
     */
    public record Result(ItemStack stack, int minCount, int maxCount, float chance, boolean byproduct,
                         @Nullable String pool, int weight, float poolChance) {
        public Result {
            if (stack.isEmpty() || minCount <= 0 || maxCount < minCount) {
                throw new IllegalArgumentException("无效的舂捣产物");
            }
            stack = stack.copy();
            chance = Math.max(0.0F, Math.min(1.0F, chance));
            weight = Math.max(1, weight);
            poolChance = Math.max(0.0F, Math.min(1.0F, poolChance));
            if (pool != null && pool.isBlank()) {
                pool = null;
            }
        }

        public Result(ItemStack stack, int minCount, int maxCount, float chance, boolean byproduct) {
            this(stack, minCount, maxCount, chance, byproduct, null, 1, 1.0F);
        }

        public boolean pooled() {
            return this.pool != null;
        }

        public int rollCount(RandomSource random) {
            return this.minCount == this.maxCount
                    ? this.minCount
                    : this.minCount + random.nextInt(this.maxCount - this.minCount + 1);
        }
    }

    /**
     * @param inputs          多种输入及各自数量
     * @param chargeThreshold 本配方要求的挥击蓄力比例，0..1
     * @param stageModels     装料/加工中各阶段方块模型；数量决定 STATE 上限
     * @param results         多项产物，各自独立掷概率
     * @param stageModelMode  阶段模型是整只木臼还是只有内容物
     * @param finishedModel   成品状态单独的模型；不写时沿用最后一个阶段模型
     * @param resultContainer 取成品时手里必须拿着的容器（比如舂出来的油要拿玻璃瓶装），每取一次扣一个；null 表示空手就能取
     * @param resultMb        大于 0 时成品按毫升装：唯一的产物是一种瓶装调料，拿空容器装出一瓶这么多毫升的，
     *                        拿没装满的同种瓶子就往里续；0 表示成品就是整件物品
     */
    public record Recipe(ResourceLocation id, int priority, List<Input> inputs, Ingredient tool,
                         int poundTimes, float chargeThreshold, List<ResourceLocation> stageModels,
                         List<Result> results, Transition transition, StageModelMode stageModelMode,
                         @Nullable ResourceLocation finishedModel, @Nullable ItemStack resultContainer,
                         int resultMb) {

        public Recipe {
            inputs = List.copyOf(inputs);
            stageModels = List.copyOf(stageModels);
            results = List.copyOf(results);
            if (inputs.isEmpty() || stageModels.isEmpty() || results.isEmpty()) {
                throw new IllegalArgumentException("舂捣配方不能没有输入、阶段模型或产物");
            }
            chargeThreshold = Math.max(0.0F, Math.min(1.0F, chargeThreshold));
            if (stageModelMode == null) {
                stageModelMode = StageModelMode.FULL;
            }
            if (resultContainer != null) {
                resultContainer = resultContainer.copyWithCount(1);
            }
            resultMb = Math.max(0, resultMb);
        }

        /** 按毫升装时，装的是哪种瓶装调料 */
        public ItemStack liquidProduct() {
            return this.results.get(0).stack();
        }

        /** 旧写法：阶段模型是整只木臼，成品沿用最后一个阶段模型 */
        public Recipe(ResourceLocation id, int priority, List<Input> inputs, Ingredient tool,
                      int poundTimes, float chargeThreshold, List<ResourceLocation> stageModels,
                      List<Result> results, Transition transition) {
            this(id, priority, inputs, tool, poundTimes, chargeThreshold, stageModels, results, transition,
                    StageModelMode.FULL, null, null, 0);
        }

        /** 装料后的第一个状态（stageModels[0]） */
        public int filledState() {
            return 1;
        }

        /** 成品状态 index（blockstate STATE 值） */
        public int doneState() {
            return this.stageModels.size() + 1;
        }

        public ResourceLocation stageModel(int state) {
            if (state >= this.doneState() && this.finishedModel != null) {
                return this.finishedModel;
            }
            int index = Math.max(0, Math.min(this.stageModels.size() - 1, state - this.filledState()));
            return this.stageModels.get(index);
        }

        public boolean matchesInput(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            for (Input input : this.inputs) {
                if (input.ingredient().test(stack)) {
                    return true;
                }
            }
            return false;
        }

        /** 给原版配方书 / 其它只支持单产物的接口使用。 */
        public ItemStack previewResult() {
            Result chosen = null;
            for (Result result : this.results) {
                if (result.pooled()) {
                    continue;
                }
                if (!result.byproduct() && result.chance() >= 1.0F) {
                    chosen = result;
                    break;
                }
                if (chosen == null) {
                    chosen = result;
                }
            }
            if (chosen == null) {
                for (Result result : this.results) {
                    if (result.pooled()) {
                        chosen = result;
                        break;
                    }
                }
            }
            if (chosen == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = chosen.stack().copy();
            stack.setCount(Math.min(chosen.minCount(), Math.max(1, stack.getMaxStackSize())));
            return stack;
        }

        public List<ItemStack> rollResults(RandomSource random) {
            List<ItemStack> rolled = new ArrayList<>();
            Map<String, List<Result>> pools = new LinkedHashMap<>();
            for (Result result : this.results) {
                if (result.pooled()) {
                    pools.computeIfAbsent(result.pool(), key -> new ArrayList<>()).add(result);
                    continue;
                }
                if (result.chance() < 1.0F && random.nextFloat() >= result.chance()) {
                    continue;
                }
                addRolled(rolled, result, random);
            }

            for (List<Result> pool : pools.values()) {
                Result first = pool.get(0);
                if (first.poolChance() < 1.0F && random.nextFloat() >= first.poolChance()) {
                    continue;
                }
                int totalWeight = 0;
                for (Result entry : pool) {
                    totalWeight += entry.weight();
                }
                int pick = random.nextInt(Math.max(1, totalWeight));
                Result chosen = first;
                for (Result entry : pool) {
                    if (pick < entry.weight()) {
                        chosen = entry;
                        break;
                    }
                    pick -= entry.weight();
                }
                addRolled(rolled, chosen, random);
            }
            return List.copyOf(rolled);
        }

        private static void addRolled(List<ItemStack> rolled, Result result, RandomSource random) {
            int remaining = result.rollCount(random);
            int maxStack = Math.max(1, result.stack().getMaxStackSize());
            while (remaining > 0) {
                int count = Math.min(remaining, maxStack);
                ItemStack stack = result.stack().copy();
                stack.setCount(count);
                rolled.add(stack);
                remaining -= count;
            }
        }

        /** 是否仍然引用内置的 wooden_mortar_1..N 序列；渲染器和 JEI 据此选择模型。 */
        public boolean usesDefaultModels() {
            for (int i = 0; i < this.stageModels.size(); i++) {
                ResourceLocation expected = DdIds.of(NortheastChinaDelight.MODID,
                        "block/wooden_mortar_" + (i + 1));
                if (!expected.equals(this.stageModels.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    private PoundingRecipes() {
    }

    public static List<Recipe> all(RecipeManager manager) {
        List<Recipe> recipes = new ArrayList<>();
        //? if <1.20.5 {
        /*for (PoundDataRecipe entry : manager.getAllRecipesFor(ModPoundRecipes.TYPE.get())) {
            Recipe recipe = entry.rule(entry.getId());
        *///?} else {
        for (var entry : manager.getAllRecipesFor(ModPoundRecipes.TYPE.get())) {
            Recipe recipe = entry.value().rule(entry.id());
        //?}
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        recipes.sort(Comparator.comparingInt(Recipe::priority).reversed()
                .thenComparing(recipe -> recipe.id().toString()));
        return List.copyOf(recipes);
    }

    /** 手持 stack 能否作为某条舂捣配方的投入料（木臼放料已改用 {@link #room} / {@link #choose}，这里留给别处查询） */
    @Nullable
    public static Recipe byInput(RecipeManager manager, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Recipe recipe : all(manager)) {
            if (recipe.matchesInput(stack)) {
                return recipe;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 按臼里的料挑配方

    /**
     * 臼里的料按某条配方分好格：{@code slots} 和配方 inputs 一一对应；{@code complete} = 每格都正好凑齐。
     *
     * <p>2026-11-06 用户：「臼应该在玩家捶打时，才检查匹配哪一项配方」。放料时不再一放第一样就认死一条配方
     * （以前熟花生一放进去就凑齐了花生酱，想加蜂蜜做别的已经来不及），而是每放一样都按臼里全部的料重新挑，
     * 开舂那一下再挑一遍才算数，见 {@code WoodenMortarBlock#pound}。</p>
     */
    public record Fit(Recipe recipe, List<ItemStack> slots, boolean complete) {
    }

    /**
     * 这些料能不能全放进这条配方：每样都有一格认它、每格不超过配方要的数、一格只放同一种东西。
     * 能就返回分好的各格（没放的格是空），不能返回 null。
     */
    @Nullable
    public static List<ItemStack> fit(Recipe recipe, List<ItemStack> contents) {
        List<Input> inputs = recipe.inputs();
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            slots.add(ItemStack.EMPTY);
        }
        for (ItemStack stack : contents) {
            int left = stack.getCount();
            for (int i = 0; i < inputs.size() && left > 0; i++) {
                ItemStack in = slots.get(i);
                if (!inputs.get(i).ingredient().test(stack) || (!in.isEmpty() && !same(in, stack))) {
                    continue;
                }
                int moved = Math.min(inputs.get(i).count() - in.getCount(), left);
                if (moved <= 0) {
                    continue;
                }
                if (in.isEmpty()) {
                    slots.set(i, stack.copyWithCount(moved));
                } else {
                    in.grow(moved);
                }
                left -= moved;
            }
            if (left > 0) {
                return null;
            }
        }
        return slots;
    }

    /** 分好的各格是不是每格都凑齐了 */
    public static boolean complete(Recipe recipe, List<ItemStack> slots) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            if (slots.get(i).getCount() < recipe.inputs().get(i).count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 臼里这些料算哪条配方：放得下的配方里先挑正好凑齐的，没有凑齐的就挑第一条放得下的（显示放了一半的料用）；
     * 都放不下返回 null。同样凑齐的几条按 priority、再按 id 排，同 {@link #all}。
     */
    @Nullable
    public static Fit choose(RecipeManager manager, List<ItemStack> contents) {
        return choose(all(manager), contents);
    }

    /** 同上，配方由调用的地方给（按 {@link #all} 的顺序排好；测试用） */
    @Nullable
    public static Fit choose(List<Recipe> recipes, List<ItemStack> contents) {
        Fit partial = null;
        for (Recipe recipe : recipes) {
            List<ItemStack> slots = fit(recipe, contents);
            if (slots == null) {
                continue;
            }
            if (complete(recipe, slots)) {
                return new Fit(recipe, slots, true);
            }
            if (partial == null) {
                partial = new Fit(recipe, slots, false);
            }
        }
        return partial;
    }

    /**
     * 这一下右键该往臼里放几个手里的东西：放得下的每条配方各自还能再收几个，取最少的那个；一条都收不下返回 0。
     * 几条配方要的数不一样时就一个一个放，好停在要得少的那条上；只有一条用得上时一下放够（同以前）。
     */
    public static int room(RecipeManager manager, List<ItemStack> contents, ItemStack held) {
        return room(all(manager), contents, held);
    }

    /** 同上，配方由调用的地方给（测试用） */
    public static int room(List<Recipe> recipes, List<ItemStack> contents, ItemStack held) {
        int room = 0;
        for (Recipe recipe : recipes) {
            int fits = 0;
            for (int n = 1; n <= held.getCount(); n++) {
                List<ItemStack> more = new ArrayList<>(contents);
                more.add(held.copyWithCount(n));
                if (fit(recipe, more) == null) {
                    break;
                }
                fits = n;
            }
            if (fits > 0) {
                room = room == 0 ? fits : Math.min(room, fits);
            }
        }
        return room;
    }

    private static boolean same(ItemStack a, ItemStack b) {
        //? if <1.20.5 {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?} else {
        return ItemStack.isSameItemSameComponents(a, b);
        //?}
    }

    @Nullable
    public static Recipe byId(RecipeManager manager, ResourceLocation id) {
        //? if <1.20.5 {
        /*var loaded = manager.byKey(id).orElse(null);
        *///?} else {
        var loaded = manager.byKey(id).map(entry -> entry.value()).orElse(null);
        //?}
        return loaded instanceof PoundDataRecipe recipe ? recipe.rule(id) : null;
    }
}
