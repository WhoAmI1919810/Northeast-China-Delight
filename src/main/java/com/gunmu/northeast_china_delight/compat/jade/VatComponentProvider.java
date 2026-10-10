package com.gunmu.northeast_china_delight.compat.jade;

import com.gunmu.northeast_china_delight.block.ModBlockStateProperties;
import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
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
//? if <1.20.2 {
/*import snownee.jade.api.ui.IProgressStyle;
*///?} else {
import snownee.jade.api.ui.ProgressStyle;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * 准星指大缸时只显示三样：液面高度、内容物、发酵进度条。详细的投料需求（几份盐、几块酱块、几份谷物…）一律不在这里写 —— 那些看 JEI 的大缸配方页就行，Jade 里堆一堆文
 * 字反而看不清。
 */
public enum VatComponentProvider implements IBlockComponentProvider {

    INSTANCE;

    /** 一行最多画几个内容物图标，避免刷屏 */
    private static final int MAX_CONTENT_ICONS = 12;

    @Override
    public ResourceLocation getUid() {
        return NortheastJadePlugin.VAT_UID;
    }

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

        List<ItemStack> contents = vat.contents();
        if (kind == VatRecipes.Kind.NONE && water <= 0 && contents.isEmpty()
                && !vat.isPressed() && !vat.isCovered()) {
            return;
        }

        if (water > 0) {
            tooltip.add(Component.translatable("jade.northeast_china_delight.vat.water", water,
                    ModBlockStateProperties.VAT_MAX_WATER));
        }

        if (!contents.isEmpty()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.northeast_china_delight.vat.contents")));
            for (int i = 0; i < contents.size() && i < MAX_CONTENT_ICONS; i++) {
                line.add(helper.smallItem(contents.get(i)));
            }
            tooltip.add(line);
        }

        if (kind != VatRecipes.Kind.NONE) {
            float ratio = fermented ? 1.0F : progress / (float) Vat.MAX_PROGRESS;
            String stage = switch (kind) {
                case MEAT, SALTED_FISH -> "curing";
                case PASTE, SOY_SAUCE, FISH_SAUCE, SHRIMP_PASTE -> "brewing";
                case FROZEN_PEAR -> "making";
                case BEAN_SPROUTS -> "sprouting";
                default -> "fermenting";
            };
            Component text;
            if (fermented) {
                text = Component.translatable("jade.northeast_china_delight.vat.done." + stage);
            } else {
                boolean started = progress > 0 || com.gunmu.northeast_china_delight.crafting.VatBrewing
                        .ready(vat) != null;
                text = started
                        ? Component.translatable("jade.northeast_china_delight.vat.progress." + stage,
                                Math.round(ratio * 100.0F))
                        : Component.translatable("jade.northeast_china_delight.vat.waiting");
            }
            // 进度条上的文字用白色，避免默认的自动取色在浅色条上看不清
            //? if <1.20.2 {
            /*IProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
            tooltip.add(helper.progress(ratio, text, style, BoxStyle.DEFAULT, false));
            *///?} else {
            ProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
            tooltip.add(helper.progress(ratio, text, style, BoxStyle.getNestedBox(), false));
            //?}
        }

    }
}
