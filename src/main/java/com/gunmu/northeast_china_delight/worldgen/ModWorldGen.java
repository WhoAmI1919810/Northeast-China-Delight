package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

import java.util.function.Supplier;

public class ModWorldGen
{
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, NortheastChinaDelight.MODID);

    public static final Supplier<HazelnutBushFeature> HAZELNUT_BUSH_FEATURE =
            FEATURES.register("hazelnut_bush", () -> new HazelnutBushFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<HazelMushroomAroundTreeFeature> HAZEL_MUSHROOM_TREE_FEATURE =
            FEATURES.register("hazel_mushroom_around_tree",
                    () -> new HazelMushroomAroundTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<GinsengPatchFeature> GINSENG_PATCH_FEATURE =
            FEATURES.register("ginseng_patch", () -> new GinsengPatchFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, NortheastChinaDelight.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, NortheastChinaDelight.MODID);

    private static final Supplier<StructureType<NortheastCourtyardStructure>> COURTYARD_TYPE_SUPPLIER =
            //? if <1.20.5 {
            /*() -> () -> NortheastCourtyardStructure.CODEC.codec();*/
            //?} else {
            () -> () -> NortheastCourtyardStructure.CODEC;
            //?}

    public static final Supplier<StructureType<NortheastCourtyardStructure>> COURTYARD_TYPE =
            STRUCTURE_TYPES.register("northeast_courtyard", COURTYARD_TYPE_SUPPLIER);

    public static final Supplier<StructurePieceType> COURTYARD_PIECE =
            PIECE_TYPES.register("northeast_courtyard",
                    () -> (StructurePieceType.StructureTemplateType) CourtyardPiece::new);

    public static final Supplier<StructurePieceType> BLACK_SOIL_PIECE =
            PIECE_TYPES.register("northeast_black_soil",
                    () -> (StructurePieceType.ContextlessType) BlackSoilPiece::new);

    public static final Supplier<StructurePieceType> BIOME_RICH_SOIL_PIECE =
            PIECE_TYPES.register("northeast_biome_rich_soil",
                    () -> (StructurePieceType.ContextlessType) BiomeRichSoilPiece::new);

    public static final Supplier<StructurePieceType> CLEARING_PIECE =
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
