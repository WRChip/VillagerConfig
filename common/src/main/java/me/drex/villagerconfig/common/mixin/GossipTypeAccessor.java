package me.drex.villagerconfig.common.mixin;

import net.minecraft.world.entity.ai.gossip.GossipType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GossipType.class)
public interface GossipTypeAccessor {
    @Mutable
    @Accessor("max")
    void setMax(int max);
}
