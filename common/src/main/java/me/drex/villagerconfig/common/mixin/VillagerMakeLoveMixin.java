package me.drex.villagerconfig.common.mixin;

import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import static me.drex.villagerconfig.common.config.ConfigManager.CONFIG;

@Mixin(VillagerMakeLove.class)
public abstract class VillagerMakeLoveMixin {

    // both parents get this as their breeding cooldown, the baby's -24000 is a different constant
    // breeding only runs during the idle activity, whose longest window is tick 10 to 2000
    @ModifyConstant(
        method = "breed",
        constant = @Constant(intValue = 6000)
    )
    public int adjustBreedingCooldown(int vanillaCooldown) {
        int limit = CONFIG.features.maxBreedsPerDay;
        return limit <= 0 ? vanillaCooldown : Math.min(vanillaCooldown, 1990 / limit);
    }

}
