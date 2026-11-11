package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.chinese_regional_culinary_ingredients.block.WoodenMortarBlock;
import com.gunmu.chinese_regional_culinary_ingredients.client.WoodenMortarRenderer;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.PoundingRecipes;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.20.2 {
import net.minecraftforge.client.model.data.ModelData;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

/**
 * JEI 里的「舂捣」分类。左边逐项列出输入（数量直接写在物品角标上），中间是配方第一阶段模型，
 * 右上工具、次数与蓄力阈值，右边列出全部产物。概率、随机数量范围和副产物标记走槽位 tooltip。
 * 输入、产物两边至少画 2×2 格，多了按整行往下加；工具格、容器格也总是画出来。用不上的格子空着（用户嫌格子忽多忽少难看）。
 */
public class PoundRecipeCategory implements IRecipeCategory<PoundJeiRecipe> {

    private static final int SLOT = 16;
    private static final int STATION_SIZE = 32;
    private static final float STATION_SCALE = 20.0F;

    private static final int INPUT_X = 5;
    private static final int RESULTS_X = 134;
    private static final int GRID_COLUMNS = 2;
    private static final int GRID_STRIDE = 18;
    /** 输入 / 产物两边至少画几格（2×2） */
    private static final int MIN_GRID_SLOTS = 4;

    private static final int STATION_X = 46;
    private static final int STATION_Y = 46;
    private static final int TOOL_X = 84;
    private static final int TOOL_Y = 46;
    private static final int ARROW_X = 106;
    private static final int ARROW_Y = 54;
    private static final int TIMES_Y = 64;
    private static final int TEXT_COLOR = 0xFF3F3F3F;

    private static final int WIDTH = 176;
    /** 2 列 × 7 行，够放 14 项输入或产物；更长的配方请在数据包里拆成多条。 */
    private static final int HEIGHT = 124;

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable background;

    public PoundRecipeCategory(IGuiHelper guiHelper) {
        this.icon = new StationIcon();
        //? if <1.20.5 {
        this.arrow = guiHelper.drawableBuilder(
                DdIds.of(NortheastChinaDelight.MODID, "textures/gui/jei_widgets.png"), 61, 93, 24, 16)
                .build();
        //?} else {
        /*this.arrow = guiHelper.getRecipeArrow();
        *///?}
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<PoundJeiRecipe> getRecipeType() {
        return NortheastJeiPlugin.POUND_TYPE;
    }

    @Override
    public ResourceLocation getRegistryName(PoundJeiRecipe recipe) {
        return recipe.id();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.northeast_china_delight.pound");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    //? if <1.20.5 {
    @Override
    public IDrawable getBackground() {
        return this.background;
    }
    //?}
    //? if >=1.20.5 {
    /*@Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }
    *///?}

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PoundJeiRecipe recipe, IFocusGroup focuses) {
        int inputSlots = gridSlots(recipe.inputs().size());
        for (int i = 0; i < inputSlots; i++) {
            if (i >= recipe.inputs().size()) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, gridX(INPUT_X, i), gridY(i, inputSlots))
                        .setStandardSlotBackground();
                continue;
            }
            PoundJeiRecipe.Input input = recipe.inputs().get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, gridX(INPUT_X, i), gridY(i, inputSlots))
                    .setStandardSlotBackground()
                    .addItemStacks(input.items());
        }

        if (recipe.tool().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, TOOL_X, TOOL_Y)
                    .setStandardSlotBackground();
        } else {
            builder.addSlot(RecipeIngredientRole.CATALYST, TOOL_X, TOOL_Y)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.tool());
        }

        if (recipe.resultContainer() == null) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, ARROW_X + 4, ARROW_Y + 18)
                    .setStandardSlotBackground();
        } else {
            // 取成品要拿着的容器放在箭头下面
            builder.addSlot(RecipeIngredientRole.INPUT, ARROW_X + 4, ARROW_Y + 18)
                    .setStandardSlotBackground()
                    .addItemStack(recipe.resultContainer())
                    .addTooltipCallback((slotView, tooltip) -> tooltip.add(
                            Component.translatable("jei.northeast_china_delight.pound.result_container")));
        }

        int outputSlots = gridSlots(recipe.outputs().size());
        for (int i = recipe.outputs().size(); i < outputSlots; i++) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, gridX(RESULTS_X, i), gridY(i, outputSlots))
                    .setStandardSlotBackground();
        }
        for (int i = 0; i < recipe.outputs().size(); i++) {
            PoundJeiRecipe.Output output = recipe.outputs().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, gridX(RESULTS_X, i), gridY(i, outputSlots))
                    .setStandardSlotBackground()
                    .addItemStack(output.stack())
                    .addTooltipCallback((slotView, tooltip) -> {
                        if (output.pooled()) {
                            tooltip.add(Component.translatable("jei.northeast_china_delight.pound.pool",
                                    Math.round(output.weight() * 100.0F
                                            / Math.max(1, output.poolTotalWeight()))));
                            if (output.poolChance() < 1.0F) {
                                tooltip.add(Component.translatable(
                                        "jei.northeast_china_delight.pound.pool_chance",
                                        Math.round(output.poolChance() * 100.0F)));
                            }
                        } else if (output.chance() < 1.0F) {
                            tooltip.add(Component.translatable("jei.northeast_china_delight.pound.chance",
                                    Math.round(output.chance() * 100.0F)));
                        }
                        if (output.minCount() != output.maxCount()) {
                            tooltip.add(Component.translatable("jei.northeast_china_delight.pound.count_range",
                                    output.minCount(), output.maxCount()));
                        }
                        if (recipe.resultMb() > 0) {
                            tooltip.add(Component.translatable("jei.northeast_china_delight.pound.result_mb",
                                    recipe.resultMb()));
                        }
                        if (output.byproduct()) {
                            tooltip.add(Component.translatable("jei.northeast_china_delight.pound.byproduct"));
                        }
                    });
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, PoundJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new StationWidget(recipe));
        builder.addWidget(new TimesWidget(this.arrow, recipe.poundTimes(), recipe.chargeThreshold()));
        builder.addWidget(new ContainerWidget(recipe));
    }

    @Override
    public void draw(PoundJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        //? if <1.20.5 {
        // 1.20.1 走老 draw：站台、箭头、小字都在这里画
        drawStation(graphics, recipe, STATION_X + STATION_SIZE / 2.0F,
                STATION_Y + STATION_SIZE / 2.0F, STATION_SCALE);
        this.arrow.draw(graphics, ARROW_X, ARROW_Y);
        drawTimes(graphics, recipe.poundTimes(), recipe.chargeThreshold());
        drawContainerHint(graphics, recipe);
        //?}
    }

    /** 一边要画几格：至少 2×2，多了补成整行 */
    private static int gridSlots(int count) {
        int rows = (Math.max(count, MIN_GRID_SLOTS) + GRID_COLUMNS - 1) / GRID_COLUMNS;
        return rows * GRID_COLUMNS;
    }

    private static int gridX(int start, int index) {
        return start + (index % GRID_COLUMNS) * GRID_STRIDE;
    }

    private static int gridY(int index, int count) {
        int rows = Math.max(1, (count + GRID_COLUMNS - 1) / GRID_COLUMNS);
        int top = (HEIGHT - rows * SLOT - (rows - 1) * (GRID_STRIDE - SLOT)) / 2;
        return top + (index / GRID_COLUMNS) * GRID_STRIDE;
    }

    private static void drawTimes(GuiGraphics graphics, int poundTimes, float chargeThreshold) {
        Font font = Minecraft.getInstance().font;
        PoseStack pose = graphics.pose();
        Component text = Component.translatable("jei.northeast_china_delight.pound.times", poundTimes);
        float scale = 0.65F;
        float x = TOOL_X + SLOT / 2.0F - font.width(text) * scale / 2.0F;
        pose.pushPose();
        pose.translate(x, TIMES_Y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, TEXT_COLOR, false);
        pose.popPose();

        if (Math.abs(chargeThreshold - WoodenMortarBlock.POUND_CHARGE_THRESHOLD) > 0.0001F) {
            Component charge = Component.translatable("jei.northeast_china_delight.pound.charge",
                    Math.round(chargeThreshold * 100.0F));
            float chargeX = TOOL_X + SLOT / 2.0F - font.width(charge) * scale / 2.0F;
            pose.pushPose();
            pose.translate(chargeX, TIMES_Y + 9.0F, 0.0F);
            pose.scale(scale, scale, 1.0F);
            graphics.drawString(font, charge, 0, 0, TEXT_COLOR, false);
            pose.popPose();
        }
    }

    /** 有 result_container / result_mb 时，把「容器」写在容器格下面，每次装出的毫升数写在产物格下面。 */
    private static void drawContainerHint(GuiGraphics graphics, PoundJeiRecipe recipe) {
        if (recipe.resultContainer() == null && recipe.resultMb() <= 0) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        PoseStack pose = graphics.pose();
        float scale = 0.65F;

        if (recipe.resultContainer() != null) {
            Component label = Component.translatable("jei.northeast_china_delight.pound.container");
            float x = ARROW_X + 4 + SLOT / 2.0F - font.width(label) * scale / 2.0F;
            pose.pushPose();
            pose.translate(x, ARROW_Y + 18 + SLOT + 2.0F, 0.0F);
            pose.scale(scale, scale, 1.0F);
            graphics.drawString(font, label, 0, 0, TEXT_COLOR, false);
            pose.popPose();
        }

        if (recipe.resultMb() > 0) {
            Component amount = Component.translatable(
                    "jei.northeast_china_delight.pound.result_mb_value", recipe.resultMb());
            float x = RESULTS_X + SLOT / 2.0F - font.width(amount) * scale / 2.0F;
            // 产物格至少两排：写在最下面一排的下面
            int slots = gridSlots(recipe.outputs().size());
            float y = gridY(slots - 1, slots) + SLOT + 2.0F;
            pose.pushPose();
            pose.translate(x, y, 0.0F);
            pose.scale(scale, scale, 1.0F);
            graphics.drawString(font, amount, 0, 0, TEXT_COLOR, false);
            pose.popPose();
        }
    }

    private static void drawStation(GuiGraphics graphics, PoundJeiRecipe recipe,
                                    float centerX, float centerY, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = ModBlocks.WOODEN_MORTAR.get().defaultBlockState()
                .setValue(WoodenMortarBlock.FACING, Direction.SOUTH)
                .setValue(WoodenMortarBlock.STATE, 1);

        boolean overlay = recipe.stageModelMode() == PoundingRecipes.StageModelMode.OVERLAY;
        BakedModel model = null;
        if (!recipe.stageModels().isEmpty()) {
            model = WoodenMortarRenderer.bakedModel(recipe.stageModels().get(0));
        }
        if (model == minecraft.getModelManager().getMissingModel()) {
            model = null;
        }

        PoseStack pose = graphics.pose();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        pose.pushPose();
        pose.translate(centerX, centerY, 100.0F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        pose.translate(-0.5F, -0.5F, -0.5F);

        if (overlay) {
            // 内容物模型不带臼身：先画空木臼，再叠内容物（缺内容物时只剩空臼）
            WoodenMortarRenderer.renderBakedModel(pose, buffers, state,
                    WoodenMortarRenderer.bakedModel(WoodenMortarRenderer.EMPTY_MORTAR_MODEL),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            if (model != null) {
                WoodenMortarRenderer.renderBakedModel(pose, buffers, state, model,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
        } else if (model != null) {
            WoodenMortarRenderer.renderBakedModel(pose, buffers, state, model,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } else {
            minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
        }
        if (recipe.liquid() != null) {
            // 倒进臼里的液体不在模型里，按流体 / 贴图画一层液面（没有世界，水用原版默认水色）
            WoodenMortarRenderer.renderLiquid(pose, buffers, null, null, recipe.liquid(), LightTexture.FULL_BRIGHT);
        }
        buffers.endBatch();
        pose.popPose();
    }

    private record StationWidget(PoundJeiRecipe recipe) implements IRecipeWidget {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            drawStation(graphics, this.recipe, STATION_X + STATION_SIZE / 2.0F,
                    STATION_Y + STATION_SIZE / 2.0F, STATION_SCALE);
        }
    }

    private record TimesWidget(IDrawable arrow, int poundTimes, float chargeThreshold) implements IRecipeWidget {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            this.arrow.draw(graphics, ARROW_X, ARROW_Y);
            drawTimes(graphics, this.poundTimes, this.chargeThreshold);
        }
    }

    private record ContainerWidget(PoundJeiRecipe recipe) implements IRecipeWidget {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            drawContainerHint(graphics, this.recipe);
        }
    }

    /** 左上角 16x16 的分类图标：缩小画一台装料的木臼 */
    private static final class StationIcon implements IDrawable {
        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            PoundJeiRecipe empty = new PoundJeiRecipe(DdIds.of(NortheastChinaDelight.MODID, "pounding/icon"),
                    java.util.List.of(), java.util.List.of(), 1, 0.9F, java.util.List.of(),
                    java.util.List.of(DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_1")),
                    PoundingRecipes.StageModelMode.FULL, null, 0, null);
            drawStation(graphics, empty, xOffset + 8.0F, yOffset + 10.0F, 9.0F);
        }
    }
}
