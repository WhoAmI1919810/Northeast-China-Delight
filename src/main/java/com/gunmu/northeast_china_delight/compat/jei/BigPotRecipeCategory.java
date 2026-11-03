package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.client.VatRenderer;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * JEI 里的「大锅烹饪」分类，照用户给的示意图（main 目录下的 铁锅JEI示意.png）排：
 *
 * <ul>
 *   <li>左边是生着火、放着锅的大灶（游戏里的方块模型），配方要液体就在锅里画一层液面（流体贴图按流体染色，水不画成白的）；
 *       灶下面一个火的小图标，和农夫乐事厨锅那页一个样子（自己重画的，见 textures/gui/jei/big_pot_heat.png）；</li>
 *   <li>上面一排 3 格调料，下面 2×3 格食材，调料那排往右错开半格；</li>
 *   <li>箭头指向右边的大成品格（数量 = 一锅盛几份），箭头下面是盛菜用的容器，最下面写烹饪时间；</li>
 *   <li>锅盖缩短时间（{@code lid_speedup}）在 JEI 里不显示。</li>
 * </ul>
 */
public class BigPotRecipeCategory implements IRecipeCategory<BigPotJeiRecipe> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 80;
    private static final int SLOT = 16;
    private static final int STRIDE = 18;

    private static final float STOVE_X = 27.0F;
    private static final float STOVE_Y = 31.0F;
    private static final float STOVE_SCALE = 17.0F;
    private static final int HEAT_X = 19;
    private static final int HEAT_Y = 58;
    private static final int SEASONING_X = 66;
    private static final int SEASONING_Y = 3;
    private static final int GRID_X = 57;
    private static final int GRID_Y = 24;
    private static final int GRID_COLUMNS = 3;
    private static final int ARROW_X = 115;
    private static final int ARROW_Y = 33;
    private static final int CONTAINER_X = 119;
    private static final int CONTAINER_Y = 55;
    private static final int OUTPUT_FRAME_X = 145;
    private static final int OUTPUT_FRAME_Y = 28;
    private static final int OUTPUT_FRAME = 26;
    private static final int TIME_Y = 67;
    private static final int TEXT_COLOR = 0xFF3F3F3F;

    /** 液面：锅里第二圈台阶那么高（32×32 的大灶坐标里 y 14.5），半径比那圈锅壁小一点 */
    private static final float LIQUID_Y = 14.5F;
    private static final float LIQUID_RADIUS = 10.9F;

    private static final ResourceLocation HEAT = DdIds.of(NortheastChinaDelight.MODID,
            "textures/gui/jei/big_pot_heat.png");

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable background;
    private final IGuiHelper guiHelper;

    public BigPotRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        //? if <1.20.5 {
        /*this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIG_IRON_POT.get()));
        this.arrow = guiHelper.drawableBuilder(
                DdIds.of(NortheastChinaDelight.MODID, "textures/gui/jei_widgets.png"), 61, 93, 24, 16)
                .build();*/
        //?} else {
        this.icon = guiHelper.createDrawableItemLike(ModItems.BIG_IRON_POT.get());
        this.arrow = guiHelper.getRecipeArrow();
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
        for (int i = 0; i < recipe.seasonings().size(); i++) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, SEASONING_X + i * STRIDE, SEASONING_Y)
                    .addItemStacks(recipe.seasonings().get(i))
                    .addTooltipCallback((slotView, tooltip) -> tooltip.add(
                            Component.translatable("jei.northeast_china_delight.big_pot.seasoning"))));
        }
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT,
                            GRID_X + (i % GRID_COLUMNS) * STRIDE, GRID_Y + (i / GRID_COLUMNS) * STRIDE)
                    .addItemStacks(recipe.ingredients().get(i)));
        }
        if (!recipe.container().isEmpty()) {
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
        builder.addWidget(new SceneWidget(recipe, this.arrow));
    }

    @Override
    public void draw(BigPotJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        //? if <1.20.5 {
        // 1.20.1 走老 draw：灶、火、箭头、大框、时间都在这里画
        drawScene(graphics, recipe, this.arrow);
        //?}
    }

    private static void drawScene(GuiGraphics graphics, BigPotJeiRecipe recipe, IDrawable arrow) {
        drawStove(graphics, recipe.liquids().isEmpty() ? null : recipe.liquids().get(0));
        graphics.blit(HEAT, HEAT_X, HEAT_Y, 0.0F, 0.0F, 16, 16, 16, 16);
        arrow.draw(graphics, ARROW_X, ARROW_Y);
        drawFrame(graphics, OUTPUT_FRAME_X, OUTPUT_FRAME_Y, OUTPUT_FRAME, OUTPUT_FRAME);
        drawTime(graphics, recipe.cookingTime());
    }

    /** 生着火、放着锅的大灶（朝北，四格照 facing=north 的位置摆），锅里有液体就叠一层液面 */
    private static void drawStove(GuiGraphics graphics, Fluid liquid) {
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
            drawLiquid(pose, buffers, liquid);
        }
        buffers.endBatch();
        pose.popPose();
    }

    /** 锅里的液面：按 1 像素一行拼成圆，流体静止贴图铺满整个圆，颜色按流体染（水给群系默认水色） */
    private static void drawLiquid(PoseStack pose, MultiBufferSource buffers, Fluid fluid) {
        TextureAtlasSprite sprite = VatRenderer.fluidSprite(fluid);
        if (sprite == null) {
            return;
        }
        int tint = VatRenderer.fluidTint(fluid);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = Math.max(0.75F, ((tint >>> 24) & 0xFF) / 255.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        float y = LIQUID_Y / 16.0F;
        for (int row = 0; row < 32; row++) {
            float d = row + 0.5F - 16.0F;
            if (Math.abs(d) >= LIQUID_RADIUS) {
                continue;
            }
            float w = Math.round((float) Math.sqrt(LIQUID_RADIUS * LIQUID_RADIUS - d * d) * 2.0F) / 2.0F;
            float x0 = (16.0F - w) / 16.0F;
            float x1 = (16.0F + w) / 16.0F;
            float z0 = row / 16.0F;
            float z1 = (row + 1) / 16.0F;
            VatRenderer.quad(consumer, pose.last(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    red, green, blue, alpha,
                    x0, y, z0, u(sprite, x0), v(sprite, z0),
                    x0, y, z1, u(sprite, x0), v(sprite, z1),
                    x1, y, z1, u(sprite, x1), v(sprite, z1),
                    x1, y, z0, u(sprite, x1), v(sprite, z0),
                    0, 1, 0);
        }
    }

    /** 贴图铺满两格宽的灶面（不用 getU：1.20.1 和 1.21.1 的参数单位不一样） */
    private static float u(TextureAtlasSprite sprite, float blocks) {
        return sprite.getU0() + (sprite.getU1() - sprite.getU0()) * blocks / 2.0F;
    }

    private static float v(TextureAtlasSprite sprite, float blocks) {
        return sprite.getV0() + (sprite.getV1() - sprite.getV0()) * blocks / 2.0F;
    }

    /** 和 JEI 的槽一个画法：左上深、右下白、中间灰 */
    private static void drawFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        graphics.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    private static void drawTime(GuiGraphics graphics, int ticks) {
        Font font = Minecraft.getInstance().font;
        String seconds = ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format("%.1f", ticks / 20.0F);
        Component text = Component.translatable("jei.northeast_china_delight.big_pot.time", seconds);
        float scale = 0.8F;
        float centerX = GRID_X + (GRID_COLUMNS * STRIDE - 2) / 2.0F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX - font.width(text) * scale / 2.0F, TIME_Y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, TEXT_COLOR, false);
        pose.popPose();
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
