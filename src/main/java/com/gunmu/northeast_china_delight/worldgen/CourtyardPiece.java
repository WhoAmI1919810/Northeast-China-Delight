package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class CourtyardPiece extends TemplateStructurePiece
{
    public CourtyardPiece(StructureTemplateManager templateManager, String templateId, BlockPos pos, Rotation rotation)
    {
        super(ModWorldGen.COURTYARD_PIECE.get(), 0, templateManager,
                DdIds.parse(templateId), templateId,
                settings(rotation), pos);
    }

    public CourtyardPiece(StructureTemplateManager templateManager, CompoundTag tag)
    {
        super(ModWorldGen.COURTYARD_PIECE.get(), tag, templateManager,
                id -> settings(Rotation.NONE));
    }

    private static StructurePlaceSettings settings(Rotation rotation)
    {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
    {
        super.addAdditionalSaveData(context, tag);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box)
    {
    }

    public String templateId()
    {
        return this.templateName;
    }
}
