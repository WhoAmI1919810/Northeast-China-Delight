package com.gunmu.dongbei_delight;

import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.block.ModBlockEntities;
import com.gunmu.dongbei_delight.block.GrillCampfireBlock;
import com.gunmu.dongbei_delight.block.Vat;
import com.gunmu.dongbei_delight.block.VatBlockEntity;
import com.gunmu.dongbei_delight.client.GrillCampfireRenderer;
import com.gunmu.dongbei_delight.client.VatRenderer;
import com.gunmu.dongbei_delight.effect.DishEffectEvents;
import com.gunmu.dongbei_delight.event.ModGameplayEvents;
import com.gunmu.dongbei_delight.effect.ModEffects;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import com.gunmu.dongbei_delight.item.ModItems;
import com.gunmu.dongbei_delight.loot.ModLootModifiers;
import com.gunmu.dongbei_delight.villager.ModVillagerProfessions;
import com.gunmu.dongbei_delight.villager.ModButcherTrades;
import com.gunmu.dongbei_delight.villager.ModCartographerTrades;
import com.gunmu.dongbei_delight.villager.ModFarmerTrades;
import com.gunmu.dongbei_delight.villager.ModFishermanTrades;
import com.gunmu.dongbei_delight.villager.ModWandererTrades;
import com.gunmu.dongbei_delight.villager.SideDishMerchantTrades;
import com.gunmu.dongbei_delight.worldgen.ModWorldGen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;


// modid 必须与 neoforge.mods.toml 中的条目保持一致
@Mod(DongbeiDelight.MODID)
public class DongbeiDelight
{
    public static final String MODID = "dongbei_delight";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DongbeiDelight(IEventBus modEventBus, ModContainer modContainer)
    {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModEffects.register(modEventBus);
        ModVillagerProfessions.register(modEventBus);
        ModWorldGen.register(modEventBus);
        // 模组配置（菜肴方块形态的总开关）
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, DongbeiConfig.SPEC);

        // 将物品添加到本模组的创造模式物品栏
        modEventBus.addListener(this::addItemsToCreativeTab);
        // 大缸的流体能力：让流体管道能把酱油 / 大酱抽进储罐
        modEventBus.addListener(this::registerCapabilities);

        // 注册服务器及其他游戏事件
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(DishEffectEvents.class);
        // 木耳（给原木去皮）与人参（空手刨土）的钩子
        NeoForge.EVENT_BUS.register(ModGameplayEvents.class);
        // 副食商的交易表（每次数据包重载时重新填一遍）
        NeoForge.EVENT_BUS.addListener(SideDishMerchantTrades::addTrades);
        // 原版各职业的交易表：农民 / 屠夫 / 渔夫 / 制图师 / 流浪商人
        NeoForge.EVENT_BUS.addListener(ModFarmerTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModButcherTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModFishermanTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModCartographerTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModWandererTrades::addTrades);
    }

    private void addItemsToCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey().equals(ModCreativeTabs.INGREDIENTS_TAB_KEY))
        {
            ModItems.INGREDIENT_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
        else if (event.getTabKey().equals(ModCreativeTabs.DISHES_TAB_KEY))
        {
            ModItems.DISH_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
        else if (event.getTabKey().equals(ModCreativeTabs.BLOCKS_TAB_KEY))
        {
            ModItems.BLOCK_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
    }

    private void registerCapabilities(@NotNull RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.VAT.get(),
                (vat, side) -> vat);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("da dong bei shi wo di jia xiang ~");
    }

    /**
     * 拿着烧烤架右键普通营火：把营火换成「架了烤架的营火」。
     *
     * 这里是另换一个方块，而不是给原版营火加一个方块状态属性 ——
     * 给原版方块加属性会改变全局方块状态 id，老存档里的方块会错位。
     */
    @SubscribeEvent
    public void onRightClickCampfire(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.isCanceled()
                || !event.getItemStack().is(ModItems.GRILL_RACK.get()))
        {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.CAMPFIRE) || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity fire))
        {
            return;
        }
        // 客户端先拦下来，避免本地预测跑一遍原版营火的交互
        event.setCanceled(true);
        // 必须给一个「已消费」的结果：默认的 PASS 会让客户端接着去处理副手，
        // 副手那次「空手右键」打在刚装好的烤架营火上，会把烤架又撸下来
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide)
        {
            return;
        }
        GrillCampfireBlock.install(level, pos, state, fire);
        if (!event.getEntity().getAbilities().instabuild)
        {
            event.getItemStack().shrink(1);
        }
        level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("da dong bei shi wo di jia xiang ~");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        /**
         * 大缸里的水面用原版 water_still 贴图，而那张贴图本身是灰白的，
         * 蓝色来自群系着色 —— 所以这里给水面（tintindex 0）注册水的颜色。
         */
        @SubscribeEvent
        public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event)
        {
            event.register(
                    (state, level, pos, tintIndex) -> {
                        // 酸引水：泡菜腌完后缸里的水，用发浑的淡黄色和其它液体区分开
                        if (level != null && pos != null
                                && level.getBlockEntity(pos) instanceof VatBlockEntity vat
                                && vat.sourWaterMb() > 0) {
                            return 0xD9D2AE;
                        }
                        int color = level != null && pos != null
                                ? BiomeColors.getAverageWaterColor(level, pos)
                                : 0x3F76E4;
                        // 盐够了的水稍微发白，用来提示「可以压石头开腌了」
                        if (state.getValue(Vat.SALTED)) {
                            int r = (color >> 16) & 0xFF;
                            int g = (color >> 8) & 0xFF;
                            int b = color & 0xFF;
                            r += (int) ((255 - r) * 0.45F);
                            g += (int) ((255 - g) * 0.45F);
                            b += (int) ((255 - b) * 0.45F);
                            color = (r << 16) | (g << 8) | b;
                        }
                        // 缸里泡着的东西会给水染色：辣椒酱发红、酱块发酱色、蔬菜发淡绿 …
                        if (level != null && pos != null
                                && level.getBlockEntity(pos) instanceof VatBlockEntity vat) {
                            float weight = 1.0F;
                            for (ItemStack content : vat.contents()) {
                                int tint = com.gunmu.dongbei_delight.client.VatItemShapes.liquidTint(content);
                                if (tint == 0xFFFFFF) {
                                    continue;
                                }
                                float strength = com.gunmu.dongbei_delight.client.VatItemShapes
                                        .liquidStrength(content) * weight;
                                color = blend(color, tint, strength);
                                weight *= 0.55F;
                            }
                        }
                        return color;
                    },
                    ModBlocks.VAT.get());
        }

        /** 按比例把两个颜色混起来（amount = 0 全用 base，= 1 全用 tint） */
        private static int blend(int base, int tint, float amount)
        {
            float a = Math.max(0.0F, Math.min(1.0F, amount));
            int br = (base >> 16) & 0xFF;
            int bg = (base >> 8) & 0xFF;
            int bb = base & 0xFF;
            int tr = (tint >> 16) & 0xFF;
            int tg = (tint >> 8) & 0xFF;
            int tb = tint & 0xFF;
            int r = (int) (br + (tr - br) * a);
            int g = (int) (bg + (tg - bg) * a);
            int b = (int) (bb + (tb - bb) * a);
            return (r << 16) | (g << 8) | b;
        }

        /**
         * 调料瓶照原版药水的做法：瓶身和液面都用原版的贴图（potion / potion_overlay），
         * 这里只给液面那一层（tintindex 0）按物品染上对应颜色，瓶子那层不染。
         * 颜色表在 {@link com.gunmu.dongbei_delight.item.BottleColors}。
         */
        @SubscribeEvent
        public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event)
        {
            event.register(
                    (stack, tintIndex) -> tintIndex == 0
                            ? com.gunmu.dongbei_delight.item.BottleColors.of(stack.getItem())
                            : -1,
                    com.gunmu.dongbei_delight.item.BottleColors.bottles());
        }

        /** 大缸里的内容物需要渲染在方块上，所以注册方块实体渲染器 */
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerBlockEntityRenderer(ModBlockEntities.VAT.get(), VatRenderer::new);
            // 烤架营火：火堆用方块模型，架上的东西和那层烤架由这个渲染器画
            event.registerBlockEntityRenderer(ModBlockEntities.GRILL_CAMPFIRE.get(), GrillCampfireRenderer::new);
        }

        /**
         * 酱油、大酱在流体储罐 / 管道里的贴图。
         * 没有这一段的话，机械动力的储罐会显示成紫黑格。
         */
        @SubscribeEvent
        public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
        {
            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_sauce_flow");
                }
            }, ModFluids.SOY_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_paste_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_paste_flow");
                }
            }, ModFluids.SOY_PASTE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_milk_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_milk_flow");
                }
            }, ModFluids.SOY_MILK_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vinegar_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vinegar_flow");
                }
            }, ModFluids.VINEGAR_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/sour_water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/sour_water_flow");
                }
            }, ModFluids.SOUR_WATER_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/white_vinegar_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/white_vinegar_flow");
                }
            }, ModFluids.WHITE_VINEGAR_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/fish_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/fish_sauce_flow");
                }
            }, ModFluids.FISH_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/shrimp_paste_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/shrimp_paste_flow");
                }
            }, ModFluids.SHRIMP_PASTE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vegetable_oil_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vegetable_oil_flow");
                }
            }, ModFluids.VEGETABLE_OIL_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/animal_oil_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/animal_oil_flow");
                }
            }, ModFluids.ANIMAL_OIL_TYPE);
        }
    }
}
