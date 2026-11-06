package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
//? if <1.20.2 {
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
//?} else {
/*import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
*///?}

import java.util.function.Supplier;

/**
 * 大脸盆（空盆）和水盆的分语境渲染：物品栏画 2D 贴图，其余场合（手、地面掉落物、展示框、头盔槽）画方块模型。
 * 两种盆各用一个实例，只是方块和图标模型不同（水盆的水面颜色由方块颜色处理器给，没有世界时用默认水色）。
 *
 * <p>为什么必须自定义渲染器：一个物品模型只有一套 quad，`isGui3d` 与 display 变不出两种几何，
 * 想「图标是 2D、拿在手里是方块」只能在拿到 {@link ItemDisplayContext} 的这里分流。
 * {@code models/item/large_basin.json} 的 parent 是 {@code builtin/entity}（isCustomRenderer），
 * 原版 {@code ItemRenderer.render} 会先套用 display 里对应视角的位移/缩放，再进到本类。</p>
 *
 * <ul>
 *   <li>GUI：画 {@code models/item/large_basin_icon.json}（水盆是 {@code water_basin_icon.json}）的普通 2D 贴图；</li>
 *   <li>其他：{@code renderSingleBlock} 画盆的方块，各视角的位移/缩放写在物品模型的 display 里
 *       （跟原版方块物品同一套数值）；头盔槽那 180 度倒扣也是 display.head 给的；</li>
 *   <li>粒子贴图取物品模型的 textures.particle。</li>
 * </ul>
 */
public class LargeBasinItemRenderer extends BlockEntityWithoutLevelRenderer
{
    /**
     * 客户端渲染器挂载点。1.20.1 由 {@code LargeBasinItem#initializeClient} / {@code WaterBasinItem#initializeClient} 取用，
     * 1.21.1 由 {@code ClientModEvents#onRegisterClientExtensions} 注册。
     */
    public static final IClientItemExtensions CLIENT_EXTENSION = new IClientItemExtensions()
    {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer()
        {
            return LargeBasinItemRenderer.get();
        }
    };

    public static final IClientItemExtensions WATER_BASIN_CLIENT_EXTENSION = new IClientItemExtensions()
    {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer()
        {
            return LargeBasinItemRenderer.water();
        }
    };

    private static LargeBasinItemRenderer instance;
    private static LargeBasinItemRenderer waterInstance;

    private final Supplier<Block> block;
    /**
     * 物品栏图标模型。它不在方块/物品的默认取模型路径上，得先由
     * {@code ClientModEvents#onRegisterAdditionalModels} 登记才会被烘焙。
     */
    //? if <1.20.2 {
    private final ResourceLocation iconModel;
    //?} else {
    /*private final ModelResourceLocation iconModel;
    *///?}

    private LargeBasinItemRenderer(Supplier<Block> block, String icon)
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.block = block;
        //? if <1.20.2 {
        this.iconModel = DdIds.of(NortheastChinaDelight.MODID, "item/" + icon);
        //?} else {
        /*this.iconModel = ModelResourceLocation.standalone(DdIds.of(NortheastChinaDelight.MODID, "item/" + icon));
        *///?}
    }

    /** 懒建：这个渲染器要等客户端起来才能造，不能放静态初始化。 */
    public static LargeBasinItemRenderer get()
    {
        if (instance == null)
        {
            instance = new LargeBasinItemRenderer(ModBlocks.LARGE_BASIN, "large_basin_icon");
        }
        return instance;
    }

    public static LargeBasinItemRenderer water()
    {
        if (waterInstance == null)
        {
            waterInstance = new LargeBasinItemRenderer(ModBlocks.WATER_BASIN, "water_basin_icon");
        }
        return waterInstance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay)
    {
        if (displayContext == ItemDisplayContext.GUI)
        {
            // 物品栏 / 配方书 / JEI：普通 2D 贴图，跟没有自定义渲染器的物品一模一样（GUI 里原版给的就是满亮）。
            // 物品栏用原版物品栏那种（translucentCullBlockSheet）：translucentItemSheet 在「极佳」里画到掉落物专用的画面上，物品栏里看不到（2026-11-06 用户：极佳下这几样物品栏没贴图）
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(),
                    buffer.getBuffer(Sheets.translucentCullBlockSheet()),
                    null,
                    Minecraft.getInstance().getModelManager().getModel(iconModel),
                    1.0F, 1.0F, 1.0F,
                    15728880,
                    packedOverlay);
        }
        else
        {
            // 手 / 地面 / 展示框 / 头顶：画盆的方块
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    block.get().defaultBlockState(),
                    poseStack, buffer, packedLight, packedOverlay);
        }
    }
}
