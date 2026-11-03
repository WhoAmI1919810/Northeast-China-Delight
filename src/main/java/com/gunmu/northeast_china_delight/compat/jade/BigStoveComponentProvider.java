package com.gunmu.northeast_china_delight.compat.jade;

import com.gunmu.northeast_china_delight.block.BigPotBlockEntity;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
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
 * 准星指大灶（四格哪一格都行，锅的数据在西北那格上，见 {@link BigStoveBlock#potAt}）：只显示两样——放进锅里的料（图标）
 * 和烹饪进度条（百分比；走满了写「烹饪完成」）。别的（火、锅盖、水、怎么盛）不写，2026-11-03 用户嫌多。
 * 进度每秒从服务端同步一次（{@link BigPotBlockEntity#tickCooking}）。
 */
public enum BigStoveComponentProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final int MAX_CONTENT_ICONS = 9;

    @Override
    public ResourceLocation getUid() {
        return NortheastJadePlugin.BIG_STOVE_UID;
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY + 100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (!(state.getBlock() instanceof BigStoveBlock) || state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.NONE) {
            return;
        }
        BigPotBlockEntity pot = BigStoveBlock.potAt(accessor.getLevel(), accessor.getPosition(), state);
        if (pot == null) {
            return;
        }
        IElementHelper helper = IElementHelper.get();

        List<ItemStack> contents = pot.contents();
        if (!contents.isEmpty()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.contents")));
            for (int i = 0; i < contents.size() && i < MAX_CONTENT_ICONS; i++) {
                line.add(helper.smallItem(contents.get(i)));
            }
            tooltip.add(line);
        }

        float ratio;
        if (pot.hasResult()) {
            ratio = 1.0F;
        } else if (pot.cooking() != null && pot.cookTotal() > 0) {
            ratio = Math.min(1.0F, pot.cookProgress() / pot.cookTotal());
        } else {
            return;
        }
        // 走满了写「烹饪完成」，不写「烹饪中 100%」
        Component text = ratio >= 1.0F
                ? Component.translatable("jade.northeast_china_delight.big_pot.done")
                : Component.translatable("jade.northeast_china_delight.big_pot.cooking", Math.round(ratio * 100.0F));
        // 进度条上的文字用白色，同大缸
        //? if <1.20.2 {
        /*IProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
        tooltip.add(helper.progress(ratio, text, style, BoxStyle.DEFAULT, false));
        *///?} else {
        ProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
        tooltip.add(helper.progress(ratio, text, style, BoxStyle.getNestedBox(), false));
        //?}
    }
}
