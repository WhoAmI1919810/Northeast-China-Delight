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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

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
 * <p>生成规则：第一座严格按群系、水面、门口和村庄避让选址；第一座成功后
 * 无条件寻找第二座，第二座只要求干燥、地形支撑、门前通道不临水和与第一座不重叠，
 * 并且必须换一种模板。
 * 两座都会逐格检查实际会填地的院落 footprint（含 1 格基础外扩）和所选门向的
 * 8 格门前通道：水、冰和流体一格都不允许；院面高度按 footprint 中位地表落位，
 * 采样高度低/高各超过 3 格才拒绝，
 * 避免清场件把低处填成露出数格的无意义土台。
 * 找不到合格的第二座时整簇不生成，避免留下独户。第三座起从东西南北四个方向按
 * {@code walk_chance} 梯度继续扩展；第三座起每座都从完整模板池独立抽取，允许重复。
 * 第一座院子的可行朝向确定院群主门向；后续院子在门口地形允许时优先沿用主门向，
 * 主门向不可通行或门前会撞已有院子时自动反向。第二座不会仅因门向不同而失败。
 * 单个 placement 区域最多尝试五个候选点：原候选区块内一次，失败后在周围 ±3 区块内
 * 再补四次；五次都失败才放弃这个区域。
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
            "northeast_china_delight:northeast_yard_manchu_thatch",
            "northeast_china_delight:northeast_yard_korean"
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
    public static final int SECOND_YARD_DISTANCE_SAMPLES = 8;
    public static final int YARD_FOOTPRINT_DROP_LIMIT = 3;
    public static final int YARD_FOOTPRINT_RISE_LIMIT = 3;
    public static final int SECOND_YARD_FOOTPRINT_RELIEF_LIMIT = 6;
    private static final int MAX_CANDIDATE_ATTEMPTS = 5;
    private static final int LOCAL_CANDIDATE_RADIUS_CHUNKS = 3;
    private static final int FOOTPRINT_INTERIOR_SAMPLE_STEP = 2;

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
        TerrainSampler terrain = new TerrainSampler(context);
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
            yards = this.growCluster(context, terrain, random, x, z);
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

    private List<Yard> growCluster(GenerationContext context, TerrainSampler terrain, WorldgenRandom random,
                                     int startX, int startZ)
    {
        if (this.maxHouses < 2)
        {
            return List.of();
        }
        List<Yard> yards = new ArrayList<>();
        Map<String, StructureTemplate> sizes = new HashMap<>();
        Yard first = this.tryMakeYard(context, terrain, random, sizes,
                this.pickTemplate(random), startX, startZ, null, List.of());
        if (first == null)
        {
            return List.of();
        }
        yards.add(first);
        Rotation preferred = first.rotation();
        Yard second = this.makeSecondYard(context, terrain, random, sizes, first);
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
                Yard candidate = this.tryMakeYard(context, terrain, random, sizes, template,
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
     * 第二座只保留干燥、地形支撑、完整高差、门前通道不临水和院落不重叠这几项硬条件。
     */
    private Yard makeSecondYard(GenerationContext context, TerrainSampler terrain, WorldgenRandom random,
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
                    Yard candidate = this.tryMakeSecondYard(context, terrain, random, sizes, template,
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

    private Yard tryMakeSecondYard(GenerationContext context, TerrainSampler terrain, WorldgenRandom random,
                                   Map<String, StructureTemplate> sizes, String template,
                                   int x, int z, Yard first)
    {
        Optional<TerrainProbe> anchor = terrain.probe(x, z);
        if (anchor.isEmpty() || anchor.get().wet())
        {
            return null;
        }
        StructureTemplate size = sizes.computeIfAbsent(template,
                id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
        Optional<FootprintTerrain> footprint = this.footprintTerrain(terrain, size, x, z);
        if (footprint.isEmpty()
                || !isFootprintReliefSuitable(footprint.get().minGroundY(),
                footprint.get().maxGroundY(),
                Math.min(this.maxSlope, SECOND_YARD_FOOTPRINT_RELIEF_LIMIT)))
        {
            return null;
        }
        int groundY = footprint.get().medianGroundY();
        int surface = groundY + 1;
        if (!isTerrainHeightWithinBounds(surface,
                context.heightAccessor().getMinBuildHeight(),
                context.heightAccessor().getMaxBuildHeight()))
        {
            return null;
        }
        boolean blackSoil = this.blackSoilChance > 0.0F && random.nextFloat() < this.blackSoilChance;
        BlockPos footprintMin = new BlockPos(x,
                surface - GROUND_LAYER_IN_TEMPLATE - 1 + YARD_RAISE, z);
        Rotation rotation = this.pickSecondRotation(terrain, size, footprintMin, first, sizes, random, surface);
        if (rotation == null)
        {
            return null;
        }
        if (!this.foundationWaterClear(terrain, size, x, z))
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

    static boolean isTerrainHeightWithinBounds(int surface, int minY, int maxY)
    {
        return surface >= minY + 12 && surface <= maxY - 32;
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

    private Yard tryMakeYard(GenerationContext context, TerrainSampler terrain, WorldgenRandom random,
                            Map<String, StructureTemplate> sizes, String template, int x, int z,
                            Rotation preferred, List<Yard> existingYards)
    {
        Optional<TerrainProbe> anchor = terrain.probe(x, z);
        if (anchor.isEmpty() || anchor.get().wet())
        {
            return null;
        }
        int anchorSurface = anchor.get().groundY() + 1;
        int minY = context.heightAccessor().getMinBuildHeight();
        int maxY = context.heightAccessor().getMaxBuildHeight();
        if (!isTerrainHeightWithinBounds(anchorSurface, minY, maxY))
        {
            return null;
        }
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(anchorSurface), QuartPos.fromBlock(z),
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
        StructureTemplate templateSize = sizes.computeIfAbsent(template,
                id -> context.structureTemplateManager().getOrCreate(DdIds.parse(id)));
        Optional<FootprintTerrain> footprint = this.footprintTerrain(terrain, templateSize, x, z);
        if (footprint.isEmpty())
        {
            return null;
        }
        int groundY = footprint.get().medianGroundY();
        if (!isFootprintHeightSuitable(groundY, footprint.get().minGroundY(), footprint.get().maxGroundY(),
                Math.min(this.maxSlope, YARD_FOOTPRINT_DROP_LIMIT),
                Math.min(this.maxSlope, YARD_FOOTPRINT_RISE_LIMIT)))
        {
            return null;
        }
        int surface = groundY + 1;
        if (!isTerrainHeightWithinBounds(surface, minY, maxY))
        {
            return null;
        }
        if (this.tooCloseToAvoidedStructures(context, template, x, z))
        {
            return null;
        }
        boolean blackSoil = this.blackSoilChance > 0.0F && random.nextFloat() < this.blackSoilChance;
        BlockPos footprintMin = new BlockPos(x,
                surface - GROUND_LAYER_IN_TEMPLATE - 1 + YARD_RAISE, z);
        Rotation rotation = this.pickRotation(terrain, templateSize, footprintMin, surface,
                random, preferred, existingYards, sizes);
        if (rotation == null)
        {
            return null;
        }
        if (!this.foundationWaterClear(terrain, templateSize, x, z))
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
    private Rotation pickRotation(TerrainSampler terrain, StructureTemplate template,
                                  BlockPos footprintMin, int surface, WorldgenRandom random, Rotation preferred,
                                  List<Yard> existingYards, Map<String, StructureTemplate> sizes)
    {
        if (!this.allowFlip)
        {
            return Rotation.NONE;
        }
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        boolean noneGateOk = this.gatePathOk(terrain, template, footprintMin, Rotation.NONE, surface, true);
        boolean flippedGateOk = this.gatePathOk(terrain, template, footprintMin, Rotation.CLOCKWISE_180,
                surface, true);
        boolean noneFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.NONE, existingYards, sizes);
        boolean flippedFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.CLOCKWISE_180, existingYards, sizes);
        return chooseRotation(preferred, noneGateOk, flippedGateOk,
                noneFrontageClear, flippedFrontageClear, random.nextFloat());
    }

    private Rotation pickSecondRotation(TerrainSampler terrain, StructureTemplate template,
                                        BlockPos footprintMin, Yard first, Map<String, StructureTemplate> sizes,
                                        WorldgenRandom random, int surface)
    {
        if (!this.allowFlip)
        {
            return Rotation.NONE;
        }
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        boolean noneGateOk = this.gatePathOk(terrain, template, footprintMin, Rotation.NONE, surface, false);
        boolean flippedGateOk = this.gatePathOk(terrain, template, footprintMin, Rotation.CLOCKWISE_180,
                surface, false);
        boolean noneFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.NONE, List.of(first), sizes);
        boolean flippedFrontageClear = !this.gateApronBlocked(footprintMin, sizeX, sizeZ,
                Rotation.CLOCKWISE_180, List.of(first), sizes);
        return chooseRotation(first.rotation(), noneGateOk, flippedGateOk,
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
        return gateCenterX(footprintMin.getX(), sizeX);
    }

    static int gateCenterX(int minX, int sizeX)
    {
        return minX + sizeX / 2;
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

    /**
     * 只检查实际会被清场件填成地基的门前通道。
     * 水面严格限定在 8 格填地通道内；gate_check_depth 只用于额外的通行高度检查。
     */
    private boolean gatePathOk(TerrainSampler terrain, StructureTemplate template, BlockPos footprintMin,
                               Rotation rotation, int yardSurface, boolean checkHeight)
    {
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        boolean waterClear = gatePathPasses(footprintMin.getX(), footprintMin.getZ(), sizeX, sizeZ,
                gateStep(rotation),
                (x, z) -> {
                    Optional<TerrainProbe> probe = terrain.probe(x, z);
                    return dryGround(probe);
                }, YardClearingPiece.GATE_PATH_LENGTH);
        if (!waterClear || !checkHeight)
        {
            return waterClear;
        }
        int heightDepth = Math.max(YardClearingPiece.GATE_PATH_LENGTH, this.gateCheckDepth);
        return gatePathPasses(footprintMin.getX(), footprintMin.getZ(), sizeX, sizeZ, gateStep(rotation),
                (x, z) -> {
                    Optional<TerrainProbe> probe = terrain.probe(x, z);
                    return dryGround(probe)
                            && Math.abs(probe.get().groundY() + 1 - yardSurface) <= MAX_GATE_SLOPE;
                }, heightDepth);
    }

    static boolean gatePathPasses(int minX, int minZ, int sizeX, int sizeZ, int gateDir,
                                  BiPredicate<Integer, Integer> passable, int depth)
    {
        int centerX = gateCenterX(minX, sizeX);
        int edgeZ = gateDir > 0 ? minZ + sizeZ - 1 : minZ;
        for (int dz = 1; dz <= depth; dz++)
        {
            int z = edgeZ + gateDir * dz;
            int halfWidth = dz <= YardClearingPiece.GATE_PATH_LENGTH
                    ? YardClearingPiece.GATE_PATH_HALF_WIDTH : 0;
            for (int dx = -halfWidth; dx <= halfWidth; dx++)
            {
                if (!passable.test(centerX + dx, z))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private Optional<FootprintTerrain> footprintTerrain(TerrainSampler terrain, StructureTemplate template,
                                                        int x, int z)
    {
        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();
        int margin = YardClearingPiece.FOUNDATION_MARGIN;
        int minDx = -margin;
        int maxDx = sizeX - 1 + margin;
        int minDz = -margin;
        int maxDz = sizeZ - 1 + margin;
        int[] heights = new int[(maxDx - minDx + 1) * (maxDz - minDz + 1)];
        int count = 0;
        for (int dx = minDx; dx <= maxDx; dx++)
        {
            boolean borderX = dx == minDx || dx == maxDx;
            for (int dz = minDz; dz <= maxDz; dz++)
            {
                boolean borderZ = dz == minDz || dz == maxDz;
                if (!borderX && !borderZ
                        && (dx % FOOTPRINT_INTERIOR_SAMPLE_STEP != 0
                        || dz % FOOTPRINT_INTERIOR_SAMPLE_STEP != 0))
                {
                    continue;
                }
                Optional<TerrainProbe> probe = terrain.probe(x + dx, z + dz);
                if (!dryGround(probe))
                {
                    return Optional.empty();
                }
                heights[count++] = probe.get().groundY();
            }
        }
        if (count == 0)
        {
            return Optional.empty();
        }
        Arrays.sort(heights, 0, count);
        return Optional.of(new FootprintTerrain(heights[0], heights[count - 1], heights[count / 2]));
    }

    /** 填地范围必须逐格无水；内部采样只用于高度，不能拿来代替临水判定。 */
    private boolean foundationWaterClear(TerrainSampler terrain, StructureTemplate template, int x, int z)
    {
        return footprintColumnsPass(template.getSize().getX(), template.getSize().getZ(),
                YardClearingPiece.FOUNDATION_MARGIN,
                (dx, dz) -> dryGround(terrain.probe(x + dx, z + dz)));
    }

    static boolean footprintColumnsPass(int sizeX, int sizeZ, int margin,
                                        BiPredicate<Integer, Integer> passable)
    {
        int min = -margin;
        int maxX = sizeX - 1 + margin;
        int maxZ = sizeZ - 1 + margin;
        for (int dx = min; dx <= maxX; dx++)
        {
            for (int dz = min; dz <= maxZ; dz++)
            {
                if (!passable.test(dx, dz))
                {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isFootprintHeightSuitable(int referenceY, int minGroundY, int maxGroundY,
                                             int dropLimit, int riseLimit)
    {
        return minGroundY >= referenceY - dropLimit && maxGroundY <= referenceY + riseLimit;
    }

    static boolean isFootprintReliefSuitable(int minGroundY, int maxGroundY, int reliefLimit)
    {
        return maxGroundY >= minGroundY && maxGroundY - minGroundY <= reliefLimit;
    }

    /** 按区块复用 NoiseChunk 取样，避免逐列重建同一片地形。 */
    private static final class TerrainSampler
    {
        private final GenerationContext context;
        private final NoiseBasedChunkGenerator noiseGenerator;
        private final NoiseGeneratorSettings generatorSettings;
        private final NoiseSettings noiseSettings;
        private final Aquifer.FluidPicker fluidPicker;
        private final Map<Long, TerrainProbe[]> chunks = new HashMap<>();
        private final int minY;
        private final int maxY;

        private TerrainSampler(GenerationContext context)
        {
            this.context = context;
            this.noiseGenerator = context.chunkGenerator() instanceof NoiseBasedChunkGenerator noise
                    ? noise : null;
            this.generatorSettings = this.noiseGenerator == null ? null
                    : this.noiseGenerator.generatorSettings().value();
            this.noiseSettings = this.generatorSettings == null ? null
                    : this.generatorSettings.noiseSettings().clampToHeightAccessor(context.heightAccessor());
            this.fluidPicker = this.generatorSettings == null ? null : createFluidPicker(this.generatorSettings);
            this.minY = context.heightAccessor().getMinBuildHeight();
            this.maxY = context.heightAccessor().getMaxBuildHeight() - 1;
        }

        private Optional<TerrainProbe> probe(int x, int z)
        {
            if (this.noiseGenerator == null)
            {
                BlockColumn column = this.context.chunkGenerator().getBaseColumn(x, z,
                        this.context.heightAccessor(), this.context.randomState());
                if (column == null)
                {
                    return Optional.empty();
                }
                return inspectTerrainColumn(column, this.minY, this.maxY);
            }
            TerrainProbe[] chunk = this.chunks.computeIfAbsent(
                    ChunkPos.asLong(x >> 4, z >> 4),
                    key -> this.sampleChunk(x >> 4, z >> 4));
            return Optional.ofNullable(chunk[((x & 15) << 4) | (z & 15)]);
        }

        private TerrainProbe[] sampleChunk(int chunkX, int chunkZ)
        {
            TerrainProbe[] probes = new TerrainProbe[256];
            int cellWidth = this.noiseSettings.getCellWidth();
            int cellHeight = this.noiseSettings.getCellHeight();
            int cells = 16 / cellWidth;
            int minCellY = Math.floorDiv(this.noiseSettings.minY(), cellHeight);
            int cellCountY = Math.floorDiv(this.noiseSettings.height(), cellHeight);
            int chunkMinX = chunkX << 4;
            int chunkMinZ = chunkZ << 4;
            TerrainNoiseChunk chunk = new TerrainNoiseChunk(cells, this.context.randomState(),
                    chunkMinX, chunkMinZ, this.noiseSettings, this.generatorSettings, this.fluidPicker);
            BlockState defaultBlock = this.generatorSettings.defaultBlock();

            chunk.initializeForFirstCellX();
            for (int cellX = 0; cellX < cells; cellX++)
            {
                chunk.advanceCellX(cellX);
                for (int cellZ = 0; cellZ < cells; cellZ++)
                {
                    for (int cellY = cellCountY - 1; cellY >= 0; cellY--)
                    {
                        chunk.selectCellYZ(cellY, cellZ);
                        for (int subY = cellHeight - 1; subY >= 0; subY--)
                        {
                            int worldY = (minCellY + cellY) * cellHeight + subY;
                            chunk.updateForY(worldY, (double) subY / cellHeight);
                            for (int subX = 0; subX < cellWidth; subX++)
                            {
                                int localX = cellX * cellWidth + subX;
                                int worldX = chunkMinX + localX;
                                chunk.updateForX(worldX, (double) subX / cellWidth);
                                for (int subZ = 0; subZ < cellWidth; subZ++)
                                {
                                    int localZ = cellZ * cellWidth + subZ;
                                    int index = (localX << 4) | localZ;
                                    if (probes[index] != null)
                                    {
                                        continue;
                                    }
                                    int worldZ = chunkMinZ + localZ;
                                    chunk.updateForZ(worldZ, (double) subZ / cellWidth);
                                    BlockState state = chunk.currentState();
                                    if (state == null)
                                    {
                                        state = defaultBlock;
                                    }
                                    if (isFluidOrIce(state))
                                    {
                                        probes[index] = new TerrainProbe(worldY, true);
                                    }
                                    else if (!state.isAir() && !isTerrainDecoration(state))
                                    {
                                        probes[index] = new TerrainProbe(worldY, false);
                                    }
                                }
                            }
                        }
                    }
                }
                chunk.swapSlices();
            }
            chunk.stopInterpolation();
            return probes;
        }
    }

    private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings settings)
    {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus fluid = new Aquifer.FluidStatus(settings.seaLevel(), settings.defaultFluid());
        return (x, y, z) -> y < Math.min(-54, settings.seaLevel()) ? lava : fluid;
    }

    private static final class TerrainNoiseChunk extends NoiseChunk
    {
        private TerrainNoiseChunk(int cellCountXZ, RandomState random, int firstNoiseX, int firstNoiseZ,
                                  NoiseSettings noiseSettings, NoiseGeneratorSettings generatorSettings,
                                  Aquifer.FluidPicker fluidPicker)
        {
            super(cellCountXZ, random, firstNoiseX, firstNoiseZ, noiseSettings, NoBeardifier.INSTANCE,
                    generatorSettings, fluidPicker, Blender.empty());
        }

        private BlockState currentState()
        {
            return this.getInterpolatedState();
        }
    }

    private enum NoBeardifier implements DensityFunctions.BeardifierOrMarker
    {
        INSTANCE;

        @Override
        public double compute(DensityFunction.FunctionContext context)
        {
            return 0.0;
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider)
        {
            Arrays.fill(values, 0.0);
        }

        @Override
        public double minValue()
        {
            return 0.0;
        }

        @Override
        public double maxValue()
        {
            return 0.0;
        }
    }

    static Optional<TerrainProbe> inspectTerrainColumn(BlockColumn column, int minY, int maxY)
    {
        for (int y = maxY; y >= minY; y--)
        {
            BlockState state = column.getBlock(y);
            if (isFluidOrIce(state))
            {
                return Optional.of(new TerrainProbe(y, true));
            }
            if (state.isAir() || isTerrainDecoration(state))
            {
                continue;
            }
            return Optional.of(new TerrainProbe(y, false));
        }
        return Optional.empty();
    }

    private static boolean dryGround(Optional<TerrainProbe> probe)
    {
        return probe.isPresent() && !probe.get().wet();
    }

    private static boolean isFluidOrIce(BlockState state)
    {
        return !state.getFluidState().isEmpty()
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    private static boolean isTerrainDecoration(BlockState state)
    {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW) || YardClearingPiece.isVegetation(state);
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

    record TerrainProbe(int groundY, boolean wet)
    {
    }

    record FootprintTerrain(int minGroundY, int maxGroundY, int medianGroundY)
    {
    }
}
