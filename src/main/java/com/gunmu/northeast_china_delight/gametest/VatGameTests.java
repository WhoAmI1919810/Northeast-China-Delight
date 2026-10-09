package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.compat.jei.VatJeiRecipe;
import com.gunmu.northeast_china_delight.compat.jei.VatJeiRecipes;
import com.gunmu.northeast_china_delight.crafting.VatBrewing;
import com.gunmu.northeast_china_delight.crafting.VatRecipe;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.crafting.VatRecipeValidator;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
*///?}
//? if <1.20.2 {
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.util.List;

/**
 * 大缸的自动化回归（跑法：{@code gradlew runGameTestServer}）。
 *
 * <p>这里覆盖的是"看代码看不出来、只能真跑一遍"的几条：
 * <ul>
 *   <li>{@link #vatRecipesPassSelfCheck()}：配方注册表自检（含"JEI 展示的份数真的能开工"）；</li>
 *   <li>{@link #vatJeiCountsMatchRecipe()}：JEI 上"6 肉 1 盐"那类份数错；</li>
 *   <li>{@link #finishedPasteCanBrewVinegar()} / {@link #pickleSourWaterCanBrewWhiteVinegar()}：
 *       二次发酵（做好的缸就地接着酿）；</li>
 *   <li>{@link #waterAfterSealingStartsBrew()} / {@link #coverComesBackBeforeFermented()}：
 *       盖布的先后顺序不该把人卡死。</li>
 * </ul>
 *
 * <p>用的空结构在 {@code data/northeast_china_delight/structure/empty.nbt}（5×5×5 全空气）。
 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class VatGameTests {

    private VatGameTests() {
    }

    /** 假玩家：1.20.1 的 GameTestHelper 拆成 makeMockPlayer / makeMockSurvivalPlayer 两个方法 */
    private static Player mockPlayer(GameTestHelper helper, GameType gameType) {
        //? if <1.20.5 {
        return gameType == GameType.CREATIVE ? helper.makeMockPlayer() : helper.makeMockSurvivalPlayer();
        //?} else {
        /*return helper.makeMockPlayer(gameType);
        *///?}
    }

    /** 大缸在结构里的相对坐标 */
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    // ===== 配方表本身 =====

    @GameTest(template = "empty")
    public static void vatRecipesPassSelfCheck(GameTestHelper helper) {
        List<String> problems = VatRecipeValidator.check(VatRecipes.all());
        if (!problems.isEmpty()) {
            helper.fail("配方自检没过：" + problems);
        }
        helper.succeed();
    }

    /** 展示的份数必须满足配方本身：咸腊肉/咸鱼是"盐数 = 肉数"，不是 1 份盐 */
    @GameTest(template = "empty")
    public static void vatJeiCountsMatchRecipe(GameTestHelper helper) {
        checkSaltFollowsMainIngredient(helper, VatRecipes.Kind.MEAT, "咸腊肉");
        checkSaltFollowsMainIngredient(helper, VatRecipes.Kind.SALTED_FISH, "咸鱼");
        helper.succeed();
    }

    private static void checkSaltFollowsMainIngredient(GameTestHelper helper, VatRecipes.Kind kind, String name) {
        VatRecipe recipe = VatRecipes.recipeOf(kind);
        int[] counts = recipe.displayedCounts(0);
        if (counts.length != 2 || counts[0] != counts[1]) {
            helper.fail(name + "展示的份数不对（主料 " + counts[0] + " / 盐 " + counts[1] + "）");
        }
    }

    /**
     * JEI 页面生成器本身：每条配方都出得来页面，页面上写的份数就是配方自检里那一份，
     * 而且没有"什么都产不出来"的空页。
     */
    @GameTest(template = "empty")
    public static void vatJeiPagesAreGenerated(GameTestHelper helper) {
        for (VatRecipe recipe : VatRecipes.all()) {
            List<VatJeiRecipe> pages = VatJeiRecipes.pagesFor(recipe);
            if (pages.isEmpty()) {
                helper.fail("配方 " + recipe.id() + " 在 JEI 里一页都没有");
            }
            for (VatJeiRecipe page : pages) {
                for (VatJeiRecipe.State state : page.states()) {
                    if (state.result().isEmpty() && !state.hasResultFluid()) {
                        helper.fail("配方 " + recipe.id() + " 有一页什么都不产（投料画了、产物没画）");
                    }
                }
            }
            checkPageFollowsIngredient(helper, recipe, pages, VatRecipes.Kind.MEAT);
            checkPageFollowsIngredient(helper, recipe, pages, VatRecipes.Kind.SALTED_FISH);
        }
        helper.succeed();
    }

    /** 抽查"盐数 = 主料数"：JEI 页面上主料格与盐格的份数必须相等 */
    private static void checkPageFollowsIngredient(GameTestHelper helper, VatRecipe recipe,
                                                   List<VatJeiRecipe> pages, VatRecipes.Kind kind) {
        if (recipe.kind() != kind) {
            return;
        }
        for (VatJeiRecipe page : pages) {
            for (VatJeiRecipe.State state : page.states()) {
                int main = shownCount(state.primary());
                int salt = shownCount(state.seasoning());
                if (main < 0 || salt != main) {
                    helper.fail("配方 " + recipe.id() + " 的 JEI 页面盐数(" + salt + ") 和主料(" + main + ") 不一样");
                }
            }
        }
    }

    /** 这一层第一格上写的份数（没画出来时返回 -1） */
    private static int shownCount(List<List<ItemStack>> layer) {
        if (layer.isEmpty() || layer.get(0).isEmpty()) {
            return -1;
        }
        return layer.get(0).get(0).getCount();
    }

    // ===== 二次发酵：做好的缸能就地接着酿 =====

    /** 大酱酿好、液体取空之后：加谷物 + 水 + 粗布毯要能接着酿醋 */
    @GameTest(template = "empty")
    public static void finishedPasteCanBrewVinegar(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        // 先把这一缸大酱"酿完"（走真实的完成流程）
        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < VatRecipes.PASTE_CHUNKS; i++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int i = 0; i < VatRecipes.PASTE_SALT; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setKind(VatRecipes.Kind.PASTE);
        VatBrewing.complete(level, pos, vat, VatRecipes.recipeOf(VatRecipes.Kind.PASTE));
        if (vat.residueCount() != VatRecipes.RESIDUE_COUNT) {
            helper.fail("大酱酿完缸里应该有 " + VatRecipes.RESIDUE_COUNT + " 份酱渣，实际 " + vat.residueCount());
        }
        // 玩家把大酱装空（10 碗）
        vat.setProductMb(VatRecipe.Fluid.PASTE, 0);

        // 这一步在修好之前会被 forInsert 拒掉：完成的缸被当成"正在发酵"
        if (VatBrewing.forInsert(vat, new ItemStack(ModItems.CORN_SEEDS.get())) == null) {
            helper.fail("酿完大酱的缸加不进谷物：二次发酵被 forInsert 挡掉了");
        }

        click(helper, player, new ItemStack(Items.WATER_BUCKET));
        for (int i = 0; i < VatRecipes.VINEGAR_GRAIN_COUNT; i++) {
            click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        }
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }
        click(helper, player, new ItemStack(cloth));

        if (VatBrewing.ready(vat) == null) {
            helper.fail("酱渣 + 谷物 + 水 + 粗布毯没有开工（配方也没认出来）");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    /** 泡菜腌完剩下的酸引水：加谷物 + 粗布毯要能接着酿白醋 */
    @GameTest(template = "empty")
    public static void pickleSourWaterCanBrewWhiteVinegar(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < 6; i++) {
            vat.addContent(new ItemStack(ModItems.NAPA_CABBAGE.get()));
        }
        for (int i = 0; i < 3; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setKind(VatRecipes.Kind.PICKLE);
        VatBrewing.complete(level, pos, vat, VatRecipes.recipeOf(VatRecipes.Kind.PICKLE));
        if (vat.sourWaterMb() < VatRecipes.SOUR_WATER_MB) {
            helper.fail("泡菜腌完应该有酸引水，实际 " + vat.sourWaterMb() + " mB");
        }
        // 玩家把菜取走（酸引水留在缸里）
        for (int i = 0; i < 6; i++) {
            VatBrewing.takeOne(vat, new java.util.ArrayList<>());
        }
        if (!vat.contents().isEmpty()) {
            helper.fail("菜没取干净，还剩 " + vat.contents().size() + " 份");
        }

        if (VatBrewing.forInsert(vat, new ItemStack(ModItems.CORN_SEEDS.get())) == null) {
            helper.fail("泡菜缸里的酸引水加不进谷物：二次发酵被挡掉了");
        }

        for (int i = 0; i < VatRecipes.VINEGAR_GRAIN_COUNT; i++) {
            click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        }
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }
        click(helper, player, new ItemStack(cloth));

        if (VatBrewing.ready(vat) == null) {
            helper.fail("酸引水 + 谷物 + 粗布毯没有开工（配方也没认出来）");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    // ===== 盖布的先后顺序 =====

    /** 先盖地毯、后倒水：倒完水就该开工（以前加水分支不会触发开工） */
    @GameTest(template = "empty")
    public static void waterAfterSealingStartsBrew(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        for (int i = 0; i < VatRecipes.PASTE_CHUNKS; i++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int i = 0; i < VatRecipes.PASTE_SALT; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setKind(VatRecipes.Kind.PASTE);
        // 先蒙羊毛地毯（大酱的封口物）
        click(helper, player, new ItemStack(Items.WHITE_CARPET));
        if (!vat.isCovered()) {
            helper.fail("羊毛地毯没能盖到缸上");
        }
        // 再倒三桶水：第三桶之后应该就开工了
        for (int i = 0; i < 3; i++) {
            click(helper, player, new ItemStack(Items.WATER_BUCKET));
        }
        if (vat.waterLayers() != 3) {
            helper.fail("三桶水应该正好三层，实际 " + vat.waterLayers() + " 层");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    /** 还没腌好的蒙盖缸：空手右键要把盖布还回来（不然只能砸缸） */
    @GameTest(template = "empty")
    public static void coverComesBackBeforeFermented(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        vat.setKind(VatRecipes.Kind.PASTE);
        vat.setCover(new ItemStack(Items.WHITE_CARPET));

        click(helper, player, ItemStack.EMPTY);
        if (vat.isCovered()) {
            helper.fail("没发酵的缸取不回盖布");
        }
        helper.succeed();
    }

    // ===== 小工具 =====

    private static VatBlockEntity placeVat(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.VAT.get());
        return (VatBlockEntity) helper.getBlockEntity(AT);
    }

    /** 拿某样东西右键大缸一下（走真实的交互入口） */
    private static void click(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.useBlock(AT, player);
    }

    private static void assertScheduled(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        if (!level.getBlockTicks().hasScheduledTick(pos, ModBlocks.VAT.get())) {
            helper.fail("条件齐了但没排上工序：这一缸不会自己往下走");
        }
    }

    /** 农夫乐事的粗布毯；没装农夫乐事时返回 null（酿醋那两条跳过） */
    private static Item clothRug() {
        Item rug = BuiltInRegistries.ITEM.get(
                DdIds.of("farmersdelight", "canvas_rug"));
        return rug == Items.AIR ? null : rug;
    }
}
