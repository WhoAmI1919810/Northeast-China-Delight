package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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
 * <p>装料可以分多次右键完成；所有输入数量满足后进入加工状态。加工满后按 {@link Result}
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

    /** 一种投入物需要多少份，以及每消耗一份返还什么容器。 */
    public record Input(Ingredient ingredient, int count, @Nullable ItemStack returnContainer) {
        public Input {
            if (count <= 0) {
                throw new IllegalArgumentException("舂捣输入数量必须为正整数");
            }
            if (returnContainer != null) {
                returnContainer = returnContainer.copy();
            }
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

    /** 手持 stack 能否作为某条舂捣配方的投入料 */
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
