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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 的「大锅烹饪」分类：左侧显示大灶和锅内液体，上方 3 格调料，中间 2×3 格食材，
 * 右侧显示成品、容器与烹饪进度。未使用的槽位也保持显示，避免不同配方的页面布局跳动。
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
    /** 箭头图底部带一行 JEI 底色，上移一格以免压住容器格的上边框。 */
    private static final int ARROW_Y = 33;
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
     * 锅口那圈在 JEI 里的位置：按 drawStove 的变换把锅沿（锅心 16,16、半径 13.75 像素、高 17.25）投到界面上，
     * 是个中心 (29, 32.5)、半宽 14.6、半高 7.3 的椭圆；各放宽一点，鼠标在这个椭圆里就算移到锅上
     */
    private static final double POT_CENTER_X = 29.0;
    private static final double POT_CENTER_Y = 32.5;
    private static final double POT_HALF_WIDTH = 15.0;
    private static final double POT_HALF_HEIGHT = 8.0;

    /** 图标沿用农夫乐事厨锅分类；图块自带 JEI 底色，因此篝火先画、锅灶后画。 */
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
        /*this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIG_IRON_POT.get()));*/
        //?} else {
        this.icon = guiHelper.createDrawableItemLike(ModItems.BIG_IRON_POT.get());
        //?}
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
    /*@Override
    public IDrawable getBackground() {
        return this.background;
    }
    *///?}
    //? if >=1.20.5 {
    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }
    //?}

    private IRecipeSlotBuilder withSlotBackground(IRecipeSlotBuilder slot) {
        //? if <1.20.5 {
        /*return slot.setBackground(this.guiHelper.getSlotDrawable(), -1, -1);*/
        //?} else {
        return slot.setStandardSlotBackground();
        //?}
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

    /** 鼠标悬停在锅口时显示所需液体和数量；沿用两版 JEI 都支持的 getTooltipStrings。 */
    @Override
    public List<Component> getTooltipStrings(BigPotJeiRecipe recipe, IRecipeSlotsView view,
                                             double mouseX, double mouseY) {
        double dx = (mouseX - POT_CENTER_X) / POT_HALF_WIDTH;
        double dy = (mouseY - POT_CENTER_Y) / POT_HALF_HEIGHT;
        if (dx * dx + dy * dy > 1.0) {
            return List.of();
        }
        if (recipe.liquidMb() <= 0) {
            return List.of(Component.translatable("jei.northeast_china_delight.big_pot.liquid_none"));
        }
        List<Component> lines = new ArrayList<>();
        String tag = recipe.liquidTag() == null ? null : "#" + recipe.liquidTag();
        MutableComponent names = Component.empty();
        if (recipe.liquids().isEmpty() && tag != null) {
            names.append(tag);
        }
        for (int i = 0; i < recipe.liquids().size(); i++) {
            if (i > 0) {
                names.append(Component.translatable("jei.northeast_china_delight.big_pot.liquid_separator"));
            }
            names.append(recipe.liquids().get(i).getFluidType().getDescription());
        }
        lines.add(Component.translatable("jei.northeast_china_delight.big_pot.liquid", names));
        if (tag != null && !recipe.liquids().isEmpty()) {
            lines.add(Component.translatable("jei.northeast_china_delight.big_pot.liquid_tag", tag)
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(amount(recipe.liquidMb(), recipe.liquidMaxMb()));
        return lines;
    }

    /** 用量那行：正好 / 至少 / 最多 / 范围（配方里的大于、小于已经换成整数毫升的大于等于、小于等于） */
    private static Component amount(int min, int max) {
        String prefix = "jei.northeast_china_delight.big_pot.liquid_";
        if (min == max) {
            return Component.translatable(prefix + "exact", min);
        }
        if (max == Integer.MAX_VALUE) {
            return Component.translatable(prefix + "at_least", min);
        }
        if (min <= 1) {
            return Component.translatable(prefix + "at_most", max);
        }
        return Component.translatable(prefix + "between", min, max);
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
            // JEI 显示未调色的原始液体颜色。
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
