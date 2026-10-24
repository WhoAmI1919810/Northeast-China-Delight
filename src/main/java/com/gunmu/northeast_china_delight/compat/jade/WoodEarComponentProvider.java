package com.gunmu.northeast_china_delight.compat.jade;

import com.gunmu.northeast_china_delight.block.WoodEarCropBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
//? if <1.20.2 {
/*import snownee.jade.api.ui.IProgressStyle;
*///?} else {
import snownee.jade.api.ui.ProgressStyle;
//?}

/** 段木木耳的生长阶段；Jade 里用进度条显示，成熟时直接提示可以收获。 */
public enum WoodEarComponentProvider implements IBlockComponentProvider
{
    INSTANCE;

    @Override
    public ResourceLocation getUid()
    {
        return NortheastJadePlugin.WOOD_EAR_UID;
    }

    @Override
    public int getDefaultPriority()
    {
        return TooltipPosition.BODY + 100;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
    {
        BlockState state = accessor.getBlockState();
        if (!state.hasProperty(WoodEarCropBlock.AGE))
        {
            return;
        }

        int age = state.getValue(WoodEarCropBlock.AGE);
        boolean mature = age >= WoodEarCropBlock.MAX_AGE;
        float ratio = age / (float) WoodEarCropBlock.MAX_AGE;
        Component text = mature
                ? Component.translatable("jade.northeast_china_delight.wood_ear.mature")
                : Component.translatable("jade.northeast_china_delight.wood_ear.progress",
                        Math.round(ratio * 100.0F));
        IElementHelper helper = IElementHelper.get();
        //? if <1.20.2 {
        /*IProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
        tooltip.add(helper.progress(ratio, text, style, BoxStyle.DEFAULT, false));
        *///?} else {
        ProgressStyle style = helper.progressStyle().textColor(0xFFFFFF);
        tooltip.add(helper.progress(ratio, text, style, BoxStyle.getNestedBox(), false));
        //?}
    }
}
