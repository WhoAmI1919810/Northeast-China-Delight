package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.client.BigPotRenderer;
import com.gunmu.northeast_china_delight.client.BigPotVisuals;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 里的「大锅烹饪」分类，照用户给的示意图（main 目录下的 铁锅JEI示意.png，2026-11-03 用户改过一版）排：
 *
 * <ul>
 *   <li>左边是生着火、放着锅的大灶（游戏里的方块模型），配方要液体就在锅里画一层液面，和锅里实际的画法一样
 *       （{@code client/BigPotRenderer#renderLiquid}），只是不按调料染：JEI 里是刚倒进去的清水，水就是普通的蓝；
 *       灶的右下角挨着农夫乐事的篝火小图标；</li>
 *   <li>上面一排 3 格调料，比下面 2×3 格食材往左错开一格半；</li>
 *   <li>右上角两行：秒表「10秒」、经验球「0.35点经验」（直接写出来，图标见 work/tools/big_stove/jei_icons.py）；</li>
 *   <li>箭头指向右边的大成品格（数量 = 一锅盛几份），按烹饪时间一遍遍走满（烹饪进度）；箭头下面是盛菜用的容器，
 *       右边一把勺子勾向成品；</li>
 *   <li>锅盖缩短时间（{@code lid_speedup}）在 JEI 里不显示；</li>
 *   <li>3 格调料、6 格食材、容器格不管配方用没用都画出来，没用的空着（用户嫌格子忽多忽少难看）。</li>
 * </ul>
 */
public class BigPotRecipeCategory implements IRecipeCategory<BigPotJeiRecipe> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 70;
    private static final int SLOT = 16;
    private static final int STRIDE = 18;

    private static final float STOVE_X = 29.0F;
    private static final float STOVE_Y = 41.0F;
    private static final float STOVE_SCALE = 17.0F;
    private static final int HEAT_X = 46;
    private static final int HEAT_Y = 53;
    private static final int SEASONING_X = 40;
    private static final int SEASONING_Y = 5;
    private static final int GRID_X = 65;
    private static final int GRID_Y = 25;
    private static final int GRID_COLUMNS = 3;
    /** 调料、食材格数（配方格式的上限）；配方用不满也把格子都画出来，空着 */
    private static final int SEASONING_SLOTS = 3;
    private static final int INGREDIENT_SLOTS = 6;
    private static final int ARROW_X = 120;
    private static final int ARROW_Y = 34;
    private static final int CONTAINER_X = 124;
    private static final int CONTAINER_Y = 51;
    private static final int OUTPUT_FRAME_X = 146;
    private static final int OUTPUT_FRAME_Y = 29;
    private static final int OUTPUT_FRAME = 26;
    /** 右上角：秒表 + 时间、经验球 + 经验（图标左边对齐，字跟在图标右边） */
    private static final int INFO_ICON_X = 120;
    private static final int TIME_Y = 5;
    private static final int XP_Y = 18;
    private static final int INFO_TEXT_X = 131;
    private static final int TEXT_COLOR = 0xFF808080;
    /** 字缩小一点：「0.35点经验」原大 47 像素，从 131 写过去会出右边界 */
    private static final float TEXT_SCALE = 0.85F;
    /** 容器格右边一把勺子，勾向成品（勺子那块图连着一圈 JEI 底色，放在成品框下面，不压框） */
    private static final int LADLE_X = 141;
    private static final int LADLE_Y = 55;

    /**
     * 篝火、箭头（灰底 + 白色走满）、秒表、经验球、勺子都直接用农夫乐事厨锅那页的图（2026-11-03 用户：照参考图用农夫乐事的图案）。
     * 位置照农夫乐事 CookingRecipeCategory：界面贴图 176,0 篝火 17×15、176,15 白箭头 24×17、176,32 秒表 8×11、176,43 经验球 9×9；
     * JEI 背景图 60,9 灰箭头 24×17、82,40 勺子 9×14。这些图块自带一圈和 JEI 底色一样的灰（#C6C6C6），所以篝火先画、锅灶压在上面。
     */
    private static final ResourceLocation FD_GUI = DdIds.of("farmersdelight", "textures/gui/cooking_pot.png");
    private static final ResourceLocation FD_JEI = DdIds.of("farmersdelight", "textures/gui/jei/cooking_pot.png");

    private final IDrawable icon;
    private final IDrawable background;
    private final IGuiHelper guiHelper;
    private final IDrawable heat;
    private final IDrawable timeIcon;
    private final IDrawable expIcon;
    private final IDrawable ladle;
    private final IDrawable arrowBackground;
    /** 按烹饪时间走满的白箭头，同一个时间的配方共用一个 */
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public BigPotRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        //? if <1.20.5 {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIG_IRON_POT.get()));
        //?} else {
        /*this.icon = guiHelper.createDrawableItemLike(ModItems.BIG_IRON_POT.get());
        *///?}
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.heat = guiHelper.createDrawable(FD_GUI, 176, 0, 17, 15);
        this.timeIcon = guiHelper.createDrawable(FD_GUI, 176, 32, 8, 11);
        this.expIcon = guiHelper.createDrawable(FD_GUI, 176, 43, 9, 9);
        this.ladle = guiHelper.createDrawable(FD_JEI, 82, 40, 9, 14);
        this.arrowBackground = guiHelper.createDrawable(FD_JEI, 60, 9, 24, 17);
    }

    @Override
    public RecipeType<BigPotJeiRecipe> getRecipeType() {
        return NortheastJeiPlugin.BIG_POT_TYPE;
    }

    @Override
    public ResourceLocation getRegistryName(BigPotJeiRecipe recipe) {
        return recipe.id();
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.northeast_china_delight.big_pot_cooking");
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

    private IRecipeSlotBuilder withSlotBackground(IRecipeSlotBuilder slot) {
        //? if <1.20.5 {
        return slot.setBackground(this.guiHelper.getSlotDrawable(), -1, -1);
        //?} else {
        /*return slot.setStandardSlotBackground();
        *///?}
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BigPotJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < SEASONING_SLOTS; i++) {
            int x = SEASONING_X + i * STRIDE;
            if (i >= recipe.seasonings().size()) {
                withSlotBackground(builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, SEASONING_Y));
                continue;
            }
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, x, SEASONING_Y)
                    .addItemStacks(recipe.seasonings().get(i))
                    .addTooltipCallback((slotView, tooltip) -> tooltip.add(
                            Component.translatable("jei.northeast_china_delight.big_pot.seasoning"))));
        }
        for (int i = 0; i < Math.max(INGREDIENT_SLOTS, recipe.ingredients().size()); i++) {
            int x = GRID_X + (i % GRID_COLUMNS) * STRIDE;
            int y = GRID_Y + (i / GRID_COLUMNS) * STRIDE;
            if (i >= recipe.ingredients().size()) {
                withSlotBackground(builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, y));
                continue;
            }
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStacks(recipe.ingredients().get(i)));
        }
        if (recipe.container().isEmpty()) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.RENDER_ONLY, CONTAINER_X, CONTAINER_Y));
        } else {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, CONTAINER_X, CONTAINER_Y)
                    .addItemStack(recipe.container())
                    .addTooltipCallback((slotView, tooltip) -> tooltip.add(
                            Component.translatable("jei.northeast_china_delight.big_pot.container"))));
        }
        // 成品格自己画一个大框（drawScene），不用标准槽背景
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_FRAME_X + (OUTPUT_FRAME - SLOT) / 2,
                        OUTPUT_FRAME_Y + (OUTPUT_FRAME - SLOT) / 2)
                .addItemStack(recipe.result())
                .addTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "jei.northeast_china_delight.big_pot.servings", recipe.result().getCount())));
        // 锅里的液体只画在锅里；挂成看不见的原料，JEI 里查这种液体的用途能查到这页
        for (Fluid fluid : recipe.liquids()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(fluid, recipe.liquidMb());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BigPotJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new SceneWidget(this, recipe));
    }

    @Override
    public void draw(BigPotJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        //? if <1.20.5 {
        // 1.20.1 走老 draw：灶、火、箭头、大框都在这里画
        drawScene(graphics, recipe);
        //?}
    }

    /** 农夫乐事厨锅那页的白箭头，按这条配方的烹饪时间走满一遍 */
    private IDrawableAnimated arrow(BigPotJeiRecipe recipe) {
        return this.arrows.computeIfAbsent(Math.max(1, recipe.cookingTime()), ticks -> this.guiHelper
                .drawableBuilder(FD_GUI, 176, 15, 24, 17)
                .buildAnimated(ticks, IDrawableAnimated.StartDirection.LEFT, false));
    }

    private void drawScene(GuiGraphics graphics, BigPotJeiRecipe recipe) {
        // 篝火先画：它那块图带一圈 JEI 底色，锅灶画在上面把那圈灰盖住
        this.heat.draw(graphics, HEAT_X, HEAT_Y);
        drawStove(graphics, recipe);
        this.arrowBackground.draw(graphics, ARROW_X, ARROW_Y);
        arrow(recipe).draw(graphics, ARROW_X, ARROW_Y);
        drawFrame(graphics, OUTPUT_FRAME_X, OUTPUT_FRAME_Y, OUTPUT_FRAME, OUTPUT_FRAME);
        this.ladle.draw(graphics, LADLE_X, LADLE_Y);
        drawInfo(graphics, recipe);
    }

    /** 右上角两行：秒表「10秒」，经验球「0.35点经验」（没经验就不写那行） */
    private void drawInfo(GuiGraphics graphics, BigPotJeiRecipe recipe) {
        Font font = Minecraft.getInstance().font;
        int ticks = recipe.cookingTime();
        String seconds = ticks % 20 == 0 ? String.valueOf(ticks / 20) : trim(ticks / 20.0F);
        this.timeIcon.draw(graphics, INFO_ICON_X, TIME_Y);
        drawSmall(graphics, font, Component.translatable("jei.northeast_china_delight.big_pot.time", seconds),
                TIME_Y + 2);
        if (recipe.experience() > 0.0F) {
            this.expIcon.draw(graphics, INFO_ICON_X, XP_Y + 1);
            drawSmall(graphics, font, Component.translatable("jei.northeast_china_delight.big_pot.experience",
                    trim(recipe.experience())), XP_Y + 2);
        }
    }

    private static void drawSmall(GuiGraphics graphics, Font font, Component text, int y) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(INFO_TEXT_X, y, 0.0F);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.drawString(font, text, 0, 0, TEXT_COLOR, false);
        pose.popPose();
    }

    /** 小数去掉末尾的 0：1.0 → 1，0.35 → 0.35 */
    private static String trim(float value) {
        String text = String.format(java.util.Locale.ROOT, "%.2f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    /** 生着火、放着锅的大灶（朝北，四格照 facing=north 的位置摆），配方要液体就叠一层液面 */
    private static void drawStove(GuiGraphics graphics, BigPotJeiRecipe recipe) {
        Fluid liquid = recipe.liquids().isEmpty() ? null : recipe.liquids().get(0);
        Minecraft minecraft = Minecraft.getInstance();
        BlockState base = ModBlocks.BIG_STOVE.get().defaultBlockState()
                .setValue(BigStoveBlock.FACING, Direction.NORTH)
                .setValue(BigStoveBlock.LIT, true)
                .setValue(BigStoveBlock.POT, BigStoveBlock.Pot.POT);
        PoseStack pose = graphics.pose();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        pose.pushPose();
        pose.translate(STOVE_X, STOVE_Y, 100.0F);
        pose.scale(STOVE_SCALE, -STOVE_SCALE, STOVE_SCALE);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        pose.translate(-1.0F, -0.5F, -1.0F);
        BigStoveBlock.Part[] parts = {BigStoveBlock.Part.NW, BigStoveBlock.Part.NE, BigStoveBlock.Part.SW, BigStoveBlock.Part.SE};
        int[][] offsets = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        for (int i = 0; i < parts.length; i++) {
            pose.pushPose();
            pose.translate(offsets[i][0], 0.0F, offsets[i][1]);
            // 不指定渲染类型：多部件模型里灶（cutout）、锅（实心）、火各按各的画
            minecraft.getBlockRenderer().renderSingleBlock(base.setValue(BigStoveBlock.PART, parts[i]), pose, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        if (liquid != null) {
            // JEI 里画的是刚倒进去的清水（流体本来的颜色，水是普通的蓝），不按调料染（2026-11-03 用户）
            BigPotRenderer.renderLiquid(pose, buffers, liquid, BigPotVisuals.liquidY(recipe.liquidMb()),
                    BigPotVisuals.liquidColor(liquid, List.of()), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        buffers.endBatch();
        pose.popPose();
    }

    /** 和 JEI 的槽一个画法：左上深、右下白、中间灰 */
    private static void drawFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        graphics.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    private record SceneWidget(BigPotRecipeCategory category, BigPotJeiRecipe recipe) implements IRecipeWidget {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            this.category.drawScene(graphics, this.recipe);
        }
    }
}
