package com.gunmu.northeast_china_delight.compat.jade;

import com.gunmu.northeast_china_delight.block.BigPotBlockEntity;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
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
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 准星指大灶（四格哪一格都行，锅的数据在西北那格上，见 {@link BigStoveBlock#potAt}）：投料阶段把食材（图标）和调料（只写名称）
 * 分开显示；盖上锅盖以后（3 秒反悔期、煮着、煮好了还没盛完）只显示一行「本次烹饪得到：菜名」，不显示投料、调料和进度
 * （2026-11-07 用户：「开煮后的 jade 以我为准」——原话「当开始烹饪时，不显示投料和调料，只显示‘本次烹饪得到：XX’」，
 * 「Jade 持续显示直到制作结束，烹饪完成也一直显示」）。别的（火、锅盖、水、怎么盛）不写。
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
            return;
        }
        List<ItemStack> ingredients = pot.ingredientStacks();
        if (!ingredients.isEmpty()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.contents")));
            for (int i = 0; i < ingredients.size() && i < MAX_CONTENT_ICONS; i++) {
                line.add(helper.smallItem(ingredients.get(i).copyWithCount(1)));
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
            tooltip.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.seasonings", names)));
        }
    }
}
