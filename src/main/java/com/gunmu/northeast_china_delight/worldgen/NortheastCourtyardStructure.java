package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
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
 * 位置：{@code data/northeast_china_delight/worldgen/structure/northeast_yard.json}
 * <pre>
 *   biomes        —— 只改这一行就能换生成在哪些生物群系（指向一个生物群系标签，
 *                    标签写法见 data/northeast_china_delight/tags/worldgen/biome/has_northeast_yard.json）
 *   templates     —— 参与抽签的院子 NBT 列表，加一个新院子就往数组里加一行
 *   origin_chance —— 群体总开关，0.0~1.0，越小越稀有
 *   walk_chance   —— 第一户继续长新院的概率（默认 0.80，之后每户 -15%，最低 20%）
 *   step_min/max  —— 游走距离范围（默认 20~50 格）
 *   max_houses    —— 一个群落最多几户（默认不限，游走概率自己兜底）
 *   min_gap       —— 两个院子的最小间隔（格子数），防止院子叠在一起
 *   max_slope     —— 起点两侧地形高差超过这个数就不生成（防止糊在陡坡上）
 *   allow_flip    —— 是否允许院子朝南 / 朝北两种朝向
 * </pre>
 * 「多大概率刷」还有第二层：{@code worldgen/structure_set/northeast_yards.json} 里的
 * {@code spacing}（每多少格试一次）和 {@code separation}（同格内最小间隔），
 * 这两个数越小越密。
 *
 * <p>生成规则（按需求实现）：每个院子在东西南北四个方向上各自掷一次，
 * 每个方向 20~50 格内随机取一个距离，有 30% 概率再生成一个院子；
 * 新院子重复同样的过程，直到掷不中、位置不合法、不是寒冷群系，或者到达 20 户上限。
 */
public class NortheastCourtyardStructure extends Structure
{
    public static final List<String> DEFAULT_TEMPLATES = List.of(
            "northeast_china_delight:northeast_yard_garden",
            "northeast_china_delight:northeast_yard_compound",
            "northeast_china_delight:northeast_yard_livestock"
    );

    public static final List<String> DEFAULT_AVOID_STRUCTURES = List.of(
            "minecraft:village_plains",
            "minecraft:village_desert",
            "minecraft:village_savanna",
            "minecraft:village_snowy",
            "minecraft:village_taiga"
    );

    public static final int TEMPLATE_BASE_Y = 4;
    public static final int GROUND_LAYER_IN_TEMPLATE = TEMPLATE_BASE_Y;
    public static final int LATERAL_JITTER = 6;
    public static final int GATE_CHECK_WIDTH = 18;
    public static final int MAX_GATE_SLOPE = 4;
    public static final float BLACK_SOIL_DENSITY = 0.12F;
    public static final float BIOME_SOIL_DENSITY = 0.25F;

    public record ClearSettings(int radius, int above)
    {
        public static final Codec<ClearSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 64).optionalFieldOf("radius", 20).forGetter(ClearSettings::radius),
                Codec.intRange(4, 96).optionalFieldOf("above", 30).forGetter(ClearSettings::above)
        ).apply(i, ClearSettings::new));
    }

    public record AvoidSettings(List<String> structures, int margin)
    {
        public static final Codec<AvoidSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("structures", DEFAULT_AVOID_STRUCTURES)
                        .forGetter(AvoidSettings::structures),
                Codec.intRange(0, 256).optionalFieldOf("margin", 64).forGetter(AvoidSettings::margin)
        ).apply(i, AvoidSettings::new));

        public static final AvoidSettings DEFAULT = new AvoidSettings(DEFAULT_AVOID_STRUCTURES, 64);
    }
    public static final float WALK_CHANCE_STEP = 0.15F;
    public static final float WALK_CHANCE_MIN = 0.20F;
    public static final int WATER_CLEARANCE = 15;

    private static final List<Direction> HORIZONTALS =
            List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);

    public static final MapCodec<NortheastCourtyardStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.listOf().optionalFieldOf("templates", DEFAULT_TEMPLATES).forGetter(s -> s.templates),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("origin_chance", 0.85F).forGetter(s -> s.originChance),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("walk_chance", 0.80F).forGetter(s -> s.walkChance),
            Codec.intRange(1, 256).optionalFieldOf("step_min", 20).forGetter(s -> s.stepMin),
            Codec.intRange(1, 256).optionalFieldOf("step_max", 50).forGetter(s -> s.stepMax),
            Codec.INT.optionalFieldOf("max_houses", Integer.MAX_VALUE).forGetter(s -> s.maxHouses),
            Codec.intRange(-8, 64).optionalFieldOf("min_gap", 1).forGetter(s -> s.minGap),
            Codec.intRange(0, 32).optionalFieldOf("max_slope", 6).forGetter(s -> s.maxSlope),
            Codec.BOOL.optionalFieldOf("allow_flip", true).forGetter(s -> s.allowFlip),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("black_soil_chance", 0.35F).forGetter(s -> s.blackSoilChance),
            Codec.intRange(0, 64).optionalFieldOf("black_soil_radius", 18).forGetter(s -> s.blackSoilRadius),
            Codec.STRING.optionalFieldOf("step_measure", "gap").forGetter(s -> s.stepMeasure),
            ClearSettings.CODEC.optionalFieldOf("clear", new ClearSettings(20, 30))
                    .forGetter(s -> new ClearSettings(s.clearRadius, s.clearAbove)),
            Codec.intRange(0, 48).optionalFieldOf("gate_check_depth", 12).forGetter(s -> s.gateCheckDepth),
            AvoidSettings.CODEC.optionalFieldOf("avoid", AvoidSettings.DEFAULT)
                    .forGetter(s -> new AvoidSettings(s.avoidStructures, s.avoidMargin))
    ).apply(instance, NortheastCourtyardStructure::new));

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
    private final List<String> avoidStructures;
    private final int avoidMargin;

    public NortheastCourtyardStructure(StructureSettings settings, List<String> templates, float originChance,
                                     float walkChance, int stepMin, int stepMax, int maxHouses, int minGap,
                                     int maxSlope, boolean allowFlip, float blackSoilChance,
                                     int blackSoilRadius, String stepMeasure,
                                     ClearSettings clear, int gateCheckDepth, AvoidSettings avoid)
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
        this.stepMeasure = "center".equalsIgnoreCase(stepMeasure) ? "center" : "gap";
        this.clearRadius = clear.radius();
        this.clearAbove = clear.above();
        this.gateCheckDepth = gateCheckDepth;
        this.avoidStructures = avoid.structures();
        this.avoidMargin = avoid.margin();
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
            for (Yard yard : yards)
            {
                if (this.clearRadius <= 0 && this.clearAbove <= 0)
                {
                    break;
                }
                StructureTemplate template = context.structureTemplateManager()
                        .getOrCreate(DdIds.parse(yard.template()));
                builder.addPiece(new YardClearingPiece(yard.pos(),
                        template.getSize().getX(), template.getSize().getZ(),
                        yard.pos().getY() + GROUND_LAYER_IN_TEMPLATE,
                        this.clearRadius, this.clearAbove,
                        yard.rotation() == Rotation.CLOCKWISE_180 ? -1 : 1));
            }
            for (Yard yard : yards)
            {
                StructureTemplate yardTemplate = context.structureTemplateManager()
                        .getOrCreate(DdIds.parse(yard.template()));
                builder.addPiece(new CourtyardPiece(context.structureTemplateManager(),
                        yard.template(), yard.pos(), yard.rotation()));
                if (yard.blackSoil() && this.blackSoilRadius > 0)
                {
                    BlockPos center = yard.pos().offset(
                            yardTemplate.getSize().getX() / 2, 0, yardTemplate.getSize().getZ() / 2);
                    builder.addPiece(new BlackSoilPiece(center, this.blackSoilRadius, BLACK_SOIL_DENSITY));
                    builder.addPiece(new BiomeRichSoilPiece(center, this.blackSoilRadius * 4, BIOME_SOIL_DENSITY));
                }
            }
        }));
    }

    private static boolean partsEnabled()
    {
        return com.gunmu.northeast_china_delight.NortheastChinaConfig.YARD_PARTS_ENABLED.get();
    }

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
                    StructureTemplate currentSize = sizes.get(current.template());
                    StructureTemplate nextSize = sizes.computeIfAbsent(template,
                            id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
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
        if (biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)
                || biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN))
        {
            return null;
        }
        if (!this.slopeOk(context, x, z, surface))
        {
            return null;
        }
        if (!this.footprintOk(context, template, x, z, surface))
        {
            return null;
        }
        if (!this.waterFarEnough(context, template, x, z))
        {
            return null;
        }
        if (this.tooCloseToAvoidedStructures(context, template, x, z))
        {
            return null;
        }
        sizes.computeIfAbsent(template, id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
        boolean blackSoil = this.blackSoilChance > 0.0F && random.nextFloat() < this.blackSoilChance;
        // 院子的草地要和外边的地面齐平。坑在于雪原：地表最上面那格是雪层，
        // 高度图会把雪层也算进去，于是院子会被抬高整整一格（就是那块露在外面的砖）。
        // 所以下雪的地方要多压一格，让院子地面落在雪层下面那层地面上。
        boolean snowy = biome.value().getPrecipitationAt(new BlockPos(x, surface, z))
                == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
        int sink = snowy ? 2 : 1;
        BlockPos pos = new BlockPos(x, surface - GROUND_LAYER_IN_TEMPLATE - sink, z);
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
        StructureTemplate t = context.structureTemplateManager().getOrCreate(DdIds.parse(template));
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
                    return false;
                }
                if (Math.abs(surface - yardSurface) > MAX_GATE_SLOPE)
                {
                    return false;
                }
            }
        }
        return true;
    }

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
        StructureTemplate t = context.structureTemplateManager().getOrCreate(DdIds.parse(template));
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

    private boolean waterFarEnough(GenerationContext context, String template, int x, int z)
    {
        StructureTemplate t = context.structureTemplateManager().getOrCreate(DdIds.parse(template));
        int half = Math.max(t.getSize().getX(), t.getSize().getZ()) / 2;
        int reach = half + WATER_CLEARANCE;
        for (int dx = -reach; dx <= reach; dx += 4)
        {
            for (int dz = -reach; dz <= reach; dz += 4)
            {
                int surface = context.chunkGenerator().getBaseHeight(x + dx, z + dz,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int floor = context.chunkGenerator().getBaseHeight(x + dx, z + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (surface != floor)
                {
                    return false;
                }
            }
        }
        return true;
    }

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

    private boolean tooCloseToAvoidedStructures(GenerationContext context, String template, int x, int z)
    {
        if (this.avoidMargin <= 0 || this.avoidStructures.isEmpty())
        {
            return false;
        }
        Registry<StructureSet> sets;
        try
        {
            sets = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        }
        catch (Exception e)
        {
            return false;
        }
        StructureTemplate t = context.structureTemplateManager().getOrCreate(DdIds.parse(template));
        int sizeX = t.getSize().getX();
        int sizeZ = t.getSize().getZ();
        int minX = x - this.avoidMargin;
        int maxX = x + sizeX - 1 + this.avoidMargin;
        int minZ = z - this.avoidMargin;
        int maxZ = z + sizeZ - 1 + this.avoidMargin;
        long seed = context.seed();
        for (String id : this.avoidStructures)
        {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null)
            {
                continue;
            }
            Optional<Holder.Reference<StructureSet>> holder =
                    sets.getHolder(ResourceKey.create(Registries.STRUCTURE_SET, key));
            if (holder.isEmpty() || !(holder.get().value().placement() instanceof RandomSpreadStructurePlacement spread))
            {
                continue;
            }
            int spacing = Math.max(1, spread.spacing());
            int minRegionX = Math.floorDiv(minX >> 4, spacing) - 1;
            int maxRegionX = Math.floorDiv(maxX >> 4, spacing) + 1;
            int minRegionZ = Math.floorDiv(minZ >> 4, spacing) - 1;
            int maxRegionZ = Math.floorDiv(maxZ >> 4, spacing) + 1;
            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++)
            {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++)
                {
                    ChunkPos candidate = spread.getPotentialStructureChunk(seed, regionX * spacing, regionZ * spacing);
                    int centerX = candidate.getMinBlockX() + 8;
                    int centerZ = candidate.getMinBlockZ() + 8;
                    if (centerX < minX || centerX > maxX || centerZ < minZ || centerZ > maxZ)
                    {
                        continue;
                    }
                    //? if >=1.20.5 {
                    /*if (!spread.applyAdditionalChunkRestrictions(candidate.x, candidate.z, seed))
                    {
                        continue;
                    }
                    *///?}
                    if (this.avoidedStructureCouldGenerate(context, holder.get().value(), centerX, centerZ))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean avoidedStructureCouldGenerate(GenerationContext context, StructureSet set, int blockX, int blockZ)
    {
        int surface = context.chunkGenerator().getBaseHeight(blockX, blockZ, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        Holder<Biome> biomeHere = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(blockX), QuartPos.fromBlock(surface), QuartPos.fromBlock(blockZ),
                context.randomState().sampler());
        for (StructureSet.StructureSelectionEntry entry : set.structures())
        {
            if (entry.structure().value().biomes().contains(biomeHere))
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

    public record Yard(String template, BlockPos pos, Rotation rotation, boolean blackSoil)
    {
    }
}
