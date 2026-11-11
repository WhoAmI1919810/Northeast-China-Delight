package com.gunmu.northeast_china_delight.compat.jade;

import com.gunmu.chinese_regional_culinary_ingredients.block.BigPotBlockEntity;
import com.gunmu.chinese_regional_culinary_ingredients.block.BigStoveBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import snownee.jade.api.ui.IProgressStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * 准星指大灶（四格哪一格都行，锅的数据在西北那格上，见 {@link BigStoveBlock#potAt}）：投料阶段显示水量，把食材（图标）和
 * 调料（只写名称）分开显示；盖上锅盖以后（3 秒反悔期、煮着、煮好了还没盛完）显示「本次烹饪得到：菜名」和进度条
 * （百分比；走满了写「烹饪完成」），不再显示水量、投料和调料。别的（火、锅盖、怎么盛）不写。
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

        ItemStack currentDish = pot.hasResult() ? pot.result() : pot.displayDish();
        if (pot.hasResult() || pot.cooking() != null) {
            if (!currentDish.isEmpty()) {
                tooltip.add(helper.text(Component.translatable(
                        "jade.northeast_china_delight.big_pot.result", currentDish.getHoverName())));
            }
        } else {
            tooltip.add(helper.text(Component.translatable(
                    "jade.northeast_china_delight.big_pot.water", pot.liquidMb())));
            List<ItemStack> ingredients = pot.ingredientStacks();
            if (!ingredients.isEmpty()) {
                List<IElement> line = new ArrayList<>();
                line.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.contents")));
                for (int i = 0; i < ingredients.size() && i < MAX_CONTENT_ICONS; i++) {
                    if (i > 0) {
                        line.add(helper.spacer(2, 0));
                    }
                    // 图标后面写「×数量」（2026-11-09 用户：Jade 显示的投入食材要能显示投入的数量）；
                    // 半大的图标角上那个数字太小看不清，所以图标只画一个，数量单独写成字
                    ItemStack stack = ingredients.get(i);
                    line.add(helper.item(stack.copyWithCount(1), 0.5F));
                    line.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.count",
                            stack.getCount())));
                }
                tooltip.add(line);
            }
            List<ItemStack> seasonings = pot.seasoningStacks();
            if (!seasonings.isEmpty()) {
                MutableComponent names = Component.empty();
                for (int i = 0; i < seasonings.size(); i++) {
                    if (i > 0) {
                        names.append(Component.translatable("jade.northeast_china_delight.big_pot.separator"));
                    }
                    names.append(seasonings.get(i).getHoverName());
                }
                tooltip.add(helper.text(Component.translatable(
                        "jade.northeast_china_delight.big_pot.seasonings", names)));
            }
        }

        float ratio;
        if (pot.hasResult()) {
            ratio = 1.0F;
        } else if (pot.cooking() != null && pot.cookTotal() > 0) {
            ratio = pot.startDelay() > 0 ? 0.0F
                    : Math.min(1.0F, pot.cookProgress() / pot.cookTotal());
        } else {
            return;
        }
        Component text = ratio >= 1.0F
                ? Component.translatable("jade.northeast_china_delight.big_pot.done")
                : Component.translatable("jade.northeast_china_delight.big_pot.cooking", Math.round(ratio * 100.0F));
        IProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
        tooltip.add(helper.progress(ratio, text, style, BoxStyle.DEFAULT, false));
    }
}
