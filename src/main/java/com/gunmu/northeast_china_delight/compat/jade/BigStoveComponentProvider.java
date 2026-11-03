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
import snownee.jade.api.ui.IProgressStyle;
//?} else {
/*import snownee.jade.api.ui.ProgressStyle;
*///?}

import java.util.ArrayList;
import java.util.List;

/**
 * 准星指大灶（四格哪一格都行，锅的数据在西北那格上，见 {@link BigStoveBlock#potAt}）：灶火点没点、锅盖盖没盖、锅里多少液体、
 * 放了什么（图标），煮着时一条进度条，煮好了写成品、还剩几份、用什么盛。配方要什么料看 JEI，这里不写。
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
        if (!(state.getBlock() instanceof BigStoveBlock)) {
            return;
        }
        IElementHelper helper = IElementHelper.get();
        boolean lit = state.getValue(BigStoveBlock.LIT);
        BigStoveBlock.Pot potState = state.getValue(BigStoveBlock.POT);
        tooltip.add(Component.translatable(lit ? "jade.northeast_china_delight.big_stove.lit"
                : "jade.northeast_china_delight.big_stove.unlit"));
        if (potState == BigStoveBlock.Pot.NONE) {
            tooltip.add(Component.translatable("jade.northeast_china_delight.big_stove.no_pot"));
            return;
        }
        if (potState == BigStoveBlock.Pot.LID) {
            tooltip.add(Component.translatable("jade.northeast_china_delight.big_stove.lid"));
        }
        BigPotBlockEntity pot = BigStoveBlock.potAt(accessor.getLevel(), accessor.getPosition(), state);
        if (pot == null) {
            return;
        }

        if (pot.liquidMb() > 0) {
            tooltip.add(Component.translatable("jade.northeast_china_delight.big_pot.liquid",
                    pot.liquid().getFluidType().getDescription(), pot.liquidMb(), BigPotBlockEntity.LIQUID_CAPACITY));
        }

        if (pot.hasResult()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.done")));
            line.add(helper.smallItem(pot.result()));
            line.add(helper.text(pot.result().getHoverName()));
            tooltip.add(line);
            ItemStack container = pot.container();
            tooltip.add(container.isEmpty()
                    ? Component.translatable("jade.northeast_china_delight.big_pot.serve_by_hand", pot.servings())
                    : Component.translatable("jade.northeast_china_delight.big_pot.serve_with",
                            container.getHoverName(), pot.servings()));
            return;
        }

        List<ItemStack> contents = pot.contents();
        if (!contents.isEmpty()) {
            List<IElement> line = new ArrayList<>();
            line.add(helper.text(Component.translatable("jade.northeast_china_delight.big_pot.contents")));
            for (int i = 0; i < contents.size() && i < MAX_CONTENT_ICONS; i++) {
                line.add(helper.smallItem(contents.get(i)));
            }
            tooltip.add(line);
        }

        if (pot.cooking() != null && pot.cookTotal() > 0) {
            if (!lit) {
                tooltip.add(Component.translatable("jade.northeast_china_delight.big_pot.unlit"));
                return;
            }
            float ratio = Math.min(1.0F, pot.cookTicks() / (float) pot.cookTotal());
            Component text = Component.translatable("jade.northeast_china_delight.big_pot.cooking",
                    Math.round(ratio * 100.0F));
            // 进度条上的文字用白色，同大缸
            //? if <1.20.2 {
            IProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
            tooltip.add(helper.progress(ratio, text, style, BoxStyle.DEFAULT, false));
            //?} else {
            /*ProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
            tooltip.add(helper.progress(ratio, text, style, BoxStyle.getNestedBox(), false));
            *///?}
        } else if (!contents.isEmpty() || pot.liquidMb() > 0) {
            tooltip.add(Component.translatable(lit ? "jade.northeast_china_delight.big_pot.waiting"
                    : "jade.northeast_china_delight.big_pot.unlit"));
        }
    }
}
