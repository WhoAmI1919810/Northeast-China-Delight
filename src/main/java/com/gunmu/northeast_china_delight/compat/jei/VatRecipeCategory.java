package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
//? if >=1.20.5 {
import mezz.jei.api.gui.builder.IIngredientConsumer;
//?}
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
//? if >=1.20.5 {
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
//?}
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
//? if >=1.20.5 {
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
//?}
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.20.5 {
import net.minecraft.client.gui.navigation.ScreenPosition;
//?}
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if <1.20.2 {
/*import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
*///?} else {
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.client.model.data.ModelData;
*///?} else {
import net.neoforged.neoforge.client.model.data.ModelData;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 里的「大缸」分类。
 *
 * 画面中央是大缸的图标（等距视角的陶缸），左侧竖排是往缸里放的材料
 * （顶部封缸物 → 配料 → 食材 → 缸底液体），每一行都有一条引线连到缸身上；
 * 右侧是产物：物品格在上、液体格在下，箭头从缸指向产物，箭头下方写发酵时长。
 *
 * 配方要用到液体时，缸口里会按那种液体的颜色画一层液面（用多少都画，只是表示「缸里有这种液体」）。
 * 箭头、引线、液面和时长都由 {@link VatWidget} 绘制 —— 控件最后绘制，所以不会被槽位盖住。
 *
 * 多档配方（例如辣白菜按水位 1 / 2 / 3 层）由同一个控件每 1.5 秒换一档。
 */
public class VatRecipeCategory implements IRecipeCategory<VatJeiRecipe> {

    public static final int WIDTH = 138;
    public static final int HEIGHT = 92;

    /** 槽位 16×16，行距 18（槽与槽之间留 2px） */
    private static final int SLOT = 16;
    private static final int ROW_STEP = 18;
    /** 一行放两个槽时的间距 */
    private static final int PAIR_GAP = 6;

    /** 输入列：单格居中时的 x，以及并排两格时的两个 x */
    private static final int INPUT_X = 15;
    private static final int INPUT_PAIR_X1 = 4;
    private static final int INPUT_PAIR_X2 = INPUT_PAIR_X1 + SLOT + PAIR_GAP;
    /** 引线从行的右边缘出发：单格行 / 双格行 */
    private static final int ROW_RIGHT_SINGLE = INPUT_X + SLOT;
    private static final int ROW_RIGHT_PAIR = INPUT_PAIR_X2 + SLOT;
    /** 引线先横着走到这条竖线上，再斜着连到缸身 */
    private static final int LEADER_BUS_X = 46;
    private static final int LEADER_COLOR = 0xFF6B6B6B;

    /** 页面竖直中心：箭头、缸、产物都对齐到这条线 */
    private static final int CENTER_Y = 46;

    /**
     * 大缸图标 32×32，是把游戏里的 vat_top / vat_side 做等距投影生成的
     * （见 tools/generate_jei_assets.ps1），缸口那片是透明的，
     * 所以先在下面画液面、再盖上图标，液体就露在缸口里。
     */
    private static final int VAT_ICON_SIZE = 32;
    private static final int VAT_X = 52;
    private static final int VAT_Y = CENTER_Y - VAT_ICON_SIZE / 2;
    private static final int MOUTH_X = VAT_X + 16;
    private static final int MOUTH_Y = VAT_Y + 9;
    /** 缸口透明区的半宽 / 半高，液面就画这么大（由脚本报告：x 6~25、y 4~13） */
    private static final int MOUTH_HALF_WIDTH = 10;
    private static final int MOUTH_HALF_HEIGHT = 5;

    /**
     * 引线的落点：封缸物指向缸口，食材 / 调料指向缸身侧面，液体指向缸底。
     */
    private static final int LEADER_MOUTH_X = MOUTH_X - 6;
    private static final int LEADER_MOUTH_Y = MOUTH_Y - 3;
    private static final int LEADER_SIDE_X = VAT_X + 4;
    private static final int LEADER_SIDE_Y = VAT_Y + 17;
    private static final int LEADER_BOTTOM_X = VAT_X + 8;
    private static final int LEADER_BOTTOM_Y = VAT_Y + 24;

    /** 箭头与产物 */
    private static final int ARROW_X = 88;
    private static final int ARROW_Y = CENTER_Y - SLOT / 2;
    private static final int OUTPUT_X = 114;
    private static final int OUTPUT_ITEM_Y = CENTER_Y - 17;
    private static final int OUTPUT_FLUID_Y = CENTER_Y + 1;
    private static final int OUTPUT_ONLY_Y = CENTER_Y - SLOT / 2;

    /** 发酵时长写在箭头下方 */
    private static final int TIME_Y = 70;
    private static final int TIME_COLOR = 0xFF3F3F3F;
    /** 时长文字最远画到这里，避免压到右边的产物格 */
    private static final int TIME_MAX_RIGHT = OUTPUT_X - 2;

    /** 槽位名字：动画换档时要靠它找回具体的槽 */
    private static final String SLOT_SEAL = "seal";
    private static final String SLOT_SEASONING_0 = "seasoning_0";
    private static final String SLOT_SEASONING_1 = "seasoning_1";
    private static final String SLOT_SEASONING_2 = "seasoning_2";
    private static final String SLOT_SEASONING_3 = "seasoning_3";
    private static final String SLOT_PRIMARY_0 = "primary_0";
    private static final String SLOT_PRIMARY_1 = "primary_1";
    private static final String SLOT_PRIMARY_2 = "primary_2";
    private static final String SLOT_PRIMARY_3 = "primary_3";
    private static final String SLOT_LIQUID = "liquid";
    private static final String SLOT_RESULT = "result";
    private static final String SLOT_RESULT_FLUID = "result_fluid";

    /** 每一层的槽位名（一层最多 4 格：每行 2 格） */
    private static final String[] SEASONING_SLOTS =
            { SLOT_SEASONING_0, SLOT_SEASONING_1, SLOT_SEASONING_2, SLOT_SEASONING_3 };
    private static final String[] PRIMARY_SLOTS =
            { SLOT_PRIMARY_0, SLOT_PRIMARY_1, SLOT_PRIMARY_2, SLOT_PRIMARY_3 };

    /** 旧版 JEI（1.20.1 用 15.x）没有现成的箭头图，小箭头从模组自己的控件贴图上取 */
    private static final ResourceLocation WIDGETS = DdIds.of(
            NortheastChinaDelight.MODID, "textures/gui/jei_widgets.png");
    private static final int ARROW_U = 61, ARROW_V = 93, ARROW_W = 24, ARROW_H = 16;

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IGuiHelper guiHelper;
    /** 旧版 JEI（1.20.1 用 15.x）必须给一张背景图，这里给的是同尺寸的空白图 */
    private final IDrawable background;

    public VatRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        //? if <1.20.5 {
        /*this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.VAT.get()));
        this.arrow = guiHelper.createDrawable(WIDGETS, ARROW_U, ARROW_V, ARROW_W, ARROW_H);*/
        //?} else {
        this.icon = guiHelper.createDrawableItemLike(ModItems.VAT.get());
        this.arrow = guiHelper.getRecipeArrow();
        //?}
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
    }

    /**
     * 槽位底图：两个版本的 JEI 说法不一样 —— 1.21 要显式说「用标准槽位底图」，
     * 旧版 JEI 得自己把 guiHelper 的槽位图挂上去。
     */
    private static IRecipeSlotBuilder withSlotBackground(IRecipeSlotBuilder slot, IGuiHelper guiHelper) {
        //? if <1.20.5 {
        /*return slot.setBackground(guiHelper.getSlotDrawable(), -1, -1);*/
        //?} else {
        return slot.setStandardSlotBackground();
        //?}
    }

    @Override
    public RecipeType<VatJeiRecipe> getRecipeType() {
        return NortheastJeiPlugin.VAT_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.northeast_china_delight.vat");
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    /** 没有背景图，JEI 的 getBackground() 默认返回 null，所以尺寸得自己给 */
    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    /** 旧版 JEI 会拿它当页面底板（空白图）；1.21 的 JEI 用不到 */
    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    // ===== 行的排布：左侧材料从上到下依次是 顶部封缸物 → 配料 → 食材 → 缸底液体，整列上下居中 =====

    /** 一层占几行：一格一行、两格一行，三 / 四格占两行（每行 2 格） */
    private static int layerRows(int slots) {
        return slots <= 0 ? 0 : (slots + 1) / 2;
    }

    /** 这一层按"每行两个"切开 */
    private static List<List<List<ItemStack>>> splitRows(List<List<ItemStack>> layer) {
        List<List<List<ItemStack>>> rows = new ArrayList<>();
        for (int i = 0; i < layer.size(); i += 2) {
            rows.add(layer.subList(i, Math.min(layer.size(), i + 2)));
        }
        return rows;
    }

    /** 这条配方在左侧占几行 */
    private static int rowCount(VatJeiRecipe.State state) {
        int rows = 0;
        if (!state.seal().isEmpty()) {
            rows++;
        }
        rows += layerRows(state.seasoning().size());
        rows += layerRows(state.primary().size());
        if (state.hasLiquid()) {
            rows++;
        }
        return rows;
    }

    /** 第一行的 y */
    private static int firstRowY(int rows) {
        if (rows <= 0) {
            return 0;
        }
        int total = rows * SLOT + (rows - 1) * (ROW_STEP - SLOT);
        return (HEIGHT - total) / 2;
    }

    /**
     * 左侧的一行：纵坐标 + 是不是并排两格 + 引线连到缸上哪一点。
     *
     * <p>槽位摆放、引线绘制都读同一份 {@link #rowsOf}，
     * 所以"行数"和"引线条数"不可能再对不上（以前辣白菜那一页就因此越界崩过）。
     */
    private record Row(int y, boolean pair, int targetX, int targetY) {
    }

    /** 把一页配方左侧的每一行算出来（封口物 → 配料 → 食材 → 缸底液体，整列上下居中） */
    private static List<Row> rowsOf(VatJeiRecipe.State state) {
        List<Row> rows = new ArrayList<>();
        int y = firstRowY(rowCount(state));
        if (!state.seal().isEmpty()) {
            rows.add(new Row(y, false, LEADER_MOUTH_X, LEADER_MOUTH_Y));
            y += ROW_STEP;
        }
        y = addLayerRows(rows, state.seasoning(), y);
        y = addLayerRows(rows, state.primary(), y);
        if (state.hasLiquid()) {
            rows.add(new Row(y, false, LEADER_BOTTOM_X, LEADER_BOTTOM_Y));
        }
        return rows;
    }

    /** 一层占几行就补几行引线；行数多了以后引线落点往下挪几像素，免得两条线完全重叠 */
    private static int addLayerRows(List<Row> rows, List<List<ItemStack>> layer, int y) {
        List<List<List<ItemStack>>> split = splitRows(layer);
        for (int i = 0; i < split.size(); i++) {
            rows.add(new Row(y, split.get(i).size() > 1,
                    LEADER_SIDE_X, LEADER_SIDE_Y + Math.min(i, 3) * 3));
            y += ROW_STEP;
        }
        return y;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VatJeiRecipe recipe, IFocusGroup focuses) {
        VatJeiRecipe.State state = recipe.first();
        List<Row> rows = rowsOf(state);
        int row = 0;

        // 顶部：封缸物
        if (!state.seal().isEmpty()) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, rows.get(row++).y())
                    .addItemStacks(state.seal())
                    .setSlotName(SLOT_SEAL), this.guiHelper);
        }
        // 配料
        if (!state.seasoning().isEmpty()) {
            row = addLayer(builder, state.seasoning(), rows, row, SEASONING_SLOTS);
        }
        // 食材
        if (!state.primary().isEmpty()) {
            row = addLayer(builder, state.primary(), rows, row, PRIMARY_SLOTS);
        }
        // 缸底：液体（缸口里另外会画一层液面，这里的格子是给 JEI 查配方用的）
        if (state.hasLiquid()) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, rows.get(row).y())
                    .addFluidStack(state.liquid(), state.liquidMb())
                    .setFluidRenderer(state.liquidCapacityMb(), false, SLOT, SLOT)
                    .setSlotName(SLOT_LIQUID), this.guiHelper);
        }

        // 产物：物品格在上、液体格在下；只有一种时单独居中
        // （大酱 / 酱油 / 醋 留在缸里的是酱渣，鱼露 / 虾酱 / 白醋 没有物品产物）
        boolean hasResultItem = !state.result().isEmpty();
        boolean hasResultFluid = state.hasResultFluid();
        if (hasResultItem) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X,
                            hasResultFluid ? OUTPUT_ITEM_Y : OUTPUT_ONLY_Y)
                    .addItemStack(state.result())
                    .setSlotName(SLOT_RESULT), this.guiHelper);
        }
        if (hasResultFluid) {
            withSlotBackground(builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X,
                            hasResultItem ? OUTPUT_FLUID_Y : OUTPUT_ONLY_Y)
                    .addFluidStack(state.resultFluid(), state.resultFluidMb())
                    .setFluidRenderer(state.resultFluidCapacityMb(), false, SLOT, SLOT)
                    .setSlotName(SLOT_RESULT_FLUID), this.guiHelper);
        }
    }

    /**
     * 画一层槽位：每行最多两个，超过两个就往下再排一行。
     * 只有一格时居中，两格并排 —— 一格一行的老配方看起来和以前完全一样。
     *
     * @return 下一层从第几行开始
     */
    private int addLayer(IRecipeLayoutBuilder builder, List<List<ItemStack>> layer, List<Row> rows,
                         int rowIndex, String[] names) {
        List<List<List<ItemStack>>> split = splitRows(layer);
        for (int r = 0; r < split.size(); r++) {
            List<List<ItemStack>> row = split.get(r);
            int y = rows.get(rowIndex + r).y();
            for (int c = 0; c < row.size(); c++) {
                List<ItemStack> items = row.get(c);
                if (items.isEmpty()) {
                    continue;
                }
                int index = r * 2 + c;
                int x = row.size() == 1 ? INPUT_X : (c == 0 ? INPUT_PAIR_X1 : INPUT_PAIR_X2);
                withSlotBackground(builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addItemStacks(items)
                        .setSlotName(names[index]), this.guiHelper);
            }
        }
        return rowIndex + split.size();
    }

    /**
     * 旧版 JEI 没有「控件」那一层，除了槽位以外的画面都在这里画：
     * 缸口液面、大缸本体、引线、箭头、发酵时长和条件注记。
     *
     * <p>旧版 JEI 也没法给槽位做「显示覆盖」，所以多档配方在这里画的是第一档
     * （槽位里摆的也是第一档）——换档轮播是 1.21 分支的功能。</p>
     */
    //? if <1.20.5 {
    /*@Override
    public void draw(VatJeiRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        VatJeiRecipe.State state = recipe.first();

        // 缸口里的液面：先画液面再盖缸（缸口是透明的）
        if (state.hasLiquid() && state.liquid() != null) {
            drawFluidRhombus(graphics, state.liquid(), MOUTH_X, MOUTH_Y,
                    MOUTH_HALF_WIDTH, MOUTH_HALF_HEIGHT);
        }
        drawVatBlock(graphics, VAT_X + VAT_ICON_SIZE / 2F, VAT_Y + VAT_ICON_SIZE / 2F + 2, 22.0F);
        // 每个材料格引一条线到缸上对应的位置
        for (Row row : rowsOf(state)) {
            int right = row.pair() ? ROW_RIGHT_PAIR : ROW_RIGHT_SINGLE;
            int centerY = row.y() + SLOT / 2;
            graphics.fill(right + 1, centerY, LEADER_BUS_X + 1, centerY + 1, LEADER_COLOR);
            drawLine(graphics, LEADER_BUS_X, centerY, row.targetX(), row.targetY(), LEADER_COLOR);
        }
        // 箭头：从缸指向产物
        this.arrow.draw(graphics, ARROW_X, ARROW_Y);

        // 发酵时长写在箭头下方
        Component text = timeText(state.seconds());
        Font font = Minecraft.getInstance().font;
        int x = ARROW_X + (this.arrow.getWidth() - font.width(text)) / 2;
        if (x + font.width(text) > TIME_MAX_RIGHT) {
            x = TIME_MAX_RIGHT - font.width(text);
        }
        graphics.drawString(font, text, x, TIME_Y, TIME_COLOR, false);

        // 额外条件注记（"要在会下雪的群系里酿"），小一号字写在时长下面
        Component note = state.note();
        if (note != null) {
            int noteX = (WIDTH - font.width(note)) / 2;
            graphics.drawString(font, note, noteX, HEIGHT - 10, 0xFF666666, false);
        }
    }*/
    //?}

    //? if >=1.20.5 {
    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, VatJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new VatWidget(recipe.states(), builder.getRecipeSlots(),
                this.arrow));
    }

    /**
     * 画大缸、引线、箭头、液面和发酵时长，并负责多档配方的轮换。
     *
     * 控件最后绘制，所以这些图形都盖在槽位之上；换档时用「显示覆盖」把当档的数量与液面写进槽里。
     */
    private static final class VatWidget implements IRecipeWidget {

        /** 每档停留多少游戏刻（30 刻 = 1.5 秒） */
        private static final int TICKS_PER_STATE = 30;

        private final List<VatJeiRecipe.State> states;
        private final IRecipeSlotDrawablesView slots;
        private final IDrawable arrow;

        private int ticks;
        private int index;

        private VatWidget(List<VatJeiRecipe.State> states, IRecipeSlotDrawablesView slots,
                          IDrawable arrow) {
            this.states = states;
            this.slots = slots;
            this.arrow = arrow;
        }

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(0, 0);
        }

        @Override
        public void tick() {
            if (this.states.size() <= 1) {
                return;
            }
            if (++this.ticks >= TICKS_PER_STATE) {
                this.ticks = 0;
                this.index = (this.index + 1) % this.states.size();
            }
            // JEI 自己的物品轮播到点时会清掉所有显示覆盖，所以每刻都重设一遍
            apply(this.states.get(this.index));
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            VatJeiRecipe.State state = this.states.get(this.index);

            // 大缸本体
            // 缸口里的液面：只要配方用到液体就画，先画液面再盖缸（缸口是透明的）
            if (state.hasLiquid() && state.liquid() != null) {
                drawFluidRhombus(graphics, state.liquid(), MOUTH_X, MOUTH_Y,
                        MOUTH_HALF_WIDTH, MOUTH_HALF_HEIGHT);
            }
            drawVatBlock(graphics, VAT_X + VAT_ICON_SIZE / 2F, VAT_Y + VAT_ICON_SIZE / 2F + 2, 22.0F);
            // 每个材料格引一条线到缸上对应的位置
            for (Row row : rowsOf(this.states.get(0))) {
                int right = row.pair() ? ROW_RIGHT_PAIR : ROW_RIGHT_SINGLE;
                int centerY = row.y() + SLOT / 2;
                graphics.fill(right + 1, centerY, LEADER_BUS_X + 1, centerY + 1, LEADER_COLOR);
                drawLine(graphics, LEADER_BUS_X, centerY, row.targetX(), row.targetY(), LEADER_COLOR);
            }
            // 箭头：从缸指向产物
            this.arrow.draw(graphics, ARROW_X, ARROW_Y);

            // 发酵时长写在箭头下方
            Component text = timeText(state.seconds());
            Font font = Minecraft.getInstance().font;
            int x = ARROW_X + (this.arrow.getWidth() - font.width(text)) / 2;
            if (x + font.width(text) > TIME_MAX_RIGHT) {
                x = TIME_MAX_RIGHT - font.width(text);
            }
            graphics.drawString(font, text, x, TIME_Y, TIME_COLOR, false);

            // 额外条件注记（"要在会下雪的群系里酿"），小一号字写在时长下面
            Component note = state.note();
            if (note != null) {
                // 底部水平居中，避开缸身和引线
                int noteX = (WIDTH - font.width(note)) / 2;
                graphics.drawString(font, note, noteX, HEIGHT - 10, 0xFF666666, false);
            }
        }

        private void apply(VatJeiRecipe.State state) {
            VatJeiRecipe.State base = this.states.get(0);
            overrideLayer(base.seasoning(), state.seasoning(), SEASONING_SLOTS);
            overrideLayer(base.primary(), state.primary(), PRIMARY_SLOTS);
            overrideFluid(SLOT_LIQUID, base.liquid(), base.liquidMb(), state.liquid(), state.liquidMb());
            overrideItem(SLOT_RESULT, base.result(), state.result());
            overrideFluid(SLOT_RESULT_FLUID, base.resultFluid(), base.resultFluidMb(),
                    state.resultFluid(), state.resultFluidMb());
        }

        private void overrideLayer(List<List<ItemStack>> baseRows, List<List<ItemStack>> rows, String[] names) {
            for (int i = 0; i < names.length; i++) {
                overrideItems(names[i], baseRows, rows, i);
            }
        }

        private void overrideItems(String slotName, List<List<ItemStack>> baseRows,
                                   List<List<ItemStack>> rows, int rowIndex) {
            List<ItemStack> base = rowIndex < baseRows.size() ? baseRows.get(rowIndex) : List.of();
            List<ItemStack> now = rowIndex < rows.size() ? rows.get(rowIndex) : List.of();
            this.slots.findSlotByName(slotName).ifPresent(slot -> {
                if (sameItems(base, now)) {
                    slot.clearDisplayOverrides();
                    return;
                }
                IIngredientConsumer consumer = resetOverrides(slot);
                for (ItemStack stack : now) {
                    consumer.addItemStack(stack);
                }
            });
        }

        private void overrideItem(String slotName, ItemStack base, ItemStack now) {
            this.slots.findSlotByName(slotName).ifPresent(slot -> {
                if (sameItem(base, now)) {
                    slot.clearDisplayOverrides();
                    return;
                }
                resetOverrides(slot).addItemStack(now);
            });
        }

        private void overrideFluid(String slotName, @Nullable Fluid baseFluid, long baseAmount,
                                   @Nullable Fluid fluid, long amount) {
            if (fluid == null || amount <= 0) {
                return;
            }
            this.slots.findSlotByName(slotName).ifPresent(slot -> {
                if (baseFluid == fluid && baseAmount == amount) {
                    slot.clearDisplayOverrides();
                    return;
                }
                resetOverrides(slot).addFluidStack(fluid, amount);
            });
        }

        private static IIngredientConsumer resetOverrides(IRecipeSlotDrawable slot) {
            slot.clearDisplayOverrides();
            return slot.createDisplayOverrides();
        }

        private static boolean sameItems(List<ItemStack> a, List<ItemStack> b) {
            if (a.size() != b.size()) {
                return false;
            }
            for (int i = 0; i < a.size(); i++) {
                if (!sameItem(a.get(i), b.get(i))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean sameItem(ItemStack a, ItemStack b) {
            return a.getItem() == b.getItem() && a.getCount() == b.getCount();
        }
    }
    //?}

    // ===== 画图小工具 =====

    /**
     * 在缸口里画一层液面：直接拿这种液体的静止贴图，按行铺进菱形里（用多少液体都画，只表示「缸里有这种东西」）。
     *
     * 这里走方块图集里的 sprite 而不是直接 blit 贴图路径 —— 水的静止贴图是动画贴图，
     * 直接 blit 会取不到（画出来是紫黑格子），用 sprite 就会自动跟着动画帧。
     */
    private static void drawFluidRhombus(GuiGraphics graphics, Fluid fluid, int centerX, int centerY,
                                         int halfWidth, int halfHeight) {
        ResourceLocation texture = IClientFluidTypeExtensions.of(fluid).getStillTexture();
        TextureAtlasSprite sprite = texture == null
                ? null
                : Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        int tint = fluidTint(fluid);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        for (int dy = -halfHeight; dy <= halfHeight; dy++) {
            int span = halfWidth * (halfHeight - Math.abs(dy)) / halfHeight;
            int width = span * 2 + 1;
            if (sprite != null) {
                graphics.blit(centerX - span, centerY + dy, 0, width, 1, sprite, red, green, blue, alpha);
            } else {
                graphics.fill(centerX - span, centerY + dy, centerX + span + 1, centerY + dy + 1,
                        0xFF3F76E4);
            }
        }
    }

    /**
     * 液面要乘的颜色。
     * 原版水的贴图是灰白的，蓝色来自群系着色，所以这里给它默认水色；
     * 其它液体（含本模组的）贴图本身就是有颜色的，用自己的 tint（一般是白色 = 不改）。
     */
    private static int fluidTint(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return 0xFF3F76E4;
        }
        return IClientFluidTypeExtensions.of(fluid).getTintColor();
    }

    /**
     * 在 JEI 页里画真实的大缸方块（不是预制 PNG）。
     * 变换用「物品栏视角」：30° 俯角 + 225° 水平角，缸中心对齐 (centerX, centerY)。
     */
    private static void drawVatBlock(GuiGraphics graphics, float centerX, float centerY, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        net.minecraft.world.level.block.state.BlockState state = ModBlocks.VAT.get().defaultBlockState();

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
        buffers.endBatch();
        pose.popPose();
    }

    /** 画一条细线（逐点填充，够用就好） */
    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    /**
     * 发酵时长用游戏日表示（1 个 MC 日 = 24000 tick = 1200 秒）：
     *   - 不到一天 → 「N 秒」
     *   - 整天     → 「N 天」
     *   - 带零头   → 「N 天 M 秒」
     */
    private static Component timeText(int seconds) {
        if (seconds <= 0) {
            return Component.empty();
        }
        final int SECONDS_PER_DAY = 1200;
        int days = seconds / SECONDS_PER_DAY;
        int rest = seconds % SECONDS_PER_DAY;
        if (days == 0) {
            return Component.translatable("jei.northeast_china_delight.time.seconds", seconds);
        }
        if (rest == 0) {
            return Component.translatable("jei.northeast_china_delight.time.days", days);
        }
        return Component.translatable("jei.northeast_china_delight.time.days_seconds", days, rest);
    }
}
