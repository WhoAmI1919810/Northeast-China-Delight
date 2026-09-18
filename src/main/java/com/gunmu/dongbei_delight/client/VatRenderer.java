package com.gunmu.dongbei_delight.client;

import com.gunmu.dongbei_delight.block.Vat;
import com.gunmu.dongbei_delight.block.VatBlockEntity;
import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

/**
 * 把大缸里的东西按实际物品画出来 —— 不打开界面也能看见缸里有什么。
 *
 * 内容物铺在缸内（2x2 网格，超过 4 份往上叠）；压缸石压在最上面；
 * 蒙缸地毯和大酱液面用方块模型画，尺寸正好填满缸口与缸内。
 */
public class VatRenderer implements BlockEntityRenderer<VatBlockEntity> {

    public VatRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VatBlockEntity vat, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Level level = vat.getLevel();
        if (level == null) {
            return;
        }
        int light = LevelRenderer.getLightColor(level, vat.getBlockPos().above());
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        // 大酱：发酵完成后才是一缸酱色液体，液面随剩余碗数下降
        boolean fermented = vat.getBlockState().getValue(Vat.FERMENTED);
        if (vat.kind() == VatRecipes.Kind.PASTE && fermented && vat.paste() > 0) {
            float ratio = vat.paste() / (float) VatRecipes.PASTE_SERVINGS;
            float height = 0.15F + 0.7F * ratio;
            pose.pushPose();
            pose.translate(0.14F, 0.10F, 0.14F);
            pose.scale(0.72F, height, 0.72F);
            blockRenderer.renderSingleBlock(Blocks.BROWN_CONCRETE.defaultBlockState(), pose, buffer,
                    light, packedOverlay, ModelData.EMPTY, null);
            pose.popPose();
        }

        // 酱油：发酵完成后是一缸深褐色酱油，液面随剩余瓶数下降
        if (vat.kind() == VatRecipes.Kind.SOY_SAUCE && fermented && vat.soySauce() > 0) {
            float ratio = vat.soySauce() / (float) VatRecipes.SOY_SAUCE_SERVINGS;
            float height = 0.15F + 0.7F * ratio;
            pose.pushPose();
            pose.translate(0.14F, 0.10F, 0.14F);
            pose.scale(0.72F, height, 0.72F);
            blockRenderer.renderSingleBlock(Blocks.BROWN_TERRACOTTA.defaultBlockState(), pose, buffer,
                    light, packedOverlay, ModelData.EMPTY, null);
            pose.popPose();
        }

        // 内容物
        boolean done = vat.getBlockState().getValue(Vat.FERMENTED);
        List<ItemStack> contents = vat.contents();
        int shown = 0;
        for (int i = 0; i < contents.size() && shown < 12; i++) {
            ItemStack content = contents.get(i);
            // 盐化在水里、酱块化成一缸酱，都不单独显示
            if (content.is(ModItems.SALT.get())) {
                continue;
            }
            // 发酵完成后酱块化成了液体，不再单独显示
            if (done && (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)
                    && content.is(ModItems.SOY_PASTE_CHUNK.get())) {
                continue;
            }
            // 酱油酿好后小麦也化进了液体里
            if (done && vat.kind() == VatRecipes.Kind.SOY_SAUCE && content.is(VatRecipes.wheatInput())) {
                continue;
            }
            // 腌制完成后显示成品的模型
            ItemStack display = content;
            if (done) {
                Item result = VatRecipes.resultFor(content.getItem());
                if (result != null) {
                    display = new ItemStack(result);
                }
            }
            float x = 0.3F + (shown % 2) * 0.4F;
            float z = 0.3F + ((shown / 2) % 2) * 0.4F;
            float y = 0.32F + (shown / 4) * 0.06F;
            pose.pushPose();
            pose.translate(x, y, z);
            pose.scale(0.45F, 0.45F, 0.45F);
            itemRenderer.renderStatic(display, ItemDisplayContext.GROUND,
                    light, packedOverlay, pose, buffer, level, 0);
            pose.popPose();
            shown++;
        }

        // 压缸的石头：压在内容物上方，仍在缸内（不是上面一格）
        if (vat.isPressed()) {
            pose.pushPose();
            pose.translate(0.5F, 0.58F, 0.5F);
            pose.scale(0.5F, 0.5F, 0.5F);
            itemRenderer.renderStatic(vat.press(), ItemDisplayContext.GROUND,
                    light, packedOverlay, pose, buffer, level, 0);
            pose.popPose();
        }

        // 蒙缸的羊毛地毯：直接画方块模型，正好盖住缸口
        if (vat.isCovered() && vat.cover().getItem() instanceof BlockItem blockItem) {
            pose.pushPose();
            pose.translate(0.14F, 0.94F, 0.14F);
            pose.scale(0.72F, 1.0F, 0.72F);
            blockRenderer.renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffer,
                    light, packedOverlay, ModelData.EMPTY, null);
            pose.popPose();
        }
    }
}
