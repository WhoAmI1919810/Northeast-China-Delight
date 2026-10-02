package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 世界生成相关的注册表：结构类型 + 结构片段类型。 */
public class ModWorldGen
{
    /** 本模组自己的地物（榛子丛、树脚下的榛蘑、野生人参） */
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, NortheastChinaDelight.MODID);

    public static final DeferredHolder<Feature<?>, HazelnutBushFeature> HAZELNUT_BUSH_FEATURE =
            FEATURES.register("hazelnut_bush", () -> new HazelnutBushFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, HazelMushroomAroundTreeFeature> HAZEL_MUSHROOM_TREE_FEATURE =
            FEATURES.register("hazel_mushroom_around_tree",
                    () -> new HazelMushroomAroundTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, GinsengPatchFeature> GINSENG_PATCH_FEATURE =
            FEATURES.register("ginseng_patch", () -> new GinsengPatchFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, NortheastChinaDelight.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, NortheastChinaDelight.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<NortheastCourtyardStructure>> COURTYARD_TYPE =
            STRUCTURE_TYPES.register("northeast_courtyard", () -> () -> NortheastCourtyardStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> COURTYARD_PIECE =
            PIECE_TYPES.register("northeast_courtyard",
                    () -> (StructurePieceType.StructureTemplateType) CourtyardPiece::new);

    /** 小院周围的「黑土地」（农夫乐事沃土）散布器 */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BLACK_SOIL_PIECE =
            PIECE_TYPES.register("northeast_black_soil",
                    () -> (StructurePieceType.ContextlessType) BlackSoilPiece::new);

    /** 小院周边的清场（上方净空 + 半径内地物） */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CLEARING_PIECE =
            PIECE_TYPES.register("northeast_clearing",
                    () -> (StructurePieceType.ContextlessType) YardClearingPiece::new);

    private ModWorldGen()
    {
    }

    public static void register(IEventBus eventBus)
    {
        FEATURES.register(eventBus);
        STRUCTURE_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
    }
}
