package com.leafia.overwrite_contents.mixin;

import com.leafia.transformer.WorldServerLeafia;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class MixinWorld {

    @Inject(method = "onEntityRemoved", at = @At("HEAD"))
    private void leafia$onEntityRemoved(Entity entityIn, CallbackInfo ci) {
        WorldServerLeafia.onEntityRemoved((World) (Object) this, entityIn);
    }
}
