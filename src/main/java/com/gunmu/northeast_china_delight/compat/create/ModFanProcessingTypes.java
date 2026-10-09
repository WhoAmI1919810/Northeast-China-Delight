package com.gunmu.northeast_china_delight.compat.create;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.item.ModItems;
//? if <1.20.2 || >=1.20.5 {
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
//?}
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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

import java.util.List;
import java.util.function.Supplier;

/**
 * Create 动力鼓风机的"裸风"处理类型（不接触水、火、熔岩、灵魂火时兜底）。
 * 目前唯一用途：把扔出来的碗装豆浆吹干，得到干豆腐 + 返还空碗。
 *
 * <p>1.20.4 节点没有机械动力（见 versions/1.20.4/gradle.properties 的说明），
 * 所以这条兼容整块停用，只留一个空的注册入口。</p>
 */
public class ModFanProcessingTypes {

    //? if <1.20.2 || >=1.20.5 {
    public static final DeferredRegister<FanProcessingType> FAN_PROCESSING_TYPES =
            DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, NortheastChinaDelight.MODID);

    /** 干燥（裸风）。优先级给最低，作为内置四种类型的兜底。 */
    public static final Supplier<FanProcessingType> DRYING =
            FAN_PROCESSING_TYPES.register("drying", DryingType::new);

    public static void register(IEventBus bus) {
        FAN_PROCESSING_TYPES.register(bus);
    }

    private static class DryingType implements FanProcessingType {
        @Override
        public boolean isValidAt(Level level, net.minecraft.core.BlockPos pos) {
            // 裸风 = 没有任何催化剂方块/流体。优先级最低，作为兜底即可。
            return true;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            // 矿物词典：别家模组的豆浆也能吹干
            return stack.is(com.gunmu.northeast_china_delight.ModTags.FOODS_SOY_MILK);
        }

        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            if (!canProcess(stack, level)) return null;
            // 鼓风机的 process 契约：返回的 List 第一个元素替换输入槽，其余的作为副产物掉落。
            return List.of(
                    new ItemStack(ModItems.DRIED_TOFU.get()),
                    new ItemStack(Items.BOWL)
            );
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            // 少量白色蒸汽，暗示"水分被吹走"。
            if (level.random.nextInt(6) != 0) return;
            RandomSource rand = level.random;
            level.addParticle(
                    ParticleTypes.CLOUD,
                    pos.x + (rand.nextFloat() - 0.5) * 0.5,
                    pos.y + 0.4,
                    pos.z + (rand.nextFloat() - 0.5) * 0.5,
                    0, 0.02, 0
            );
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess access, RandomSource rand) {
            // 把风染成偏白的暖色，视觉上区别于普通白色风，但又不像火/灵魂火那么艳。
            float t = rand.nextFloat() * 0.4F;
            int r = (int) (0xFF + (0xE8 - 0xFF) * t);
            int g = (int) (0xFF + (0xDC - 0xFF) * t);
            int b = (int) (0xFF + (0xC8 - 0xFF) * t);
            access.setColor((r << 16) | (g << 8) | b);
            access.setAlpha(1.0F);
            if (rand.nextFloat() < 1F / 32F) {
                access.spawnExtraParticle(ParticleTypes.CLOUD, 0.12F);
            }
        }

        @Override
        public void affectEntity(Entity entity, Level level) {
            // 裸风对实体无影响。
        }
    }
    //?} else {
    /*public static void register(IEventBus bus) {
        // 1.20.4 没有机械动力：「裸风风干」这类鼓风机处理类型不参与
    }*/
    //?}
}
