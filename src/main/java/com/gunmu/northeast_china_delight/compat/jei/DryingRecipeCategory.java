// 这个分类完全靠机械动力的模型（AllPartialModels）+ catnip 的 GUI 元素渲染，
// 1.20.4 节点没有机械动力，整个文件停用（只留注释）。
//? if >=1.20.5 {
package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI「批量风干」分类 —— 照抄 Create {@code ProcessingViaFanCategory}，
 * 区别只在没有底部催化剂方块（水/火/灵魂火）和白色阴影。
 * 风扇渲染直接调用 catnip {@link GuiGameElement#of}，就是
 * {@code AnimatedKinetics} 内部的真实调用。
 */
public class DryingRecipeCategory implements IRecipeCategory<DryingJeiRecipe> {

    private static final int SLOT = 16;
    private static final int INPUT_X = 21, INPUT_Y = 48;
    private static final int OUTPUT_X = 141, OUTPUT_Y = 48;
    private static final int ARROW_X = 54, ARROW_Y = 51;

    // 风扇位置：页面正中偏左（Create 是 (56, 33)，但我们没有 attached block，居中摆）
    // 风扇视觉中心想在页面的水平正中（177/2≈88）。
    // 但 GuiGameElement 经 22.5 度 Y 旋转后，渲染出的投影中心比 translate 锚点偏右约 22px，
    // 所以 translate 用 FAN_VCX-22 才能把风扇真正摆到正中。
    private static final int FAN_VCX = 88;
    private static final int FAN_X = FAN_VCX - 14;
    private static final int FAN_Y = 30;
    private static final int FAN_SCALE = 24;

    // 第二个输出与主输出并排
    private static final int OUTPUT2_X = OUTPUT_X + SLOT + 2;
    private static final int OUTPUT2_Y = OUTPUT_Y;

    private static final int WIDTH = 177;
    private static final int HEIGHT = 80;

    private static final ResourceLocation WIDGETS = DdIds.of(
            NortheastChinaDelight.MODID, "textures/gui/jei_widgets.png");
    private static final int LARROW_U = 19, LARROW_V = 0, LARROW_W = 71, LARROW_H = 10;
    // Create JEI_SHADOW：白色椭圆光斑，画在风扇正下方
    private static final int SHADOW_U = 0, SHADOW_V = 56, SHADOW_W = 52, SHADOW_H = 11;

    private final IDrawable icon;

    public DryingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(
                AllBlocks.ENCASED_FAN.get()));
    }

    @Override public RecipeType<DryingJeiRecipe> getRecipeType() { return NortheastJeiPlugin.DRYING_TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.northeast_china_delight.drying"); }
    @Override public IDrawable getIcon() { return this.icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DryingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addItemStacks(recipe.ingredient()).setStandardSlotBackground();
        var results = recipe.results();
        if (!results.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                    .addItemStack(results.get(0)).setStandardSlotBackground();
        }
        if (results.size() > 1) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT2_X, OUTPUT2_Y)
                    .addItemStack(results.get(1)).setStandardSlotBackground();
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, DryingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new DryingWidget());
    }

    private static void blitWidget(GuiGraphics g, int u, int v, int w, int h, int x, int y) {
        g.blit(WIDGETS, x, y, u, v, w, h, 256, 256);
    }
    private static final class DryingWidget implements IRecipeWidget {
        private int ticks;

        @Override public ScreenPosition getPosition() { return new ScreenPosition(0, 0); }
        @Override public void tick() { this.ticks++; }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {

            // 长箭头
            blitWidget(graphics, LARROW_U, LARROW_V, LARROW_W, LARROW_H, ARROW_X, ARROW_Y);
            // 风扇正下方的白色椭圆阴影（Create JEI_SHADOW）
            blitWidget(graphics, SHADOW_U, SHADOW_V, SHADOW_W, SHADOW_H,
                    FAN_VCX - SHADOW_W / 2, FAN_Y + 14);

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(FAN_X, FAN_Y, 100.0F);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-12.5F));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(22.5F));

            float angle = this.ticks * 4.0F;
            GuiGameElement.of(AllPartialModels.ENCASED_FAN_INNER)
                    .rotateBlock(0, 180, angle * 16.0F)
                    .scale(FAN_SCALE)
                    .render(graphics);
            GuiGameElement.of(AllBlocks.ENCASED_FAN.getDefaultState())
                    .rotateBlock(0, 180, 0)
                    .scale(FAN_SCALE)
                    .render(graphics);

            pose.popPose();
        }
    }
}
//?}
