package com.gunmu.dongbei_delight.compat.jade;

import com.gunmu.dongbei_delight.block.ModBlockStateProperties;
import com.gunmu.dongbei_delight.block.Vat;
import com.gunmu.dongbei_delight.block.VatBlockEntity;
import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 准星指大缸时显示：状态、水位、盐、内容物、压缸石/盖布、发酵进度、成品份数。
 *
 * 进度条只在这里显示；物品栏上方的 HUD 已经移除。
 * 数据全部读客户端那份方块实体（大缸每次变化都会同步），所以在没装 Jade 的服务端上也能正常显示。
 */
public enum VatComponentProvider implements IBlockComponentProvider {

    INSTANCE;

    /** 一行最多画几个内容物图标，避免刷屏 */
    private static final int MAX_CONTENT_ICONS = 12;

    @Override
    public ResourceLocation getUid() {
        return DongbeiJadePlugin.VAT_UID;
    }

    /** 排在方块名之后、其它模组信息之前 */
    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY + 100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof VatBlockEntity vat)) {
            return;
        }

        BlockState state = accessor.getBlockState();
        VatRecipes.Kind kind = vat.kind();
        int water = state.getValue(Vat.WATER_LEVEL);
        int progress = state.getValue(Vat.PROGRESS);
        boolean fermented = state.getValue(Vat.FERMENTED);
        IElementHelper helper = IElementHelper.get();

        tooltip.add(Component.translatable(kindKey(kind)).withStyle(ChatFormatting.WHITE));

        List<ItemStack> contents = vat.contents();
        if (kind == VatRecipes.Kind.NONE && water <= 0 && contents.isEmpty()
                && !vat.isPressed() && !vat.isCovered()) {
            return;
        }

        // 水位与盐：泡菜按「一层水一份盐」，大酱 / 酱油固定 3 份盐，腊肉按肉的数量配对
        int salt = vat.countOf(ModItems.SALT.get());
        int saltNeeded = switch (kind) {
            case PICKLE, SPICY_PICKLE -> water * VatRecipes.SALT_PER_WATER;
            case MEAT -> vat.meatCount();
            case PASTE, SOY_SAUCE -> VatRecipes.PASTE_SALT;
            default -> 0;
        };
        if (water > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.water", water,
                    ModBlockStateProperties.VAT_MAX_WATER));
        }
        if (salt > 0 || saltNeeded > 0) {
            // 泡菜 / 大酱 / 酱油的盐溶在水里，只做数字显示
            tooltip.add(Component.translatable(vat.isSaltDissolved()
                    ? "jade.dongbei_delight.vat.salt_dissolved"
                    : "jade.dongbei_delight.vat.salt", salt, saltNeeded));
        }
        // 大酱 / 酱油：把「酱块放够了没」也写出来，方便对着凑材料
        if (kind == VatRecipes.Kind.PASTE || kind == VatRecipes.Kind.SOY_SAUCE) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.paste_chunks",
                    vat.countOf(ModItems.SOY_PASTE_CHUNK.get()), VatRecipes.PASTE_CHUNKS));
        }
        // 酱渣：酿完之后缸里剩的东西，也是酿醋的引子
        if (vat.residueCount() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.residue", vat.residueCount()));
        }
        // 酿醋 / 酿白醋：写出谷物放了几份
        if (kind == VatRecipes.Kind.VINEGAR || kind == VatRecipes.Kind.WHITE_VINEGAR) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.vinegar_grain",
                    vat.vinegarGrainCount(), VatRecipes.VINEGAR_GRAIN_COUNT));
        }
        // 辣白菜：红辣椒是必选，调味品数量按份数要求
        if (kind == VatRecipes.Kind.SPICY_PICKLE) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.spicy_seasoning",
                    vat.chiliSauceCount(),
                    vat.seasoningCount(),
                    VatRecipes.spicySeasoningNeed(water)));
        }
        // 生豆芽：写出缸里的黄豆 / 豆芽总数
        if (kind == VatRecipes.Kind.BEAN_SPROUTS) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.sprout_soybean",
                    vat.soybeanCount() + vat.countOf(ModItems.BEAN_SPROUTS.get()),
                    VatRecipes.SPROUT_SOYBEAN_MAX));
        }
        // 酸玉米粒：写出缸里的玉米粒 / 酸玉米粒总数
        if (kind == VatRecipes.Kind.SOUR_CORN) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.sour_corn_count",
                    vat.cornKernelCount() + vat.countOf(ModItems.SOUR_CORN_KERNELS.get()),
                    water * VatRecipes.SOUR_CORN_PER_WATER));
        }

        // 内容物图标
        if (!contents.isEmpty()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.dongbei_delight.vat.contents")));
            for (int i = 0; i < contents.size() && i < MAX_CONTENT_ICONS; i++) {
                line.add(helper.smallItem(contents.get(i)));
            }
            tooltip.add(line);
        }

        if (vat.isPressed()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.dongbei_delight.vat.press")));
            line.add(helper.smallItem(vat.press()));
            tooltip.add(line);
        }
        if (vat.isCovered()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.dongbei_delight.vat.cover")));
            line.add(helper.smallItem(vat.cover()));
            tooltip.add(line);
        }

        // 发酵进度条
        if (kind != VatRecipes.Kind.NONE) {
            float ratio = fermented ? 1.0F : progress / (float) Vat.MAX_PROGRESS;
            // 按大缸在做的东西换说法：泡菜「发酵」、腊肉「腌制」、大酱/酱油「酿造」
            String stage = switch (kind) {
                case MEAT -> "curing";
                case PASTE, SOY_SAUCE -> "brewing";
                case BEAN_SPROUTS -> "sprouting";
                default -> "fermenting";
            };
            Component text = fermented
                    ? Component.translatable("jade.dongbei_delight.vat.done." + stage)
                    : Component.translatable("jade.dongbei_delight.vat.progress." + stage,
                            Math.round(ratio * 100.0F));
            // 进度条上的文字用白色，避免默认的自动取色在浅色条上看不清
            ProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
            tooltip.add(helper.progress(ratio, text, style, BoxStyle.getNestedBox(), false));
        }

        // 成品份数（同时给出 mB，方便对着储罐估算）
        if (fermented && kind == VatRecipes.Kind.PASTE && vat.pasteMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.paste_amount",
                    vat.paste(), vat.pasteMb()));
        }
        if (fermented && kind == VatRecipes.Kind.SOY_SAUCE && vat.soySauceMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.soy_sauce_amount",
                    vat.soySauce(), vat.soySauceMb()));
        }
        if (fermented && kind == VatRecipes.Kind.VINEGAR && vat.vinegarMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.vinegar_amount",
                    vat.vinegar(), vat.vinegarMb()));
        }
        // 泡菜腌好后缸里那缸水变成了酸引水，可以装瓶也可以抽走
        if (fermented && (kind == VatRecipes.Kind.PICKLE || kind == VatRecipes.Kind.SPICY_PICKLE)
                && vat.sourWaterMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.sour_water_amount",
                    vat.sourWater(), vat.sourWaterMb()));
        }
        if (fermented && kind == VatRecipes.Kind.WHITE_VINEGAR && vat.whiteVinegarMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.white_vinegar_amount",
                    vat.whiteVinegar(), vat.whiteVinegarMb()));
        }
        if (fermented && kind == VatRecipes.Kind.FISH_SAUCE && vat.fishSauceMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.fish_sauce_amount",
                    vat.fishSauce(), vat.fishSauceMb()));
        }
        if (fermented && kind == VatRecipes.Kind.SHRIMP_PASTE && vat.shrimpPasteMb() > 0) {
            tooltip.add(Component.translatable("jade.dongbei_delight.vat.shrimp_paste_amount",
                    vat.shrimpPaste(), vat.shrimpPasteMb()));
        }
    }

    private static String kindKey(VatRecipes.Kind kind) {
        return "jade.dongbei_delight.vat.kind." + kind.name().toLowerCase(Locale.ROOT);
    }
}
