package com.gunmu.northeast_china_delight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * 唱片歌词显示：唱片机播放「大东北是我的家乡」时，
 * 在物品栏上方按时间轴滚动显示歌词。
 *
 * 客户端本地计时——唱片机发声时 vanilla 已经同步了声音到附近玩家，
 * 我们只需要在听到声音的那一刻开始计时即可。
 * 每 tick 检查 SoundInstance 是否还在播放，不在就立刻停歌词。
 */
public class LyricsHudOverlay {

    /** 歌词时间轴：(开始秒, 歌词翻译 key) */
    private static final List<LyricLine> LYRICS = List.of(
            new LyricLine(33.5f, "lyrics.northeast_china_delight.0"),
            new LyricLine(37.5f, "lyrics.northeast_china_delight.1"),
            new LyricLine(41.1f, "lyrics.northeast_china_delight.2"),
            new LyricLine(44.8f, "lyrics.northeast_china_delight.3"),
            new LyricLine(48.5f, "lyrics.northeast_china_delight.4"),
            new LyricLine(52.3f, "lyrics.northeast_china_delight.5"),
            new LyricLine(56.4f, "lyrics.northeast_china_delight.6"),
            new LyricLine(60.0f, "lyrics.northeast_china_delight.7"),
            new LyricLine(65.5f, "lyrics.northeast_china_delight.8"),
            new LyricLine(69.8f, "lyrics.northeast_china_delight.9"),
            new LyricLine(73.0f, "lyrics.northeast_china_delight.10"),
            new LyricLine(76.8f, "lyrics.northeast_china_delight.11"),
            new LyricLine(80.5f, "lyrics.northeast_china_delight.12"),
            new LyricLine(84.1f, "lyrics.northeast_china_delight.13"),
            new LyricLine(88.0f, "lyrics.northeast_china_delight.14"),
            new LyricLine(92.0f, "lyrics.northeast_china_delight.15")
    );

    /** 歌词消失前的淡出时间（秒） */
    private static final float FADE_OUT = 2.0f;
    /** 整个歌词 HUD 在歌曲结束后保留的时间（秒） */
    private static final float HUD_LINGER = 3.0f;
    /** 歌曲总时长（与 jukebox_song 里的 length_in_seconds 一致） */
    private static final float SONG_LENGTH = 116.3f;

    /** 当前活跃的唱片声音实例（由 mixin 捕获） */
    private static SoundInstance activeSound = null;
    private static boolean playing = false;
    private static long startTick = -1;
    private static int currentLyricIndex = -1;

    /** 由 SoundEngineMixin 调用：检测到我们唱片的声音开始播放 */
    public static void onRecordStarted(SoundInstance sound) {
        activeSound = sound;
        playing = true;
        startTick = -1; // 等下一次 render tick 初始化
        currentLyricIndex = -1;
    }

    /** 声音停止（唱片被取出/唱片机被破坏） */
    public static void onRecordStopped() {
        activeSound = null;
        playing = false;
        currentLyricIndex = -1;
    }



    /**
     * 每帧调用（RenderGuiEvent.Post），在物品栏上方渲染当前歌词。
     */
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!playing) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            playing = false;
            return;
        }

        // 初始化计时器（用 level tick 保证暂停时冻结）
        if (startTick < 0) {
            startTick = mc.level.getGameTime();
        }
        long elapsed = mc.level.getGameTime() - startTick;
        float elapsedSec = elapsed / 20.0f;

        // 歌曲结束后再留一会儿，然后自动停
        if (elapsedSec > SONG_LENGTH + HUD_LINGER) {
            onRecordStopped();
            return;
        }

        // 切到下一句前先确认声音还在（取出唱片/唱片机破坏时会停）
        if (activeSound != null && !mc.getSoundManager().isActive(activeSound)) {
            onRecordStopped();
            return;
        }

        // 找当前应显示的歌词行
        int targetIndex = -1;
        for (int i = LYRICS.size() - 1; i >= 0; i--) {
            if (elapsedSec >= LYRICS.get(i).startSec) {
                targetIndex = i;
                break;
            }
        }

        // 歌词还没开始（前奏）就隐藏
        if (targetIndex < 0) {
            currentLyricIndex = -1;
            return;
        }

        // 当前歌词行结束时间 = 下一行的开始时间
        float lineEnd = targetIndex < LYRICS.size() - 1
                ? LYRICS.get(targetIndex + 1).startSec
                : SONG_LENGTH;
        // 在最后 FADE_OUT 秒内淡出
        float fadeStart = lineEnd - FADE_OUT;
        float alpha = 1.0f;
        if (elapsedSec > fadeStart) {
            alpha = Math.max(0.0f, (lineEnd - elapsedSec) / FADE_OUT);
        }

        currentLyricIndex = targetIndex;
        String key = LYRICS.get(targetIndex).translationKey;
        Component text = Component.translatable(key);

        Font font = mc.font;
        int textWidth = font.width(text);
        int x = (screenWidth - textWidth) / 2;
        // 物品栏上方约 60px
        int y = screenHeight - 60;

        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        graphics.drawString(font, text, x, y, color, true);
    }

    public static boolean isPlaying() {
        return playing;
    }

    private record LyricLine(float startSec, String translationKey) {
    }
}