package com.gunmu.dongbei_delight.compat.jei;

import com.gunmu.dongbei_delight.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 里的「烧烤」分类。
 *
 * 一行摆开：最左边是食材，接着是要刷的调料（一样一格），箭头右边是烤好的成品，
 * 烤制时长写在箭头下面。和「大缸」分类一样不带背景图，尺寸自己给。
 */
public class GrillRecipeCategory implements IRecipeCategory<GrillJeiRecipe> {

    private static final int SLOT = 16;
    /** 槽位间距（16 + 4） */
    private static final int STEP = 20;
    private static final int ROW_Y = 13;
    /** 最多展示几种调料 */
    private static final int MAX_SEASONINGS = 3;

    private static final int INPUT_X = 6;
    private static final int SEASONING_X = INPUT_X + STEP;
    private static final int ARROW_X = SEASONING_X + MAX_SEASONINGS * STEP;
    private static final int ARROW_Y = ROW_Y;
    private static final int RESULT_X = ARROW_X + 24;
    private static final int TIME_Y = ROW_Y + 22;
    private static final int TIME_COLOR = 0xFF3F3F3F;

    private static final int WIDTH = RESULT_X + SLOT + 6;
    private static final int HEIGHT = 46;

    private final IDrawable icon;
    private final IDrawable arrow;

    public GrillRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModItems.GRILL_RACK.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<GrillJeiRecipe> getRecipeType() {
        return DongbeiJeiPlugin.GRILL_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.dongbei_delight.grill");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    /** 没有背景图，尺寸得自己给 */
    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GrillJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, ROW_Y)
                .addItemStacks(recipe.ingredient())
                .setStandardSlotBackground();

        List<ItemStack> seasonings = recipe.seasonings();
        for (int i = 0; i < seasonings.size() && i < MAX_SEASONINGS; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, SEASONING_X + i * STEP, ROW_Y)
                    .addItemStack(seasonings.get(i))
                    .setStandardSlotBackground();
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, ROW_Y)
                .addItemStack(recipe.result())
                .setStandardSlotBackground();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GrillJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new TimeWidget(this.arrow, recipe.seconds()));
    }

    /** 画箭头和箭头下面的烤制时长 */
    private record TimeWidget(IDrawable arrow, int seconds) implements IRecipeWidget {

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            this.arrow.draw(graphics, ARROW_X, ARROW_Y);
            Component text = timeText(this.seconds);
            Font font = Minecraft.getInstance().font;
            int x = ARROW_X + (this.arrow.getWidth() - font.width(text)) / 2;
            graphics.drawString(font, text, Math.max(0, x), TIME_Y, TIME_COLOR, false);
        }
    }

    /** 烤制时长：不足一分钟写「N 秒」，整分钟写「N 分钟」，其余写「N 分 M 秒」 */
    private static Component timeText(int seconds) {
        if (seconds <= 0) {
            return Component.empty();
        }
        if (seconds < 60) {
            return Component.translatable("jei.dongbei_delight.time.seconds", seconds);
        }
        if (seconds % 60 == 0) {
            return Component.translatable("jei.dongbei_delight.time.minutes", seconds / 60);
        }
        return Component.translatable("jei.dongbei_delight.time.minutes_seconds", seconds / 60, seconds % 60);
    }
}
