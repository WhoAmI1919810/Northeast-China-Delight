package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if <1.20.2 {
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
//?} else {
/*import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
*///?}

import java.util.HashMap;
import java.util.Map;

/**
 * 大盆菜（8 道用大脸盆盛装的料理）的分语境渲染：做法与大脸盆（{@link LargeBasinItemRenderer}）完全一致 ——
 * 物品栏画 2D 贴图，其余场合（手、地面掉落物、展示框）画方块模型。
 *
 * <p>一个物品模型只有一套 quad，变不出「GUI 是 2D、拿在手里是方块」两种几何，
 * 所以 {@code models/item/<id>.json} 的 parent 写 {@code builtin/entity}（isCustomRenderer），
 * 由本类拿到 {@link ItemDisplayContext} 后分流；display 里的位移 / 缩放照抄大脸盆那一套。</p>
 *
 * <ul>
 *   <li>GUI：按物品注册名取 {@code models/item/<注册名>_icon.json}（普通 2D 贴图）。
 *       这些图标模型不在默认取模型路径上，得先由 {@code ClientModEvents#onRegisterAdditionalModels} 登记；</li>
 *   <li>其他：{@code renderSingleBlock} 画该物品方块的默认状态（{@code DdFeastBlock}，朝北、还满着的那一档）；</li>
 *   <li>粒子贴图取物品模型的 textures.particle。</li>
 * </ul>
 */
public class DdFeastItemRenderer extends BlockEntityWithoutLevelRenderer
{
    /**
     * 客户端渲染器挂载点。1.20.1 由 {@code DdFeastItem#initializeClient} 取用，
     * 1.21.1 由 {@code ClientModEvents#onRegisterClientExtensions} 注册。
     */
    public static final IClientItemExtensions CLIENT_EXTENSION = new IClientItemExtensions()
    {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer()
        {
            return DdFeastItemRenderer.get();
        }
    };

    /** 图标模型缓存：模型名按物品注册名拼（item/<注册名>_icon），每帧现拼太浪费。 */
    //? if <1.20.2 {
    private static final Map<Item, ResourceLocation> ICON_MODELS = new HashMap<>();
    //?} else {
    /*private static final Map<Item, ModelResourceLocation> ICON_MODELS = new HashMap<>();
    *///?}

    private static DdFeastItemRenderer instance;

    private DdFeastItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet)
    {
        super(dispatcher, modelSet);
    }

    /** 懒建：这个渲染器要等客户端起来才能造，不能放静态初始化。 */
    public static DdFeastItemRenderer get()
    {
        if (instance == null)
        {
            instance = new DdFeastItemRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());
        }
        return instance;
    }

    //? if <1.20.2 {
    private static ResourceLocation iconModel(Item item)
    {
        return ICON_MODELS.computeIfAbsent(item, key -> DdIds.of(NortheastChinaDelight.MODID,
                "item/" + BuiltInRegistries.ITEM.getKey(key).getPath() + "_icon"));
    }
    //?} else {
    /*private static ModelResourceLocation iconModel(Item item)
    {
        return ICON_MODELS.computeIfAbsent(item, key -> ModelResourceLocation.standalone(DdIds.of(
                NortheastChinaDelight.MODID,
                "item/" + BuiltInRegistries.ITEM.getKey(key).getPath() + "_icon")));
    }
    *///?}

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay)
    {
        if (displayContext == ItemDisplayContext.GUI)
        {
            // 物品栏 / 配方书 / JEI：普通 2D 贴图，跟没有自定义渲染器的物品一模一样（GUI 里原版给的就是满亮）。
            // 物品栏用原版物品栏那种（translucentCullBlockSheet）：translucentItemSheet 在「极佳」里画到掉落物专用的画面上，物品栏里看不到
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(),
                    buffer.getBuffer(Sheets.translucentCullBlockSheet()),
                    null,
                    Minecraft.getInstance().getModelManager().getModel(iconModel(stack.getItem())),
                    1.0F, 1.0F, 1.0F,
                    15728880,
                    packedOverlay);
        }
        else
        {
            // 手 / 地面 / 展示框：画满盆那一档方块模型
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    ((BlockItem) stack.getItem()).getBlock().defaultBlockState(),
                    poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
