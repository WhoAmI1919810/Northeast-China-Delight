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
import mezz.jei.api.gui.builder.ITooltipBuilder;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 里的「大锅烹饪」分类，照用户给的示意图（main 目录下的 铁锅JEI示意.png）排：
 *
 * <ul>
 *   <li>左边是生着火、放着锅的大灶（游戏里的方块模型），配方要液体就在锅里画一层液面，和锅里实际的画法一样
 *       （{@code client/BigPotRenderer#renderLiquid}：流体贴图按流体染色、水不画成白的，配方里的调料往调料的颜色染）；
 *       灶下面一个火的小图标，和农夫乐事厨锅那页一个样子（自己重画的，见 textures/gui/jei/big_pot_heat.png）；</li>
 *   <li>上面一排 3 格调料，下面 2×3 格食材，调料那排往左错开半格（照示意图）；火的小图标在灶门正下方；</li>
 *   <li>排成三列（锅灶 + 火 / 调料 + 食材 / 箭头 + 容器 + 成品），三列上下都对齐、上下边距一样（2026-11-03 用户让我自己看着排）；</li>
 *   <li>箭头指向右边的大成品格（数量 = 一锅盛几份），箭头下面是盛菜用的容器；箭头按烹饪时间一遍遍走满（烹饪进度），
 *       烹饪时间和经验只在鼠标移到箭头上时在气泡里写（2026-11-03 用户要的）；</li>
 *   <li>锅盖缩短时间（{@code lid_speedup}）在 JEI 里不显示；</li>
 *   <li>3 格调料、6 格食材、容器格不管配方用没用都画出来，没用的空着（用户嫌格子忽多忽少难看）。</li>
 * </ul>
 */
public class BigPotRecipeCategory implements IRecipeCategory<BigPotJeiRecipe> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 66;
    private static final int SLOT = 16;
    private static final int STRIDE = 18;

    private static final float STOVE_X = 28.0F;
    private static final float STOVE_Y = 24.0F;
    private static final float STOVE_SCALE = 17.0F;
    private static final int HEAT_X = 30;
    private static final int HEAT_Y = 46;
    private static final int SEASONING_X = 53;
    private static final int SEASONING_Y = 4;
    private static final int GRID_X = 62;
    private static final int GRID_Y = 25;
    private static final int GRID_COLUMNS = 3;
    /** 调料、食材格数（配方格式的上限）；配方用不满也把格子都画出来，空着 */
    private static final int SEASONING_SLOTS = 3;
    private static final int INGREDIENT_SLOTS = 6;
    private static final int ARROW_X = 121;
    private static final int ARROW_Y = 27;
    private static final int CONTAINER_X = 125;
    private static final int CONTAINER_Y = 46;
    private static final int OUTPUT_FRAME_X = 148;
    private static final int OUTPUT_FRAME_Y = 29;
    private static final int OUTPUT_FRAME = 26;

    private static final ResourceLocation HEAT = DdIds.of(NortheastChinaDelight.MODID,
            "textures/gui/jei/big_pot_heat.png");

    private final IDrawable icon;
    private final IDrawable background;
    private final IGuiHelper guiHelper;
    /** 按烹饪时间走满的进度箭头，同一个时间的配方共用一个 */
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    public BigPotRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        //? if <1.20.5 {
        /*this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIG_IRON_POT.get()));*/
        //?} else {
        this.icon = guiHelper.createDrawableItemLike(ModItems.BIG_IRON_POT.get());
        //?}
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
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

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BigPotJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new SceneWidget(recipe, arrow(recipe)));
    }

    @Override
    public void draw(BigPotJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        //? if <1.20.5 {
        // 1.20.1 走老 draw：灶、火、箭头、大框都在这里画
        drawScene(graphics, recipe, arrow(recipe));
        //?}
    }

    /** 鼠标在箭头上：气泡里写烹饪时间（不盖锅盖时）和每盛一份给的经验 */
    @Override
    public void getTooltip(ITooltipBuilder tooltip, BigPotJeiRecipe recipe, IRecipeSlotsView view,
                           double mouseX, double mouseY) {
        IDrawable arrow = arrow(recipe);
        if (mouseX < ARROW_X || mouseX >= ARROW_X + arrow.getWidth()
                || mouseY < ARROW_Y || mouseY >= ARROW_Y + arrow.getHeight()) {
            return;
        }
        int ticks = recipe.cookingTime();
        String seconds = ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format("%.1f", ticks / 20.0F);
        tooltip.add(Component.translatable("jei.northeast_china_delight.big_pot.time", seconds));
        if (recipe.experience() > 0.0F) {
            float xp = recipe.experience();
            String text = xp == Math.floor(xp) ? String.valueOf((int) xp) : String.format("%.1f", xp);
            tooltip.add(Component.translatable("jei.northeast_china_delight.big_pot.experience", text));
        }
    }

    private IDrawableAnimated arrow(BigPotJeiRecipe recipe) {
        return this.arrows.computeIfAbsent(Math.max(1, recipe.cookingTime()), this.guiHelper::createAnimatedRecipeArrow);
    }

    private static void drawScene(GuiGraphics graphics, BigPotJeiRecipe recipe, IDrawable arrow) {
        drawStove(graphics, recipe);
        graphics.blit(HEAT, HEAT_X, HEAT_Y, 0.0F, 0.0F, 16, 16, 16, 16);
        arrow.draw(graphics, ARROW_X, ARROW_Y);
        drawFrame(graphics, OUTPUT_FRAME_X, OUTPUT_FRAME_Y, OUTPUT_FRAME, OUTPUT_FRAME);
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
            // 调料每格取第一样，各算一份
            List<ItemStack> seasonings = new ArrayList<>();
            for (List<ItemStack> slot : recipe.seasonings()) {
                if (!slot.isEmpty()) {
                    seasonings.add(slot.get(0).copyWithCount(1));
                }
            }
            BigPotRenderer.renderLiquid(pose, buffers, liquid, BigPotVisuals.liquidY(recipe.liquidMb()),
                    BigPotVisuals.liquidColor(liquid, seasonings), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
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

    private record SceneWidget(BigPotJeiRecipe recipe, IDrawable arrow) implements IRecipeWidget {
        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            drawScene(graphics, this.recipe, this.arrow);
        }
    }
}
