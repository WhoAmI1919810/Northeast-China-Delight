package com.gunmu.northeast_china_delight.worldgen;

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

/**
 * 放下一个院子 NBT 的结构片段。
 *
 * 只做「把模板摆到世界上」这一件事，尺寸、朝向全部由模板和 {@link StructurePlaceSettings} 决定。
 */
public class CourtyardPiece extends TemplateStructurePiece
{
    public CourtyardPiece(StructureTemplateManager templateManager, String templateId, BlockPos pos, Rotation rotation)
    {
        super(ModWorldGen.COURTYARD_PIECE.get(), 0, templateManager,
                net.minecraft.resources.ResourceLocation.parse(templateId), templateId,
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
        // 模板里没有用 data marker，留空
    }

    /** 让 Java 侧也能拿到模板名字（调试用） */
    public String templateId()
    {
        return this.templateName;
    }
}
