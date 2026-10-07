package com.gunmu.northeast_china_delight.event;

import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

/**
 * 临时调试输出：进度授予、方块放置、右键方块、大缸状态。
 * 调完之后整个文件可以删掉。
 */
public final class ModDebugEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ModDebugEvents() {
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LOGGER.info("[DBG-ADV] {} 获得进度 {}（位置 {}）",
                player.getName().getString(), event.getAdvancement().id(), player.blockPosition());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            LOGGER.info("[DBG-PLACE] {} 在 {} 放下了 {}", player.getName().getString(),
                    event.getPos(), event.getState().getBlock());
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getItemStack().isEmpty() == false) {
            LOGGER.info("[DBG-USE] {} 右键 {}（手持 {}，面 {}）",
                    player.getName().getString(),
                    event.getLevel().getBlockState(event.getPos()).getBlock(),
                    event.getItemStack().getItem(), event.getFace());
        }
        // 顺手把大缸的状态也打出来
        if (event.getLevel() instanceof ServerLevel level) {
            BlockPos pos = event.getPos();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VatBlockEntity vat) {
                logVat("右键", vat, level.getBlockState(pos).getValue(Vat.FERMENTED));
            }
        }
    }

    /** 大缸当前状态（kind / 水量 / 是否完成 / 内容物摘要） */
    public static void logVat(String why, VatBlockEntity vat, boolean fermented) {
        LOGGER.info("[DBG-VAT] {} kind={} 水={}mB 酸引水={}mB 完成={} 压石={} 盖={} 内容物={}",
                why, vat.kind(), vat.waterMb(), vat.sourWaterMb(), fermented,
                vat.press(), vat.cover(), vat.contents());
    }
}
