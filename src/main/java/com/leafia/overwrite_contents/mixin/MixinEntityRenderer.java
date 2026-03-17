package com.leafia.overwrite_contents.mixin;

import com.leafia.contents.worldgen.biomes.effects.HasAcidicRain;
import com.leafia.settings.AddonConfig;
import com.leafia.transformer.LeafiaGeneralLocal;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(method = "addRainParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V", ordinal = 0))
    private void leafia$spawnAcidRainParticles(World world, EnumParticleTypes particleType, double x, double y,
                                               double z, double xSpeed, double ySpeed, double zSpeed, int[] parameters,
                                               @Local(name = "entity") Entity entity,
                                               @Local(name = "biome") Biome biome,
                                               @Local(name = "iblockstate") IBlockState state,
                                               @Local(name = "blockpos2") BlockPos down, @Local(name = "d3") double rx,
                                               @Local(name = "d4") double rz,
                                               @Local(name = "axisalignedbb") AxisAlignedBB boundingBox) {
        if (!AddonConfig.enableAcidRainRender || LeafiaGeneralLocal.acidRainParticles(entity, biome, state, down, rx,
                rz, boundingBox)) {
            world.spawnParticle(particleType, x, y, z, xSpeed, ySpeed, zSpeed, parameters);
        }
    }

    @Redirect(method = "renderRainSnow", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", ordinal = 0))
    private void leafia$bindAcidRainTexture(TextureManager textureManager, ResourceLocation texture,
                                            @Local(name = "biome") Biome biome) {
        if (AddonConfig.enableAcidRainRender && biome instanceof HasAcidicRain) {
            textureManager.bindTexture(LeafiaGeneralLocal.acidRain);
            return;
        }
        textureManager.bindTexture(texture);
    }
}
