package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.client.LyricsHudOverlay;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 SoundEngine.play：检测到我们唱片的声音开始播放时通知 LyricsHudOverlay。
 * 停止检测由 LyricsHudOverlay.tick() 完成（每 tick 检查声音是否还在）。
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    private static final ResourceLocation NORTHEAST_SOUND =
            com.gunmu.northeast_china_delight.util.DdIds.of("northeast_china_delight", "music_disc.northeast");

    @Inject(method = "play", at = @At("HEAD"))
    private void onPlay(SoundInstance sound, CallbackInfo ci) {
        if (sound.getLocation().equals(NORTHEAST_SOUND)) {
            LyricsHudOverlay.onRecordStarted(sound);
        }
    }
}