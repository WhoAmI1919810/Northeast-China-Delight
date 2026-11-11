package com.gunmu.northeast_china_delight.compat.create;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.DryingRecipes;
//? if <1.20.2 || >=1.20.5 {
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

import java.util.List;
import java.util.function.Supplier;

/**
 * Create 动力鼓风机的"裸风"处理类型（不接触水、火、熔岩、灵魂火时兜底）。目前唯一用途：把扔出来的碗装豆浆吹干，得到干豆腐 + 返还空碗。
 */
public class ModFanProcessingTypes {

    //? if <1.20.2 || >=1.20.5 {
    public static final DeferredRegister<FanProcessingType> FAN_PROCESSING_TYPES =
            DeferredRegister.create(CreateRegistries.FAN_PROCESSING_TYPE, NortheastChinaDelight.MODID);

    public static final Supplier<FanProcessingType> DRYING =
            FAN_PROCESSING_TYPES.register("drying", DryingType::new);

    public static void register(IEventBus bus) {
        FAN_PROCESSING_TYPES.register(bus);
    }

    private static class DryingType implements FanProcessingType {
        /**
         * 只在气流最前端的空气格成立，也就是鼓风机正前方那一格、而且没有被方块挡住。
         *
         * Create 重建气流时靠这个判断某一段气流算哪种处理，返回 true 的位置会成为整段的类型。
         * 所以这里不能对所有位置都返回 true：那样每一段气流都会被判成干燥，
         * 烟熏/鼓风/喷溅/缠魂全都选不中，整合包里鼓风机什么都加工不了。
         * 只认鼓风机正前方、又没有方块的位置，往后的空气格在 Create 眼里仍然没有类型，
         * 会沿用前面找到的催化剂，原版的四种处理不受影响。
         */
        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            if (!level.getBlockState(pos).isAir()) return false;
            for (Direction direction : Direction.values()) {
                BlockState fanState = level.getBlockState(pos.relative(direction));
                if (fanState.getBlock() instanceof EncasedFanBlock
                        && fanState.getValue(EncasedFanBlock.FACING) == direction.getOpposite()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return DryingRecipes.byInput(level.getRecipeManager(), stack) != null;
        }

        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            DryingRecipes.Recipe recipe = DryingRecipes.byInput(level.getRecipeManager(), stack);
            return recipe == null ? null : recipe.process(stack);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
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
        }
    }
    //?}
}
