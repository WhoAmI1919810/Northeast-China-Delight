package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if <1.20.2 {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*///?} else {
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
//?}

/**
 * 木槌的分语境渲染：物品栏画 16x16 的 2D 贴图，其余场合（手、地面掉落物、展示框）画 Blockbench 的 3D 模型。
 *
 * <p>一个物品模型只有一套 quad，变不出「GUI 是贴图、拿在手里是模型」两种几何，
 * 所以 {@code models/item/wooden_mallet.json} 的 parent 写 {@code builtin/entity}（isCustomRenderer），
 * 由本类拿到 {@link ItemDisplayContext} 后分流；各视角的位移 / 缩放写在那个物品模型的 display 里。</p>
 *
 * <ul>
 *   <li>GUI：画 {@code models/item/wooden_mallet_icon.json}（普通 2D 贴图）；</li>
 *   <li>其他：画 {@code models/item/wooden_mallet_model.json}（3D 模型）；</li>
 *   <li>两个模型都不在默认取模型路径上，得先由 {@code ClientModEvents#onRegisterAdditionalModels} 登记。</li>
 * </ul>
 */
public class WoodenMalletItemRenderer extends BlockEntityWithoutLevelRenderer
{
    /**
     * 客户端渲染器挂载点。1.20.1 由 {@code WoodenMalletItem#initializeClient} 取用，
     * 1.21.1 由 {@code ClientModEvents#onRegisterClientExtensions} 注册。
     */
    public static final IClientItemExtensions CLIENT_EXTENSION = new IClientItemExtensions()
    {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer()
        {
            return WoodenMalletItemRenderer.get();
        }
    };

    //? if <1.20.2 {
    /*private static final ResourceLocation ICON_MODEL = DdIds.of(NortheastChinaDelight.MODID, "item/wooden_mallet_icon");
    private static final ResourceLocation HAND_MODEL = DdIds.of(NortheastChinaDelight.MODID, "item/wooden_mallet_model");
    *///?} else {
    private static final ModelResourceLocation ICON_MODEL =
            ModelResourceLocation.standalone(DdIds.of(NortheastChinaDelight.MODID, "item/wooden_mallet_icon"));
    private static final ModelResourceLocation HAND_MODEL =
            ModelResourceLocation.standalone(DdIds.of(NortheastChinaDelight.MODID, "item/wooden_mallet_model"));
    //?}

    private static WoodenMalletItemRenderer instance;

    private WoodenMalletItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet)
    {
        super(dispatcher, modelSet);
    }

    /** 懒建：这个渲染器要等客户端起来才能造，不能放静态初始化。 */
    public static WoodenMalletItemRenderer get()
    {
        if (instance == null)
        {
            instance = new WoodenMalletItemRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay)
    {
        boolean gui = displayContext == ItemDisplayContext.GUI;
        // 附魔光效得自己贴：自定义渲染器不走 ItemRenderer 那条路，要用 getFoilBufferDirect 的双写 buffer（Glint 层 + 模型层），否则附了魔也没有光
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                buffer, Sheets.translucentItemSheet(), true, stack.hasFoil());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(),
                consumer,
                null,
                Minecraft.getInstance().getModelManager().getModel(gui ? ICON_MODEL : HAND_MODEL),
                1.0F, 1.0F, 1.0F,
                gui ? 15728880 : packedLight,
                packedOverlay);
    }
}
