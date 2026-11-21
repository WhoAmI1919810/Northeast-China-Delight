package com.gunmu.northeast_china_delight.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版 {@code ServerLevel#tickChunk} 的降雪逻辑只对 {@code MOTION_BLOCKING} 高度图的落点下方一格
 * 调用 {@code handlePrecipitation}：对无碰撞的灌木／参苗（{@link BushBlock}）自身永远不会触发，
 * 树冠树叶下方的植株也会被树叶挡住。这里在降雪／降雨时沿 {@code MOTION_BLOCKING_NO_LEAVES}
 * 补发一次降水回调，使露天与树荫下的生长期／成熟期人参和榛子丛都能自然覆雪。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelPrecipitationMixin
{
    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void northeast_china_delight$precipitateOnSnowyBushes(LevelChunk chunk, int randomTickSpeed,
                                                                  CallbackInfo ci)
    {
        ServerLevel self = (ServerLevel) (Object) this;
        if (!self.isRaining())
        {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int attempts = Math.max(1, Math.min(randomTickSpeed, 256));
        for (int i = 0; i < attempts; i++)
        {
            if (self.getRandom().nextInt(16) != 0)
            {
                continue;
            }
            BlockPos randomCol = self.getBlockRandomPos(minX, 0, minZ, 15);
            BlockPos surfacePos = self.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, randomCol);
            if (!self.isAreaLoaded(surfacePos, 1))
            {
                continue;
            }
            BlockState state = self.getBlockState(surfacePos);
            if (!(state.getBlock() instanceof BushBlock) || !state.hasProperty(BlockStateProperties.SNOWY))
            {
                continue;
            }
            Biome.Precipitation precipitation = self.getBiome(surfacePos).value().getPrecipitationAt(surfacePos);
            if (precipitation == Biome.Precipitation.SNOW)
            {
                if (!state.getValue(BlockStateProperties.SNOWY)
                        && self.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT) > 0
                        && self.getBrightness(LightLayer.BLOCK, surfacePos) < 10)
                {
                    state.getBlock().handlePrecipitation(state, self, surfacePos, precipitation);
                }
            }
            else if (precipitation == Biome.Precipitation.RAIN && state.getValue(BlockStateProperties.SNOWY))
            {
                state.getBlock().handlePrecipitation(state, self, surfacePos, precipitation);
            }
        }
    }
}
