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
 *   walk_chance   —— 第二户之后继续长新院的概率（默认 0.80，之后每户 -15%，最低 20%）
 *   step_min/max  —— 游走距离范围（默认 20~50 格）
 *   max_houses    —— 一个群落最多几户（默认不限，游走概率自己兜底）
 *   min_gap       —— 两个院子的最小间隔（格子数），防止院子叠在一起
 *   max_slope     —— 起点两侧地形高差超过这个数就不生成（防止糊在陡坡上）
 *   allow_flip    —— 是否允许院子朝南 / 朝北两种朝向
 * </pre>
 * 「多大概率刷」还有第二层：{@code worldgen/structure_set/northeast_yards.json} 里的
 * {@code spacing}（每多少区块试一次）和 {@code separation}（同格内最小间隔），
 * 这两个数越小越密。
 *
 * <p>生成规则：第一座严格按群系、坡度、水面、门口和村庄避让选址；第一座成功后
 * 无条件寻找第二座，第二座只要求干燥、地形支撑和与第一座不重叠，并且必须换一种模板。
 * 两座都会对完整院落边界逐格检查地表高差，避免清场件把低处填成露出数格的无意义土台。
 * 找不到合格的第二座时整簇不生成，避免留下独户。第三座起从东西南北四个方向按
 * {@code walk_chance} 梯度继续扩展；第三座起每座都从完整模板池独立抽取，允许重复。
 * 第一座院子的可行朝向确定院群主门向；后续院子在门口地形允许时优先沿用主门向，
 * 主门向不可通行或门前会撞已有院子时自动反向。第二座不会仅因门向不同而失败。
 * 单个 placement 区域最多尝试三个候选点：原候选区块内一次，失败后在周围 ±3 区块内
 * 各再补一次；三次都失败才放弃这个区域。
 */
public class NortheastCourtyardStructure extends Structure
{
    public static final List<String> DEFAULT_TEMPLATES = List.of(
            "northeast_china_delight:northeast_yard_garden",
            "northeast_china_delight:northeast_yard_compound",
            "northeast_china_delight:northeast_yard_livestock",
            "northeast_china_delight:northeast_yard_tiled_house",
            "northeast_china_delight:northeast_yard_long_house",
            "northeast_china_delight:northeast_yard_brick_green_roof",
            "northeast_china_delight:northeast_yard_manchu_thatch"
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
    public static final int YARD_RAISE = 1;
    public static final int LATERAL_JITTER = 6;
    public static final int GATE_CHECK_WIDTH = 18;
    public static final int MAX_GATE_SLOPE = 4;
    private static final float CLUSTER_FACING_CHANCE = 0.8F;
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
    public static final int SECOND_YARD_DISTANCE_SAMPLES = 8;
    public static final int YARD_FOOTPRINT_DROP_LIMIT = 2;
    public static final int YARD_FOOTPRINT_RISE_LIMIT = 1;
    public static final int SECOND_YARD_FOOTPRINT_DROP_LIMIT = 3;
    public static final int SECOND_YARD_FOOTPRINT_RISE_LIMIT = 2;
    private static final int MAX_CANDIDATE_ATTEMPTS = 3;
    private static final int LOCAL_CANDIDATE_RADIUS_CHUNKS = 3;
    private static final int FOOTPRINT_INTERIOR_SAMPLE_STEP = 2;
    private static final int WATER_CLEARANCE_SAMPLE_STEP = 4;

    private static final List<Direction> HORIZONTALS =
            List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);
    private static final int[][] SECOND_YARD_STEPS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

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
        int baseX = context.chunkPos().getMinBlockX();
        int baseZ = context.chunkPos().getMinBlockZ();
        List<Yard> yards = List.of();
        for (int attempt = 0; attempt < MAX_CANDIDATE_ATTEMPTS && yards.isEmpty(); attempt++)
        {
            int x = baseX + random.nextInt(16);
            int z = baseZ + random.nextInt(16);
            if (attempt > 0)
            {
                x += (random.nextInt(LOCAL_CANDIDATE_RADIUS_CHUNKS * 2 + 1)
                        - LOCAL_CANDIDATE_RADIUS_CHUNKS) * 16;
                z += (random.nextInt(LOCAL_CANDIDATE_RADIUS_CHUNKS * 2 + 1)
                        - LOCAL_CANDIDATE_RADIUS_CHUNKS) * 16;
            }
            yards = this.growCluster(context, random, x, z);
        }
        if (yards.isEmpty())
        {
            return Optional.empty();
        }
        List<Yard> generatedYards = yards;
        BlockPos origin = yards.get(0).pos();
        return Optional.of(new GenerationStub(origin, builder -> {
            for (Yard yard : generatedYards)
            {
                if (this.clearRadius <= 0 && this.clearAbove <= 0)
                {
                    break;
                }
                StructureTemplate template = context.structureTemplateManager()
                        .getOrCreate(DdIds.parse(yard.template()));
                int sizeX = template.getSize().getX();
                int sizeZ = template.getSize().getZ();
                BlockPos clearingOrigin = new BlockPos(
                        footprintMinX(yard.pos().getX(), sizeX, yard.rotation()),
                        yard.pos().getY(),
                        footprintMinZ(yard.pos().getZ(), sizeZ, yard.rotation()));
                builder.addPiece(new YardClearingPiece(clearingOrigin,
                        sizeX, sizeZ,
                        yard.pos().getY() + GROUND_LAYER_IN_TEMPLATE,
                        this.clearRadius, this.clearAbove,
                        yard.rotation() == Rotation.CLOCKWISE_180 ? -1 : 1));
            }
            for (Yard yard : generatedYards)
            {
                StructureTemplate yardTemplate = context.structureTemplateManager()
                        .getOrCreate(DdIds.parse(yard.template()));
                builder.addPiece(new CourtyardPiece(context.structureTemplateManager(),
                        yard.template(), yard.pos(), yard.rotation()));
                if (yard.blackSoil() && this.blackSoilRadius > 0)
                {
                    int sizeX = yardTemplate.getSize().getX();
                    int sizeZ = yardTemplate.getSize().getZ();
                    BlockPos center = new BlockPos(
                            footprintMinX(yard.pos().getX(), sizeX, yard.rotation()) + sizeX / 2,
                            yard.pos().getY(),
                            footprintMinZ(yard.pos().getZ(), sizeZ, yard.rotation()) + sizeZ / 2);
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
        if (this.maxHouses < 2)
        {
            return List.of();
        }
        List<Yard> yards = new ArrayList<>();
        Map<String, StructureTemplate> sizes = new HashMap<>();
        Yard first = this.tryMakeYard(context, random, sizes,
                this.pickTemplate(random), startX, startZ, null, List.of());
        if (first == null)
        {
            return List.of();
        }
        yards.add(first);
        Rotation preferred = first.rotation();
        Yard second = this.makeSecondYard(context, random, sizes, first);
        if (second == null)
        {
            return List.of();
        }
        yards.add(second);
        ArrayDeque<Yard> queue = new ArrayDeque<>();
        queue.add(first);
        queue.add(second);

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
                StructureTemplate currentSize = sizes.computeIfAbsent(current.template(),
                        id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
                StructureTemplate nextSize = sizes.computeIfAbsent(template,
                        id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
                int lateral = LATERAL_JITTER > 0
                        ? random.nextInt(LATERAL_JITTER * 2 + 1) - LATERAL_JITTER : 0;
                int[] next = this.stepPosition(current, currentSize, nextSize,
                        direction.getStepX(), direction.getStepZ(), distance, lateral);
                Yard candidate = this.tryMakeYard(context, random, sizes, template,
                        next[0], next[1], preferred, yards);
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
     * 第一座成功后再找第二座：优先保证群落不是独户，因此不掷游走概率。
     * 第二座只保留干燥、地形支撑、世界高度和院落不重叠这几项硬条件。
     */
    private Yard makeSecondYard(GenerationContext context, WorldgenRandom random,
                                Map<String, StructureTemplate> sizes, Yard first)
    {
        List<String> alternatives = secondTemplatePool(this.templates, first.template());
        if (alternatives.isEmpty())
        {
            return null;
        }
        List<String> candidates = new ArrayList<>(alternatives);
        for (int index = candidates.size() - 1; index > 0; index--)
        {
            int swap = random.nextInt(index + 1);
            String template = candidates.get(index);
            candidates.set(index, candidates.get(swap));
            candidates.set(swap, template);
        }
        StructureTemplate firstSize = sizes.computeIfAbsent(first.template(),
                id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
        int startDirection = random.nextInt(SECOND_YARD_STEPS.length);
        int distanceSpan = this.stepMax - this.stepMin;
        for (String template : candidates)
        {
            StructureTemplate secondSize = sizes.computeIfAbsent(template,
                    id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
            for (int directionIndex = 0; directionIndex < SECOND_YARD_STEPS.length; directionIndex++)
            {
                int[] step = SECOND_YARD_STEPS[(startDirection + directionIndex) % SECOND_YARD_STEPS.length];
                for (int sample = 0; sample < SECOND_YARD_DISTANCE_SAMPLES; sample++)
                {
                    int distance = this.stepMin + (SECOND_YARD_DISTANCE_SAMPLES == 1 ? 0
                            : Math.round((float) distanceSpan * sample / (SECOND_YARD_DISTANCE_SAMPLES - 1)));
                    int lateral = step[0] == 0 || step[1] == 0
                            ? random.nextInt(LATERAL_JITTER * 2 + 1) - LATERAL_JITTER : 0;
                    int[] next = this.stepPosition(first, firstSize, secondSize,
                            step[0], step[1], distance, lateral);
                    Yard candidate = this.tryMakeSecondYard(context, random, sizes, template,
                            next[0], next[1], first);
                    if (candidate != null && !this.overlaps(candidate, List.of(first), sizes))
                    {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private Yard tryMakeSecondYard(GenerationContext context, WorldgenRandom random,
                                   Map<String, StructureTemplate> sizes, String template,
                                   int x, int z, Yard first)
    {
        int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight();
        if (!isSecondYardTerrainSuitable(surface, floor, minY, maxY))
        {
            return null;
        }
        StructureTemplate size = sizes.computeIfAbsent(template,
                id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
        if (!this.footprintTerrainOk(context, size, x, z, surface, minY, maxY,
                Math.min(this.maxSlope, SECOND_YARD_FOOTPRINT_DROP_LIMIT),
                Math.min(Math.max(1, this.maxSlope / 2), SECOND_YARD_FOOTPRINT_RISE_LIMIT),
                false))
        {
            return null;
        }
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(surface), QuartPos.fromBlock(z),
                context.randomState().sampler());
        boolean blackSoil = this.blackSoilChance > 0.0F && random.nextFloat() < this.blackSoilChance;
        boolean snowy = biome.value().getPrecipitationAt(new BlockPos(x, surface, z))
                == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
        int sink = snowy ? 2 : 1;
        BlockPos footprintMin = new BlockPos(x, surface - GROUND_LAYER_IN_TEMPLATE - sink + YARD_RAISE, z);
        Rotation rotation = this.pickSecondRotation(size, footprintMin, first, sizes, random);
        if (rotation == null)
        {
            return null;
        }
        BlockPos placedPos = templateOrigin(footprintMin, size.getSize().getX(), size.getSize().getZ(), rotation);
        return new Yard(template, placedPos, rotation, blackSoil);
    }

    private int[] stepPosition(Yard current, StructureTemplate currentSize, StructureTemplate nextSize,
                               int stepX, int stepZ, int distance, int lateral)
    {
        if ("center".equals(this.stepMeasure))
        {
            int currentCenterX = footprintMinX(current.pos().getX(),
                    currentSize.getSize().getX(), current.rotation()) + currentSize.getSize().getX() / 2;
            int currentCenterZ = footprintMinZ(current.pos().getZ(),
                    currentSize.getSize().getZ(), current.rotation()) + currentSize.getSize().getZ() / 2;
            return new int[]{
                    currentCenterX + stepX * distance,
                    currentCenterZ + stepZ * distance
            };
        }
        int currentMinX = footprintMinX(current.pos().getX(), currentSize.getSize().getX(), current.rotation());
        int currentMinZ = footprintMinZ(current.pos().getZ(), currentSize.getSize().getZ(), current.rotation());
        int x = gapStepOrigin(currentMinX, currentSize.getSize().getX(), nextSize.getSize().getX(),
                stepX, distance, lateral);
        int z = gapStepOrigin(currentMinZ, currentSize.getSize().getZ(), nextSize.getSize().getZ(),
                stepZ, distance, lateral);
        return new int[]{x, z};
    }

    static int gapStepOrigin(int currentMin, int currentSize, int nextSize, int step, int distance, int lateral)
    {
        if (step > 0)
        {
            return currentMin + currentSize + distance;
        }
        if (step < 0)
        {
            return currentMin - nextSize - distance;
        }
        return currentMin + lateral;
    }

    static List<String> secondTemplatePool(List<String> templates, String firstTemplate)
    {
        return templates.stream()
                .filter(template -> !template.equals(firstTemplate))
                .distinct()
                .toList();
    }

    static boolean isSecondYardTerrainSuitable(int surface, int floor, int minY, int maxY)
    {
        return surface == floor && surface >= minY + 12 && surface <= maxY - 32;
    }

    static boolean isSecondYardFootprintHeightSuitable(int anchorSurface, int sampleSurface, int maxSlope)
    {
        int lowerTolerance = Math.min(maxSlope, SECOND_YARD_FOOTPRINT_DROP_LIMIT);
        int upperTolerance = Math.min(Math.max(1, maxSlope / 2), SECOND_YARD_FOOTPRINT_RISE_LIMIT);
        return isFootprintHeightSuitable(anchorSurface, sampleSurface, lowerTolerance, upperTolerance);
    }

    static boolean isFootprintHeightSuitable(int anchorSurface, int sampleSurface,
                                             int dropLimit, int riseLimit)
    {
        return sampleSurface >= anchorSurface - dropLimit && sampleSurface <= anchorSurface + riseLimit;
    }

    /** 从完整模板池中均匀抽取。 */
    private String pickTemplate(WorldgenRandom random)
    {
        return pickTemplate(random, this.templates);
    }

    static String pickTemplate(WorldgenRandom random, List<String> templates)
    {
        return templates.get(random.nextInt(templates.size()));
    }

    private float walkChanceFor(List<Yard> placed)
    {
        return walkChanceFor(placed.size(), this.walkChance);
    }

    static float walkChanceFor(int placedCount, float baseChance)
    {
        float chance = baseChance - WALK_CHANCE_STEP * Math.max(0, placedCount - 1);
        return Math.max(WALK_CHANCE_MIN, chance);
    }

    private Yard tryMakeYard(GenerationContext context, WorldgenRandom random,
                            Map<String, StructureTemplate> sizes, String template, int x, int z,
                            Rotation preferred, List<Yard> existingYards)
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
        StructureTemplate templateSize = sizes.computeIfAbsent(template,
                id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
        if (!this.footprintTerrainOk(context, templateSize, x, z, surface, minY, maxY,
                Math.min(this.maxSlope, YARD_FOOTPRINT_DROP_LIMIT),
                Math.min(Math.max(1, this.maxSlope / 2), YARD_FOOTPRINT_RISE_LIMIT),
                true))
        {
            return null;
        }
        if (this.tooCloseToAvoidedStructures(context, template, x, z))
        {
            return null;
        }
        boolean blackSoil = this.blackSoilChance > 0.0F && random.nextFloat() < this.blackSoilChance;
        // 院子的草地比外边地面高 YARD_RAISE 格。雪原的高度图会把雪层也算进去，
        // 所以下雪的地方仍多压一格作为高度图补偿；YARD_RAISE 再统一抬高院面。
        boolean snowy = biome.value().getPrecipitationAt(new BlockPos(x, surface, z))
                == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
        int sink = snowy ? 2 : 1;
        BlockPos footprintMin = new BlockPos(x, surface - GROUND_LAYER_IN_TEMPLATE - sink + YARD_RAISE, z);
        Rotation rotation = this.pickRotation(context, templateSize, footprintMin, surface,
                random, preferred, existingYards, sizes);
        if (rotation == null)
        {
            return null;
        }
        BlockPos placedPos = templateOrigin(footprintMin, templateSize.getSize().getX(),
                templateSize.getSize().getZ(), rotation);
        return new Yard(template, placedPos, rotation, blackSoil);
    }

    /**
     * 挑院子朝向：先淘汰门口地形不可通行的方向，再尽量服从院群主门向。
     * 主门向只是偏好；门口会被已有院子挡住时允许反向。
     */
    private Rotation pickRotation(GenerationContext context, StructureTemplate template, BlockPos footprintMin,
                                  int surface, WorldgenRandom random, Rotation preferred,
                                  List<Yard> existingYards, Map<String, StructureTemplate> sizes)
    {
        if (!this.allowFlip)
        {
            return Rotation.NONE;
        }
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        boolean noneGateOk = this.gateCheckDepth <= 0
                || this.gateFrontOk(context, template, footprintMin, Rotation.NONE, surface);
        boolean flippedGateOk = this.gateCheckDepth <= 0
                || this.gateFrontOk(context, template, footprintMin, Rotation.CLOCKWISE_180, surface);
        boolean noneFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.NONE, existingYards, sizes);
        boolean flippedFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.CLOCKWISE_180, existingYards, sizes);
        return chooseRotation(preferred, noneGateOk, flippedGateOk,
                noneFrontageClear, flippedFrontageClear, random.nextFloat());
    }

    private Rotation pickSecondRotation(StructureTemplate template, BlockPos footprintMin, Yard first,
                                        Map<String, StructureTemplate> sizes, WorldgenRandom random)
    {
        if (!this.allowFlip)
        {
            return Rotation.NONE;
        }
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        boolean noneFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.NONE, List.of(first), sizes);
        boolean flippedFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.CLOCKWISE_180, List.of(first), sizes);
        return chooseRotation(first.rotation(), true, true,
                noneFrontageClear, flippedFrontageClear, random.nextFloat());
    }

    static Rotation chooseRotation(Rotation preferred, boolean noneGateOk, boolean flippedGateOk,
                                   boolean noneFrontageClear, boolean flippedFrontageClear, float roll)
    {
        boolean noneUsable = noneGateOk && noneFrontageClear;
        boolean flippedUsable = flippedGateOk && flippedFrontageClear;
        if (!noneUsable && !flippedUsable)
        {
            return null;
        }
        if (noneUsable != flippedUsable)
        {
            return noneUsable ? Rotation.NONE : Rotation.CLOCKWISE_180;
        }
        if (preferred == Rotation.CLOCKWISE_180)
        {
            return roll < CLUSTER_FACING_CHANCE ? Rotation.CLOCKWISE_180 : Rotation.NONE;
        }
        if (preferred == Rotation.NONE)
        {
            return roll < CLUSTER_FACING_CHANCE ? Rotation.NONE : Rotation.CLOCKWISE_180;
        }
        return roll < 0.5F ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    /** 选区统一使用院子包围盒最小角；180 度模板原点在包围盒的对角。 */
    static BlockPos templateOrigin(BlockPos footprintMin, int sizeX, int sizeZ, Rotation rotation)
    {
        return rotation == Rotation.CLOCKWISE_180
                ? footprintMin.offset(sizeX - 1, 0, sizeZ - 1)
                : footprintMin;
    }

    static int gateCenterX(BlockPos footprintMin, int sizeX)
    {
        return footprintMin.getX() + sizeX / 2;
    }

    static int gateEdgeZ(BlockPos footprintMin, int sizeZ, Rotation rotation)
    {
        return rotation == Rotation.CLOCKWISE_180
                ? footprintMin.getZ()
                : footprintMin.getZ() + sizeZ - 1;
    }

    static int gateStep(Rotation rotation)
    {
        return rotation == Rotation.CLOCKWISE_180 ? -1 : 1;
    }

    static boolean rectanglesOverlap(int aMinX, int aMinZ, int aMaxX, int aMaxZ,
                                     int bMinX, int bMinZ, int bMaxX, int bMaxZ)
    {
        return aMinX <= bMaxX && aMaxX >= bMinX && aMinZ <= bMaxZ && aMaxZ >= bMinZ;
    }

    private boolean gateApronBlocked(BlockPos footprintMin, int sizeX, int sizeZ, Rotation rotation,
                                     List<Yard> yards, Map<String, StructureTemplate> sizes)
    {
        if (yards.isEmpty())
        {
            return false;
        }
        int centerX = gateCenterX(footprintMin, sizeX);
        int edgeZ = gateEdgeZ(footprintMin, sizeZ, rotation);
        int step = gateStep(rotation);
        int apronMinX = centerX - YardClearingPiece.GATE_PATH_HALF_WIDTH;
        int apronMaxX = centerX + YardClearingPiece.GATE_PATH_HALF_WIDTH;
        int apronNearZ = edgeZ + step;
        int apronFarZ = edgeZ + step * YardClearingPiece.GATE_PATH_LENGTH;
        int apronMinZ = Math.min(apronNearZ, apronFarZ);
        int apronMaxZ = Math.max(apronNearZ, apronFarZ);
        for (Yard yard : yards)
        {
            StructureTemplate other = sizes.get(yard.template());
            if (other == null)
            {
                continue;
            }
            int otherSizeX = other.getSize().getX();
            int otherSizeZ = other.getSize().getZ();
            int otherMinX = footprintMinX(yard.pos().getX(), otherSizeX, yard.rotation());
            int otherMinZ = footprintMinZ(yard.pos().getZ(), otherSizeZ, yard.rotation());
            if (rectanglesOverlap(apronMinX, apronMinZ, apronMaxX, apronMaxZ,
                    otherMinX, otherMinZ, otherMinX + otherSizeX - 1, otherMinZ + otherSizeZ - 1))
            {
                return true;
            }
        }
        return false;
    }

    /** 门口前方那块地能不能走人 */
    private boolean gateFrontOk(GenerationContext context, StructureTemplate template, BlockPos footprintMin,
                                Rotation rotation, int yardSurface)
    {
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        int centerX = gateCenterX(footprintMin, sizeX);
        int edgeZ = gateEdgeZ(footprintMin, sizeZ, rotation);
        int step = gateStep(rotation);
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

    private boolean footprintTerrainOk(GenerationContext context, StructureTemplate template,
                                       int x, int z, int anchorSurface, int minY, int maxY,
                                       int dropLimit, int riseLimit, boolean checkWaterClearance)
    {
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        for (int dx = 0; dx < sizeX; dx++)
        {
            boolean borderX = dx == 0 || dx == sizeX - 1;
            for (int dz = 0; dz < sizeZ; dz++)
            {
                boolean borderZ = dz == 0 || dz == sizeZ - 1;
                if (!borderX && !borderZ
                        && (dx % FOOTPRINT_INTERIOR_SAMPLE_STEP != 0
                        || dz % FOOTPRINT_INTERIOR_SAMPLE_STEP != 0))
                {
                    continue;
                }
                int sampleX = x + dx;
                int sampleZ = z + dz;
                int surface = context.chunkGenerator().getBaseHeight(sampleX, sampleZ,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int floor = context.chunkGenerator().getBaseHeight(sampleX, sampleZ,
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (!isSecondYardTerrainSuitable(surface, floor, minY, maxY)
                        || !isFootprintHeightSuitable(anchorSurface, surface, dropLimit, riseLimit))
                {
                    return false;
                }
            }
        }
        return !checkWaterClearance || this.waterFarEnough(context, sizeX, sizeZ, x, z);
    }

    private boolean waterFarEnough(GenerationContext context, int sizeX, int sizeZ, int x, int z)
    {
        int minX = x - WATER_CLEARANCE;
        int maxX = x + sizeX - 1 + WATER_CLEARANCE;
        int minZ = z - WATER_CLEARANCE;
        int maxZ = z + sizeZ - 1 + WATER_CLEARANCE;
        for (int sampleX = minX; sampleX <= maxX; sampleX += WATER_CLEARANCE_SAMPLE_STEP)
        {
            for (int sampleZ = minZ; sampleZ <= maxZ; sampleZ += WATER_CLEARANCE_SAMPLE_STEP)
            {
                int surface = context.chunkGenerator().getBaseHeight(sampleX, sampleZ,
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int floor = context.chunkGenerator().getBaseHeight(sampleX, sampleZ,
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
        int aMinX = footprintMinX(candidate.pos().getX(), aw, candidate.rotation());
        int aMinZ = footprintMinZ(candidate.pos().getZ(), ad, candidate.rotation());
        for (Yard other : yards)
        {
            StructureTemplate b = sizes.get(other.template());
            int bw = b.getSize().getX();
            int bd = b.getSize().getZ();
            int bMinX = footprintMinX(other.pos().getX(), bw, other.rotation());
            int bMinZ = footprintMinZ(other.pos().getZ(), bd, other.rotation());
            int overlapX = Math.min(aMinX + aw, bMinX + bw) - Math.max(aMinX, bMinX);
            int overlapZ = Math.min(aMinZ + ad, bMinZ + bd) - Math.max(aMinZ, bMinZ);
            if (overlapX > -this.minGap && overlapZ > -this.minGap)
            {
                return true;
            }
        }
        return false;
    }

    static int footprintMinX(int originX, int sizeX, Rotation rotation)
    {
        return rotation == Rotation.CLOCKWISE_180 ? originX - sizeX + 1 : originX;
    }

    static int footprintMinZ(int originZ, int sizeZ, Rotation rotation)
    {
        return rotation == Rotation.CLOCKWISE_180 ? originZ - sizeZ + 1 : originZ;
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
