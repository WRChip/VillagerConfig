package me.drex.villagerconfig.common.mixin;

import me.drex.villagerconfig.common.data.TradeTable;
import me.drex.villagerconfig.common.util.CustomVillagerData;
import me.drex.villagerconfig.common.util.duck.IVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.drex.villagerconfig.common.config.ConfigManager.CONFIG;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements IVillager {

    @Shadow
    public abstract VillagerData getVillagerData();

    @Unique
    private int villagerConfig$breedsToday;

    @Unique
    private long villagerConfig$breedDay;

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(
        method = "updateTrades",
        at = @At(
            value = "HEAD"
        ),
        cancellable = true
    )
    public void putCustomTrades(/*? if > 1.21.10 {*/ServerLevel serverLevel, /*?}*/CallbackInfo ci) {
        TradeTable tradeTable = CustomVillagerData.getTradeTable((Villager) (Object) this);
        if (tradeTable != null) {
            VillagerData villagerData = this.getVillagerData();
            int level = villagerData./*? if >= 1.21.5 {*/ level() /*?} else {*/ /*getLevel() *//*?}*/;
            VillagerTrades.ItemListing[] tradeOffers = tradeTable.getTradeOffers(this, level);
            MerchantOffers tradeOfferList = this.getOffers();
            for (VillagerTrades.ItemListing tradeOffer : tradeOffers) {
                tradeOfferList.add(tradeOffer.getOffer(/*? if > 1.21.10 {*/serverLevel, /*?}*/this, this.random));
            }
            ci.cancel();
        }
    }

    @Redirect(
        method = "shouldIncreaseLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/villager/VillagerData;canLevelUp(I)Z"
        )
    )
    public boolean adjustMaxLevel(int level) {
        return CustomVillagerData.canLevelUp((Villager) (Object) this, level);
    }

    @Redirect(
        method = "shouldIncreaseLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/villager/VillagerData;getMaxXpPerLevel(I)I"
        )
    )
    public int adjustUpperLevelExperience(int level) {
        return CustomVillagerData.getMaxXpPerLevel((Villager) (Object) this, level);
    }

    @ModifyConstant(
        method = "allowedToRestock",
        constant = @Constant(intValue = 2)
    )
    public int adjustRestockLimit(int vanillaLimit) {
        int limit = CONFIG.features.maxRestocksPerDay;
        return limit < 0 ? vanillaLimit : limit;
    }

    // the work activity only runs from tick 2000 to 9000, so the vanilla 2400 tick gap fits 3 restocks at most
    @ModifyConstant(
        method = "allowedToRestock",
        constant = @Constant(longValue = 2400L)
    )
    public long adjustRestockDelay(long vanillaDelay) {
        int limit = CONFIG.features.maxRestocksPerDay;
        return limit <= 0 ? vanillaDelay : Math.min(vanillaDelay, 7000L / limit);
    }

    // allowedToRestock always lets the first restock of the day through, so 0 needs its own case
    @Inject(
        method = "allowedToRestock",
        at = @At("HEAD"),
        cancellable = true
    )
    public void denyRestock(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.features.maxRestocksPerDay == 0) cir.setReturnValue(false);
    }

    @Inject(
        method = "canBreed",
        at = @At("HEAD"),
        cancellable = true
    )
    public void limitBreeding(CallbackInfoReturnable<Boolean> cir) {
        int limit = CONFIG.features.maxBreedsPerDay;
        if (limit >= 0 && this.level() instanceof ServerLevel serverLevel && villagerConfig$breedCount(serverLevel) >= limit) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/npc/villager/Villager;",
        at = @At("RETURN")
    )
    public void countBreed(ServerLevel serverLevel, AgeableMob partner, CallbackInfoReturnable<Villager> cir) {
        if (cir.getReturnValue() == null) return;
        this.villagerConfig$recordBreed(serverLevel);
        if (partner instanceof IVillager other) other.villagerConfig$recordBreed(serverLevel);
    }

    @Inject(
        method = "addAdditionalSaveData",
        at = @At("TAIL")
    )
    public void saveBreedCount(ValueOutput output, CallbackInfo ci) {
        if (this.villagerConfig$breedsToday > 0) {
            output.putInt("VillagerConfigBreedsToday", this.villagerConfig$breedsToday);
            output.putLong("VillagerConfigBreedDay", this.villagerConfig$breedDay);
        }
    }

    @Inject(
        method = "readAdditionalSaveData",
        at = @At("TAIL")
    )
    public void loadBreedCount(ValueInput input, CallbackInfo ci) {
        this.villagerConfig$breedsToday = input.getIntOr("VillagerConfigBreedsToday", 0);
        this.villagerConfig$breedDay = input.getLongOr("VillagerConfigBreedDay", 0);
    }

    @Override
    public void villagerConfig$recordBreed(ServerLevel serverLevel) {
        this.villagerConfig$breedsToday = villagerConfig$breedCount(serverLevel) + 1;
    }

    @Unique
    private int villagerConfig$breedCount(ServerLevel serverLevel) {
        long day = serverLevel.getDayCount();
        if (day != this.villagerConfig$breedDay) {
            this.villagerConfig$breedDay = day;
            this.villagerConfig$breedsToday = 0;
        }
        return this.villagerConfig$breedsToday;
    }
}
