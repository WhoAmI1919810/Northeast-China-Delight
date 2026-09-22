package com.gunmu.dongbei_delight.compat.jei;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.item.ModItems;
import mezz.jei.api.gui.builder.IIngredientConsumer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
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
    private static final int TIME_Y = 58;
    private static final int TIME_COLOR = 0xFF3F3F3F;
    /** 时长文字最远画到这里，避免压到右边的产物格 */
    private static final int TIME_MAX_RIGHT = OUTPUT_X - 2;

    /** 槽位名字：动画换档时要靠它找回具体的槽 */
    private static final String SLOT_SEAL = "seal";
    private static final String SLOT_SEASONING_0 = "seasoning_0";
    private static final String SLOT_SEASONING_1 = "seasoning_1";
    private static final String SLOT_PRIMARY_0 = "primary_0";
    private static final String SLOT_PRIMARY_1 = "primary_1";
    private static final String SLOT_LIQUID = "liquid";
    private static final String SLOT_RESULT = "result";
    private static final String SLOT_RESULT_FLUID = "result_fluid";

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable vatIcon;

    public VatRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModItems.VAT.get());
        this.arrow = guiHelper.getRecipeArrow();
        this.vatIcon = guiHelper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "textures/gui/jei_vat.png"),
                        0, 0, VAT_ICON_SIZE, VAT_ICON_SIZE)
                .setTextureSize(VAT_ICON_SIZE, VAT_ICON_SIZE)
                .build();
    }

    @Override
    public RecipeType<VatJeiRecipe> getRecipeType() {
        return DongbeiJeiPlugin.VAT_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.dongbei_delight.vat");
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

    // ===== 行的排布：左侧材料从上到下依次是 顶部封缸物 → 配料 → 食材 → 缸底液体，整列上下居中 =====

    /** 这条配方在左侧占几行 */
    private static int rowCount(VatJeiRecipe.State state) {
        int rows = 0;
        if (!state.seal().isEmpty()) {
            rows++;
        }
        if (!state.seasoning().isEmpty()) {
            rows++;
        }
        if (!state.primary().isEmpty()) {
            rows++;
        }
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

    /** 每一行的 y（和 {@link #rowCount} 的顺序一致），给引线用 */
    private static List<Integer> rowYs(VatJeiRecipe.State state) {
        int rows = rowCount(state);
        List<Integer> ys = new ArrayList<>(rows);
        int y = firstRowY(rows);
        for (int i = 0; i < rows; i++) {
            ys.add(y);
            y += ROW_STEP;
        }
        return ys;
    }

    /** 那一行是不是并排两格（右侧边界更靠右） */
    private static boolean isPairRow(VatJeiRecipe.State state, int index) {
        boolean hasSeal = !state.seal().isEmpty();
        boolean hasSeasoning = !state.seasoning().isEmpty();
        boolean hasPrimary = !state.primary().isEmpty();
        int row = 0;
        if (hasSeal) {
            if (row++ == index) {
                return false;
            }
        }
        if (hasSeasoning) {
            if (row++ == index) {
                return state.seasoning().size() > 1;
            }
        }
        if (hasPrimary) {
            if (row++ == index) {
                return state.primary().size() > 1;
            }
        }
        return false;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VatJeiRecipe recipe, IFocusGroup focuses) {
        VatJeiRecipe.State state = recipe.first();
        int y = firstRowY(rowCount(state));

        // 顶部：封缸物
        if (!state.seal().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, y)
                    .addItemStacks(state.seal())
                    .setSlotName(SLOT_SEAL)
                    .setStandardSlotBackground();
            y += ROW_STEP;
        }
        // 配料
        if (!state.seasoning().isEmpty()) {
            addRow(builder, state.seasoning(), y, SLOT_SEASONING_0, SLOT_SEASONING_1);
            y += ROW_STEP;
        }
        // 食材
        if (!state.primary().isEmpty()) {
            addRow(builder, state.primary(), y, SLOT_PRIMARY_0, SLOT_PRIMARY_1);
            y += ROW_STEP;
        }
        // 缸底：液体（缸口里另外会画一层液面，这里的格子是给 JEI 查配方用的）
        if (state.hasLiquid()) {
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, y)
                    .addFluidStack(state.liquid(), state.liquidMb())
                    .setFluidRenderer(state.liquidCapacityMb(), false, SLOT, SLOT)
                    .setSlotName(SLOT_LIQUID)
                    .setStandardSlotBackground();
        }

        // 产物：物品格在上、液体格在下；只有一种时单独居中
        // （大酱 / 酱油 / 醋 留在缸里的是酱渣，鱼露 / 虾酱 / 白醋 没有物品产物）
        boolean hasResultItem = !state.result().isEmpty();
        boolean hasResultFluid = state.hasResultFluid();
        if (hasResultItem) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X,
                            hasResultFluid ? OUTPUT_ITEM_Y : OUTPUT_ONLY_Y)
                    .addItemStack(state.result())
                    .setSlotName(SLOT_RESULT)
                    .setStandardSlotBackground();
        }
        if (hasResultFluid) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X,
                            hasResultItem ? OUTPUT_FLUID_Y : OUTPUT_ONLY_Y)
                    .addFluidStack(state.resultFluid(), state.resultFluidMb())
                    .setFluidRenderer(state.resultFluidCapacityMb(), false, SLOT, SLOT)
                    .setSlotName(SLOT_RESULT_FLUID)
                    .setStandardSlotBackground();
        }
    }

    /**
     * 一行里的槽：一个就放单列位置，两个就并排（整体仍以单列为中心线）。
     */
    private static void addRow(IRecipeLayoutBuilder builder, List<List<ItemStack>> slots, int y,
                               String firstName, String secondName) {
        int count = Math.min(slots.size(), 2);
        for (int i = 0; i < count; i++) {
            List<ItemStack> items = slots.get(i);
            if (items.isEmpty()) {
                continue;
            }
            int x = count == 1 ? INPUT_X : (i == 0 ? INPUT_PAIR_X1 : INPUT_PAIR_X2);
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStacks(items)
                    .setSlotName(i == 0 ? firstName : secondName)
                    .setStandardSlotBackground();
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, VatJeiRecipe recipe, IFocusGroup focuses) {
        builder.addWidget(new VatWidget(recipe.states(), builder.getRecipeSlots(),
                this.arrow, this.vatIcon));
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
        private final IDrawable vatIcon;
        private int ticks;
        private int index;

        private VatWidget(List<VatJeiRecipe.State> states, IRecipeSlotDrawablesView slots,
                          IDrawable arrow, IDrawable vatIcon) {
            this.states = states;
            this.slots = slots;
            this.arrow = arrow;
            this.vatIcon = vatIcon;
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
            this.vatIcon.draw(graphics, VAT_X, VAT_Y);
            // 每个材料格引一条线到缸上对应的位置
            List<Integer> ys = rowYs(this.states.get(0));
            List<int[]> targets = leaderTargets(this.states.get(0));
            for (int i = 0; i < ys.size(); i++) {
                int rowY = ys.get(i);
                int right = isPairRow(this.states.get(0), i) ? ROW_RIGHT_PAIR : ROW_RIGHT_SINGLE;
                int centerY = rowY + SLOT / 2;
                int[] target = targets.get(i);
                graphics.fill(right + 1, centerY, LEADER_BUS_X + 1, centerY + 1, LEADER_COLOR);
                drawLine(graphics, LEADER_BUS_X, centerY, target[0], target[1], LEADER_COLOR);
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
        }

        private void apply(VatJeiRecipe.State state) {
            VatJeiRecipe.State base = this.states.get(0);
            overrideItems(SLOT_SEASONING_0, base.seasoning(), state.seasoning(), 0);
            overrideItems(SLOT_SEASONING_1, base.seasoning(), state.seasoning(), 1);
            overrideItems(SLOT_PRIMARY_0, base.primary(), state.primary(), 0);
            overrideItems(SLOT_PRIMARY_1, base.primary(), state.primary(), 1);
            overrideFluid(SLOT_LIQUID, base.liquid(), base.liquidMb(), state.liquid(), state.liquidMb());
            overrideItem(SLOT_RESULT, base.result(), state.result());
            overrideFluid(SLOT_RESULT_FLUID, base.resultFluid(), base.resultFluidMb(),
                    state.resultFluid(), state.resultFluidMb());
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

    // ===== 画图小工具 =====

    /** 每一行的引线落点，顺序和 {@link #rowYs} 一致：封缸物 → 缸口，食材/调料 → 侧面，液体 → 缸底 */
    private static List<int[]> leaderTargets(VatJeiRecipe.State state) {
        List<int[]> targets = new ArrayList<>(4);
        if (!state.seal().isEmpty()) {
            targets.add(new int[]{LEADER_MOUTH_X, LEADER_MOUTH_Y});
        }
        if (!state.seasoning().isEmpty()) {
            targets.add(new int[]{LEADER_SIDE_X, LEADER_SIDE_Y});
        }
        if (!state.primary().isEmpty()) {
            targets.add(new int[]{LEADER_SIDE_X, LEADER_SIDE_Y});
        }
        if (state.hasLiquid()) {
            targets.add(new int[]{LEADER_BOTTOM_X, LEADER_BOTTOM_Y});
        }
        return targets;
    }

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

    /** 发酵时长：不足一分钟写「N 秒」，整分钟写「N 分钟」，其余写「N 分 M 秒」 */
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
        return Component.translatable("jei.dongbei_delight.time.minutes_seconds",
                seconds / 60, seconds % 60);
    }
}
