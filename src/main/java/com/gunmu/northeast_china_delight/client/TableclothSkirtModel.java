package com.gunmu.northeast_china_delight.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
//? if <1.20.2 {
/*import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
*///?} else {
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 格子桌布的方块模型包一层，修垂下来的布边发黑（2026-11-05 用户截图：桌布斜上方放了方块，那一侧的布边变成全黑）。
 *
 * <p>布边（gingham_tablecloth/skirt_*、corner_*）伸出桌布这一格、垂在下面那块桌子的侧面外。桌布模型关了平滑光照
 * （伸出格子的模型开平滑光照会外推出黑影，2026-11-04），走原版的平面光照：一个面要是正好平贴在格子边界上或边界外
 * （ModelBlockRenderer#calculateShape 判的），就取「朝向那一格」的光，比如东边布边的外侧面取的是和桌布同一层东边那格的光，
 * 那格放了泥土这种不透光的方块光照就是 0，整片布边全黑。布边底面同理，取的是下面桌子那格，也是黑的。</p>
 *
 * <p>这里把伸到桌布这一格下面去的面（布边、拐角的所有面）挪一个角 1/10000 格（肉眼看不出来），面就不算平贴边界，
 * 原版改取桌布自己这一格的光——和桌面一个亮度。挪过的面按原来那个面缓存，同一个面只算一次。</p>
 */
public class TableclothSkirtModel extends BakedModelWrapper<BakedModel> {

    /** 挪多少（方块单位，1/10000 格 = 0.0016 像素） */
    private static final float NUDGE = 1.0E-4F;
    /** 面顶点里低于这个高度就算伸到桌布这一格下面去了（布边、拐角从 y -9 像素垂到 0.5 像素） */
    private static final float BELOW = -1.0E-4F;

    private final Map<BakedQuad, BakedQuad> nudged = new ConcurrentHashMap<>();

    public TableclothSkirtModel(BakedModel original) {
        super(original);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return nudge(super.getQuads(state, side, rand));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        return nudge(super.getQuads(state, side, rand, extraData, renderType));
    }

    private List<BakedQuad> nudge(List<BakedQuad> quads) {
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            BakedQuad moved = nudged.computeIfAbsent(quad, TableclothSkirtModel::nudgeQuad);
            if (moved != quad) {
                if (out == null) {
                    out = new ArrayList<>(quads);
                }
                out.set(i, moved);
            }
        }
        return out == null ? quads : out;
    }

    /** 伸到下面去的面：第一个顶点沿面的法线方向挪 {@link #NUDGE}；别的面原样返回 */
    private static BakedQuad nudgeQuad(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        boolean below = false;
        for (int i = 0; i < 4; i++) {
            if (Float.intBitsToFloat(vertices[i * stride + 1]) < BELOW) {
                below = true;
                break;
            }
        }
        if (!below) {
            return quad;
        }
        int[] copy = vertices.clone();
        // 顶点里前三个 int 是 x、y、z（方块格式每个顶点 8 个 int）；轴的序号 X 0、Y 1、Z 2 正好对上
        int axis = quad.getDirection().getAxis().ordinal();
        copy[axis] = Float.floatToRawIntBits(Float.intBitsToFloat(copy[axis]) + NUDGE);
        return new BakedQuad(copy, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }
}
