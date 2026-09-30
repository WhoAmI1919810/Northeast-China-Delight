package com.gunmu.dongbei_delight.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 东北小院：一次生成一整个「小院群落」。
 *
 * <h2>想改参数，只动数据包，不用改代码</h2>
 * 位置：{@code data/dongbei_delight/worldgen/structure/dongbei_yard.json}
 * <pre>
 *   biomes        —— 只改这一行就能换生成在哪些生物群系（指向一个生物群系标签，
 *                    标签写法见 data/dongbei_delight/tags/worldgen/biome/has_dongbei_yard.json）
 *   templates     —— 参与抽签的院子 NBT 列表，加一个新院子就往数组里加一行
 *   origin_chance —— 群体总开关，0.0~1.0，越小越稀有
 *   walk_chance   —— 第一户继续长新院的概率（默认 0.80，之后每户 -15%，最低 20%）
 *   step_min/max  —— 游走距离范围（默认 20~50 格）
 *   max_houses    —— 一个群落最多几户（默认 20）
 *   min_gap       —— 两个院子的最小间隔（格子数），防止院子叠在一起
 *   max_slope     —— 起点两侧地形高差超过这个数就不生成（防止糊在陡坡上）
 *   allow_flip    —— 是否允许院子朝南 / 朝北两种朝向
 * </pre>
 * 「多大概率刷」还有第二层：{@code worldgen/structure_set/dongbei_yards.json} 里的
 * {@code spacing}（每多少格试一次）和 {@code separation}（同格内最小间隔），
 * 这两个数越小越密。
 *
 * <p>生成规则（按需求实现）：每个院子在东西南北四个方向上各自掷一次，
 * 每个方向 20~50 格内随机取一个距离，有 30% 概率再生成一个院子；
 * 新院子重复同样的过程，直到掷不中、位置不合法、不是寒冷群系，或者到达 20 户上限。
 */
public class DongbeiCourtyardStructure extends Structure
{
    /** 默认 6 种院子（顺序即抽签概率） */
    public static final List<String> DEFAULT_TEMPLATES = List.of(
            "dongbei_delight:dongbei_yard_garden",
            "dongbei_delight:dongbei_yard_livestock",
            "dongbei_delight:dongbei_yard_corn",
            "dongbei_delight:dongbei_yard_compound",
            "dongbei_delight:dongbei_yard_cabin",
            "dongbei_delight:dongbei_yard_metal"
    );

    /** 模板里在院子地面以下多垫了几层土（见 tools/structure_gen，base_y=2） */
    public static final int TEMPLATE_BASE_Y = 2;
    /**
     * 模板里「院子地面（草方块）」在第几层：**第 0 层就是院子地面**，
     * 上面才是房子和家具 —— 见 tools/structure_gen/designs.py 的 GROUND/ON。
     */
    public static final int GROUND_LAYER_IN_TEMPLATE = 0;
    /** 游走时的横向随机偏移（格），让村子不像棋盘 */
    public static final int LATERAL_JITTER = 6;
    /** 门口前方检查范围：多宽、地形高差容忍几格 */
    public static final int GATE_CHECK_WIDTH = 18;
    public static final int MAX_GATE_SLOPE = 4;
    /** 黑土地每格替换概率（改这一个数就能调密度） */
    public static final float BLACK_SOIL_DENSITY = 0.12F;
    /**
     * 游走概率递减曲线：第 1 户 80%，之后每多一户降 15%，降到 20% 不再降
     * （80 / 65 / 50 / 35 / 20 / 20 / 20 …）。{@code walk_chance} 是起始概率。
     */
    public static final float WALK_CHANCE_STEP = 0.15F;
    public static final float WALK_CHANCE_MIN = 0.20F;

    private static final List<Direction> HORIZONTALS =
            List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);

    public static final MapCodec<DongbeiCourtyardStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.listOf().optionalFieldOf("templates", DEFAULT_TEMPLATES).forGetter(s -> s.templates),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("origin_chance", 0.85F).forGetter(s -> s.originChance),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("walk_chance", 0.80F).forGetter(s -> s.walkChance),
            Codec.intRange(1, 256).optionalFieldOf("step_min", 20).forGetter(s -> s.stepMin),
            Codec.intRange(1, 256).optionalFieldOf("step_max", 50).forGetter(s -> s.stepMax),
            Codec.intRange(1, 64).optionalFieldOf("max_houses", 20).forGetter(s -> s.maxHouses),
            Codec.intRange(-8, 64).optionalFieldOf("min_gap", 1).forGetter(s -> s.minGap),
            Codec.intRange(0, 32).optionalFieldOf("max_slope", 6).forGetter(s -> s.maxSlope),
            Codec.BOOL.optionalFieldOf("allow_flip", true).forGetter(s -> s.allowFlip),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("black_soil_chance", 0.35F).forGetter(s -> s.blackSoilChance),
            Codec.intRange(0, 64).optionalFieldOf("black_soil_radius", 18).forGetter(s -> s.blackSoilRadius),
            Codec.STRING.optionalFieldOf("step_measure", "gap").forGetter(s -> s.stepMeasure),
            Codec.intRange(0, 64).optionalFieldOf("clear_radius", 20).forGetter(s -> s.clearRadius),
            Codec.intRange(4, 96).optionalFieldOf("clear_above", 30).forGetter(s -> s.clearAbove),
            Codec.intRange(0, 48).optionalFieldOf("gate_check_depth", 12).forGetter(s -> s.gateCheckDepth)
    ).apply(instance, DongbeiCourtyardStructure::new));

    private final List<String> templates;
    private final float originChance;
    private final float walkChance;
    private final int stepMin;
    private final int stepMax;
    private final int maxHouses;
    private final int minGap;
    private final int maxSlope;
    private final boolean allowFlip;
    private final float blackSoilChance;
    private final int blackSoilRadius;
    private final String stepMeasure;
    private final int clearRadius;
    private final int clearAbove;
    private final int gateCheckDepth;

    public DongbeiCourtyardStructure(StructureSettings settings, List<String> templates, float originChance,
                                     float walkChance, int stepMin, int stepMax, int maxHouses, int minGap,
                                     int maxSlope, boolean allowFlip, float blackSoilChance,
                                     int blackSoilRadius, String stepMeasure,
                                     int clearRadius, int clearAbove, int gateCheckDepth)
    {
        super(settings);
        this.templates = templates.isEmpty() ? DEFAULT_TEMPLATES : templates;
        this.originChance = originChance;
        this.walkChance = walkChance;
        this.stepMin = Math.min(stepMin, stepMax);
        this.stepMax = Math.max(stepMin, stepMax);
        this.maxHouses = maxHouses;
        this.minGap = minGap;
        this.maxSlope = maxSlope;
        this.allowFlip = allowFlip;
        this.blackSoilChance = blackSoilChance;
        this.blackSoilRadius = blackSoilRadius;
        // gap = 两个院子之间的空隙（默认，院子大了也不会挤在一起）
        // center = 两个院子原点的直线距离（旧行为）
        this.stepMeasure = "center".equalsIgnoreCase(stepMeasure) ? "center" : "gap";
        this.clearRadius = clearRadius;
        this.clearAbove = clearAbove;
        this.gateCheckDepth = gateCheckDepth;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context)
    {
        WorldgenRandom random = context.random();
        if (random.nextFloat() > this.originChance)
        {
            return Optional.empty();
        }
        int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
        int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);

        List<Yard> yards = this.growCluster(context, random, x, z);
        if (yards.isEmpty())
        {
            return Optional.empty();
        }
        BlockPos origin = yards.get(0).pos();
        return Optional.of(new GenerationStub(origin, builder -> {
            // 先把所有院子的清场件加进去，再加院子本体 ——
            // 不然相邻院子的清场会把这一户院里的大树也清掉。
            for (Yard yard : yards)
            {
                if (this.clearRadius <= 0 && this.clearAbove <= 0)
                {
                    break;
                }
                StructureTemplate template = context.structureTemplateManager()
                        .getOrCreate(ResourceLocation.parse(yard.template()));
                builder.addPiece(new YardClearingPiece(yard.pos(),
                        template.getSize().getX(), template.getSize().getZ(),
                        yard.pos().getY() + GROUND_LAYER_IN_TEMPLATE,
                        this.clearRadius, this.clearAbove));
            }
            for (Yard yard : yards)
            {
                StructureTemplate yardTemplate = context.structureTemplateManager()
                        .getOrCreate(ResourceLocation.parse(yard.template()));
                builder.addPiece(new CourtyardPiece(context.structureTemplateManager(),
                        yard.template(), yard.pos(), yard.rotation()));
                // 黑土地：这个院子周围撒一片沃土（不一定是每户都有）
                if (yard.blackSoil() && this.blackSoilRadius > 0)
                {
                    BlockPos center = yard.pos().offset(
                            yardTemplate.getSize().getX() / 2, 0, yardTemplate.getSize().getZ() / 2);
                    builder.addPiece(new BlackSoilPiece(center, this.blackSoilRadius, BLACK_SOIL_DENSITY));
                }
            }
        }));
    }

    /** 游走：从第一户开始，四个方向各自掷概率、各自随机距离，长到上限为止。 */
    private List<Yard> growCluster(GenerationContext context, WorldgenRandom random, int startX, int startZ)
    {
        List<Yard> yards = new ArrayList<>();
        Map<String, StructureTemplate> sizes = new HashMap<>();
        Yard first = this.tryMakeYard(context, random, sizes,
                this.pickTemplate(random), startX, startZ);
        if (first == null)
        {
            return List.of();
        }
        yards.add(first);
        ArrayDeque<Yard> queue = new ArrayDeque<>();
        queue.add(first);

        while (!queue.isEmpty())
        {
            Yard current = queue.poll();
            for (Direction direction : HORIZONTALS)
            {
                if (yards.size() >= this.maxHouses)
                {
                    return yards;
                }
                if (random.nextFloat() >= this.walkChanceFor(yards))
                {
                    // 这个方向没掷中就到此为止
                    continue;
                }
                int distance = this.stepMin + random.nextInt(this.stepMax - this.stepMin + 1);
                String template = this.pickTemplate(random);
                int x;
                int z;
                if ("center".equals(this.stepMeasure))
                {
                    x = current.pos().getX() + direction.getStepX() * distance;
                    z = current.pos().getZ() + direction.getStepZ() * distance;
                }
                else
                {
                    // 空隙模式：先按当前院子的尺寸往外挪，再留出距离，最后补上新院子自己的尺寸
                    StructureTemplate currentSize = sizes.get(current.template());
                    StructureTemplate nextSize = sizes.computeIfAbsent(template,
                            id -> context.structureTemplateManager().getOrCreate(ResourceLocation.parse(id)));
                    int lateral = LATERAL_JITTER > 0
                            ? random.nextInt(LATERAL_JITTER * 2 + 1) - LATERAL_JITTER : 0;
                    int stepX = direction.getStepX();
                    int stepZ = direction.getStepZ();
                    if (stepX > 0)
                    {
                        x = current.pos().getX() + currentSize.getSize().getX() + distance;
                    }
                    else if (stepX < 0)
                    {
                        x = current.pos().getX() - nextSize.getSize().getX() - distance;
                    }
                    else
                    {
                        x = current.pos().getX() + lateral;
                    }
                    if (stepZ > 0)
                    {
                        z = current.pos().getZ() + currentSize.getSize().getZ() + distance;
                    }
                    else if (stepZ < 0)
                    {
                        z = current.pos().getZ() - nextSize.getSize().getZ() - distance;
                    }
                    else
                    {
                        z = current.pos().getZ() + lateral;
                    }
                }
                Yard candidate = this.tryMakeYard(context, random, sizes, template, x, z);
                if (candidate == null || this.overlaps(candidate, yards, sizes))
                {
                    // 位置不合法 / 不是寒冷群系 / 和别的院子挤在一起 —— 这条支路终止
                    continue;
                }
                yards.add(candidate);
                queue.add(candidate);
            }
        }
        return yards;
    }

    /**
     * 判断一个候选位置能不能放院子：地表不能是水、坡度不能太陡、
     * 生物群系必须还是数据包里配置的那套（默认就是寒冷群系那堆）。
     */
    private String pickTemplate(WorldgenRandom random)
    {
        return this.templates.get(random.nextInt(this.templates.size()));
    }

    /** 第 n 户的游走概率：起始 80%，每户降 15%，到 20% 封底 */
    private float walkChanceFor(List<Yard> placed)
    {
        float chance = this.walkChance - WALK_CHANCE_STEP * Math.max(0, placed.size() - 1);
        return Math.max(WALK_CHANCE_MIN, chance);
    }

    private Yard tryMakeYard(GenerationContext context, WorldgenRandom random,
                            Map<String, StructureTemplate> sizes, String template, int x, int z)
    {
        int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
        if (surface != floor)
        {
            // 水面 / 水下，不要
            return null;
        }
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight();
        if (surface < minY + 12 || surface > maxY - 32)
        {
            return null;
        }
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(surface), QuartPos.fromBlock(z),
                context.randomState().sampler());
        if (!context.validBiome().test(biome))
        {
            return null;
        }
        if (!this.slopeOk(context, x, z, surface))
        {
            return null;
        }
        if (!this.footprintOk(context, template, x, z, surface))
        {
            // 院子这块地被山体埋住 / 悬在崖上，放弃
            return null;
        }
        sizes.computeIfAbsent(template, id -> context.structureTemplateManager().getOrCreate(ResourceLocation.parse(id)));
        boolean blackSoil = this.blackSoilChance > 0.0F && random.nextFloat() < this.blackSoilChance;
        // 院子的草地要对齐地形表面：surface 是「地表之上的空气层」，
        // 所以再往下压一格，模板里的垫土全埋进地形，不会露出一圈土基。
        BlockPos pos = new BlockPos(x, surface - GROUND_LAYER_IN_TEMPLATE - 1, z);
        // 朝向：优先挑「门口不是悬崖/水面/墙」的那一面
        Rotation rotation = this.pickRotation(context, template, pos, surface, random);
        if (rotation == null)
        {
            return null;
        }
        return new Yard(template, pos, rotation, blackSoil);
    }

    /**
     * 挑院子朝向：南向（NONE）和北向（CLOCKWISE_180）里选一个门口能走人的。
     * 门口前方一段距离内不能是水面，地形高差也不能太大（悬崖、石壁都算不合格）。
     * 两个方向都不行就返回 null —— 这一支游走到此为止。
     */
    private Rotation pickRotation(GenerationContext context, String template, BlockPos pos,
                                  int surface, WorldgenRandom random)
    {
        boolean[] preferFlip = {random.nextBoolean()};
        Rotation first = (this.allowFlip && preferFlip[0]) ? Rotation.CLOCKWISE_180 : Rotation.NONE;
        Rotation second = first == Rotation.NONE ? Rotation.CLOCKWISE_180 : Rotation.NONE;
        if (!this.allowFlip || this.gateCheckDepth <= 0)
        {
            return first;
        }
        if (this.gateFrontOk(context, template, pos, first, surface))
        {
            return first;
        }
        if (this.gateFrontOk(context, template, pos, second, surface))
        {
            return second;
        }
        return null;
    }

    /** 门口前方那块地能不能走人 */
    private boolean gateFrontOk(GenerationContext context, String template, BlockPos pos,
                                Rotation rotation, int yardSurface)
    {
        StructureTemplate t = context.structureTemplateManager().getOrCreate(ResourceLocation.parse(template));
        int sizeX = t.getSize().getX();
        int sizeZ = t.getSize().getZ();
        boolean flipped = rotation == Rotation.CLOCKWISE_180;
        int centerX = pos.getX() + sizeX / 2;
        int edgeZ = flipped ? pos.getZ() : pos.getZ() + sizeZ - 1;
        int step = flipped ? -1 : 1;
        for (int dx = -GATE_CHECK_WIDTH / 2; dx <= GATE_CHECK_WIDTH / 2; dx += 2)
        {
            for (int dz = 2; dz <= this.gateCheckDepth; dz += 2)
            {
                int x = centerX + dx;
                int z = edgeZ + step * dz;
                int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState());
                if (surface != floor)
                {
                    // 门口是水面/水下，不合格
                    return false;
                }
                if (Math.abs(surface - yardSurface) > MAX_GATE_SLOPE)
                {
                    // 门口是悬崖或高墙，不合格
                    return false;
                }
            }
        }
        return true;
    }

    /** 地形高差检查：院子四角高差太大就放弃（免得一半悬空）。 */
    private boolean slopeOk(GenerationContext context, int x, int z, int surface)
    {
        int reach = 8;
        int a = context.chunkGenerator().getBaseHeight(x - reach, z - reach, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int b = context.chunkGenerator().getBaseHeight(x + reach, z + reach, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int c = context.chunkGenerator().getBaseHeight(x - reach, z + reach, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int d = context.chunkGenerator().getBaseHeight(x + reach, z - reach, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int high = Math.max(Math.max(a, b), Math.max(c, d));
        int low = Math.min(Math.min(a, b), Math.min(c, d));
        return (high - low) <= this.maxSlope && Math.abs(surface - (high + low) / 2) <= this.maxSlope + 4;
    }

    /**
     * 院子整块地的地形检查：四个角 + 中心的地表高度都不能高过院子地面（否则院子被山埋住），
     * 也不能低太多（否则院子悬在崖边）。
     */
    private boolean footprintOk(GenerationContext context, String template, int x, int z, int surface)
    {
        StructureTemplate t = context.structureTemplateManager().getOrCreate(ResourceLocation.parse(template));
        int sizeX = t.getSize().getX();
        int sizeZ = t.getSize().getZ();
        int[][] probes = {
                {x, z}, {x + sizeX - 1, z}, {x, z + sizeZ - 1}, {x + sizeX - 1, z + sizeZ - 1},
                {x + sizeX / 2, z + sizeZ / 2}
        };
        for (int[] probe : probes)
        {
            int h = context.chunkGenerator().getBaseHeight(probe[0], probe[1], Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            if (h > surface + 1 || h < surface - 5)
            {
                return false;
            }
        }
        return true;
    }

    /** 用模板的真实尺寸判断两个院子是否挤在一起（留 minGap 格缝）。 */
    private boolean overlaps(Yard candidate, List<Yard> yards, Map<String, StructureTemplate> sizes)
    {
        StructureTemplate a = sizes.get(candidate.template());
        int aw = a.getSize().getX();
        int ad = a.getSize().getZ();
        for (Yard other : yards)
        {
            StructureTemplate b = sizes.get(other.template());
            int bw = b.getSize().getX();
            int bd = b.getSize().getZ();
            int overlapX = Math.min(candidate.pos().getX() + aw, other.pos().getX() + bw)
                    - Math.max(candidate.pos().getX(), other.pos().getX());
            int overlapZ = Math.min(candidate.pos().getZ() + ad, other.pos().getZ() + bd)
                    - Math.max(candidate.pos().getZ(), other.pos().getZ());
            if (overlapX > -this.minGap && overlapZ > -this.minGap)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public StructureType<?> type()
    {
        return ModWorldGen.COURTYARD_TYPE.get();
    }

    /** 一个院子的落点 */
    public record Yard(String template, BlockPos pos, Rotation rotation, boolean blackSoil)
    {
    }
}
