package com.gunmu.dongbei_delight.client;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.block.ModBlockStateProperties.VatContent;
import com.gunmu.dongbei_delight.block.Vat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 准星指向正在发酵的大缸时，在快捷栏上方显示一条发酵进度。
 *
 * 进度直接读方块状态（游戏会自动同步到客户端），因此不需要额外的网络包。
 * 后续大缸接入 GUI 后，这个 HUD 可以保留作为「看方块就知道进度」的便捷提示。
 */
@EventBusSubscriber(modid = DongbeiDelight.MODID, value = Dist.CLIENT)
public class VatHudOverlay {

    private static final int BAR_WIDTH = 90;
    private static final int BAR_HEIGHT = 6;
    /** 进度条距屏幕底边的高度，正好落在快捷栏上方 */
    private static final int BAR_BOTTOM_MARGIN = 58;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.options.hideGui || minecraft.level == null) {
            return;
        }

        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockState state = minecraft.level.getBlockState(((BlockHitResult) hit).getBlockPos());
        if (!(state.getBlock() instanceof Vat) || state.getValue(Vat.CONTENT) == VatContent.EMPTY) {
            return;
        }

        boolean finished = state.getValue(Vat.FERMENTED);
        float ratio = finished ? 1.0F : (float) state.getValue(Vat.PROGRESS) / Vat.MAX_PROGRESS;

        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - BAR_BOTTOM_MARGIN;
        int left = centerX - BAR_WIDTH / 2;

        // 外框、底槽、进度
        graphics.fill(left - 1, y - 1, left + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xC0000000);
        graphics.fill(left, y, left + BAR_WIDTH, y + BAR_HEIGHT, 0xFF2B1F14);
        graphics.fill(left, y, left + Math.round(BAR_WIDTH * ratio), y + BAR_HEIGHT,
                finished ? 0xFF63C13C : 0xFFD8A33A);

        Component label = Component.translatable(
                finished ? "hud.dongbei_delight.vat.ready" : "hud.dongbei_delight.vat.fermenting");
        graphics.drawCenteredString(minecraft.font, label, centerX, y - 12, 0xFFFFFF);
    }
}
