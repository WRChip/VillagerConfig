package me.drex.villagerconfig.common.mixin;

import net.minecraft.world.entity.monster/*?if > 1.21.10 {*/.zombie/*?}*/.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static me.drex.villagerconfig.common.config.ConfigManager.CONFIG;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerMixin {

    // only the initial cure is scaled, readAdditionalSaveData also calls startConverting with the time left over
    @ModifyArg(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;startConverting(Ljava/util/UUID;I)V"
        ),
        index = 1
    )
    public int adjustCureTime(int conversionTime) {
        return Math.max(0, (int) (conversionTime * CONFIG.features.cureTime / 100));
    }

}
