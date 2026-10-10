package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.client.GrillRackGeometry;
import com.gunmu.northeast_china_delight.item.GrillSeasonings;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
//? if >=1.20.5 {
/*import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
*///?}
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.20.2 {
import net.minecraftforge.client.model.data.ModelData;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

import java.util.List;

/**
 * JEI 里的「烧烤」分类。排版：最左边是营火烤架本身，食材那一格就摆在它正上方；营火右边是这一份要用到的调料（一样一格），再往右是箭头和烤好的成品，烤制时长写在箭头下面。和「大
 * 缸」分类一样不带背景图，尺寸自己给。
 */
public class GrillRecipeCategory implements IRecipeCategory<GrillJeiRecipe> {

    private static final int SLOT = 16;
    private static final int STEP = 20;
    private static final int MAX_SEASONINGS = Math.max(1, GrillJeiRecipes.all().stream()
            .mapToInt(recipe -> recipe.seasonings().size())
            .max()
            .orElse(1));

    private static final int STATION_SIZE = 32;
    private static final int STATION_X = 6;
    private static final int STATION_Y = 20;
    private static final float STATION_SCALE = 20.0F;

    private static final int INPUT_X = STATION_X + (STATION_SIZE - SLOT) / 2;
    private static final int INPUT_Y = STATION_Y - SLOT - 2;

    private static final int ROW_Y = STATION_Y + (STATION_SIZE - SLOT) / 2;
    private static final int SEASONING_X = STATION_X + STATION_SIZE + 6;

    private static final int ARROW_X = SEASONING_X + MAX_SEASONINGS * STEP + 4;
    private static final int ARROW_Y = ROW_Y;
    private static final int RESULT_X = ARROW_X + 24;
    private static final int TIME_Y = ROW_Y + 22;
    private static final int TIME_COLOR = 0xFF3F3F3F;

    private static final int AMOUNT_Y = ROW_Y + SLOT + 3;
    private static final float AMOUNT_TEXT_SCALE = 0.65F;

    private static final int WIDTH = RESULT_X + SLOT + 6;
    private static final int HEIGHT = 60;

    private static final ResourceLocation WIDGETS = DdIds.of(
            NortheastChinaDelight.MODID, "textures/gui/jei_widgets.png");
    private static final int ARROW_U = 61, ARROW_V = 93, ARROW_W = 24, ARROW_H = 16;

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IGuiHelper guiHelper;
    /** 旧版 JEI（1.20.1 用 15.x）必须给一张背景图，这里给的是同尺寸的空白图 */
    private final IDrawable background;

    public GrillRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = new StationIcon();
        //? if <1.20.5 {
        this.arrow = guiHelper.createDrawable(WIDGETS, ARROW_U, ARROW_V, ARROW_W, ARROW_H);
        //?} else {
        /*this.arrow = guiHelper.getRecipeArrow();
        *///?}
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    private static IRecipeSlotBuilder withSlotBackground(IRecipeSlotBuilder slot, IGuiHelper guiHelper) {
        //? if <1.20.5 {
        return slot.setBackground(guiHelper.getSlotDrawable(), -1, -1);
        //?} else {
        /*return slot.setStandardSlotBackground();
        *///?}
    }

    @Override
    public RecipeType<GrillJeiRecipe> getRecipeType() {
        return NortheastJeiPlugin.GRILL_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.northeast_china_delight.grill");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GrillJeiRecipe recipe, IFocusGroup focuses) {
        withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addItemStacks(recipe.ingredient()), this.guiHelper);

        List<ItemStack> seasonings = recipe.seasonings();
        for (int i = 0; i < seasonings.size() && i < MAX_SEASONINGS; i++) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, SEASONING_X + i * STEP, ROW_Y)
                    .addItemStack(seasonings.get(i)), this.guiHelper);
        }

        withSlotBackground(builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, ROW_Y)
                .addItemStack(recipe.result()), this.guiHelper);
    }

    @Override
    public void draw(GrillJeiRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        //? if <1.20.5 {
        drawStation(graphics, STATION_X + STATION_SIZE / 2.0F, STATION_Y + STATION_SIZE / 2.0F, STATION_SCALE);
        drawSeasoningAmounts(graphics, recipe.seasonings());
        this.arrow.draw(graphics, ARROW_X, ARROW_Y);
        Component text = timeText(recipe.seconds());
        Font font = Minecraft.getInstance().font;
        int x = ARROW_X + (this.arrow.getWidth() - font.width(text)) / 2;
        graphics.drawString(font, text, Math.max(0, x), TIME_Y, TIME_COLOR, false);
        //?}
    }

    /**
     * JEI 分类图标：同样画那台营火烤架，只是缩到 16×16 的标签格里
     */
    private static final class StationIcon implements IDrawable {

        @Override
        public int getWidth() {
            return SLOT;
        }

        @Override
        public int getHeight() {
            return SLOT;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            drawStation(graphics, xOffset + SLOT / 2.0F, yOffset + SLOT / 2.0F, 12.0F);
        }
    }

    private static Component timeText(int seconds) {
        if (seconds <= 0) {
            return Component.empty();
        }
        if (seconds < 60) {
            return Component.translatable("jei.northeast_china_delight.time.seconds", seconds);
        }
        if (seconds % 60 == 0) {
            return Component.translatable("jei.northeast_china_delight.time.minutes", seconds / 60);
        }
        return Component.translatable("jei.northeast_china_delight.time.minutes_seconds", seconds / 60, seconds % 60);
    }

    //? if >=1.20.5 {
    /*@Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GrillJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new StationWidget());
        builder.addWidget(new SeasoningAmountWidget(recipe.seasonings()));
        builder.addWidget(new TimeWidget(this.arrow, recipe.seconds()));
    }
    *///?}

    private static void drawStation(GuiGraphics graphics, float centerX, float centerY, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = ModBlocks.GRILL_CAMPFIRE.get().defaultBlockState()
                .setValue(CampfireBlock.LIT, true)
                .setValue(CampfireBlock.FACING, Direction.SOUTH);

        PoseStack pose = graphics.pose();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        pose.pushPose();
        pose.translate(centerX, centerY, 100.0F);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        pose.translate(-0.5F, -0.5F, -0.5F);

        minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
        GrillRackGeometry.render(pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                false, false, false, false);
        buffers.endBatch();
        pose.popPose();
    }

    private static void drawSeasoningAmounts(GuiGraphics graphics, List<ItemStack> seasonings) {
        Font font = Minecraft.getInstance().font;
        Component bottleText = Component.translatable("jei.northeast_china_delight.grill.seasoning_amount_brush",
                SeasoningBottleItem.GRILL_DOSE_MB);
        Component sauceText = Component.translatable("jei.northeast_china_delight.grill.seasoning_sauce");
        PoseStack pose = graphics.pose();
        for (int i = 0; i < seasonings.size() && i < MAX_SEASONINGS; i++) {
            ItemStack seasoning = seasonings.get(i);
            Component text;
            if (GrillSeasonings.isSauce(seasoning)) {
                text = sauceText;
            } else if (seasoning.getItem() instanceof SeasoningBottleItem) {
                text = bottleText;
            } else {
                continue;
            }
            float textWidth = font.width(text) * AMOUNT_TEXT_SCALE;
            float x = SEASONING_X + i * STEP + (SLOT - textWidth) / 2.0F;
            pose.pushPose();
            pose.translate(x, AMOUNT_Y, 0.0F);
            pose.scale(AMOUNT_TEXT_SCALE, AMOUNT_TEXT_SCALE, 1.0F);
            graphics.drawString(font, text, 0, 0, TIME_COLOR, false);
            pose.popPose();
        }
    }

    //? if >=1.20.5 {
    /*/^* 页面中间那台营火烤架。控件最后绘制，所以不会被槽位盖住 ^/
    private static final class StationWidget implements IRecipeWidget {

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            drawStation(graphics, STATION_X + STATION_SIZE / 2.0F, STATION_Y + STATION_SIZE / 2.0F, STATION_SCALE);
        }
    }

    /^*
     * 瓶装调料的用量：在对应槽位下面写一行小字（比如「10 mB」）。
     * 盐、糖这类不是瓶装的就不写 —— 它们一次消耗一份，没有 mB 概念。
     ^/
    private record SeasoningAmountWidget(List<ItemStack> seasonings) implements IRecipeWidget {

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            drawSeasoningAmounts(graphics, this.seasonings);
        }
    }

    /^* 画箭头和箭头下面的烤制时长 ^/
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

    *///?}
}
