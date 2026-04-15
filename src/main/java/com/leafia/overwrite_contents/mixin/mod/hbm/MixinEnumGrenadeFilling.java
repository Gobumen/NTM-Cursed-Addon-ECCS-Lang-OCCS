package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.leafia.contents.miscellanous.slop.SlopTE;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.function.Consumer;

@Mixin(value = EnumGrenadeFilling.class,remap = false)
public class MixinEnumGrenadeFilling {
	@Shadow @Final @Mutable public static EnumGrenadeFilling[] VALUES;
	@Shadow @Final @Mutable private static EnumGrenadeFilling[] $VALUES;
	@Invoker("<init>")
	private static EnumGrenadeFilling leafia$constructor(String enumName,int enumOrdinal,Consumer<EntityGrenadeUniversal> explode,int bodyColor,int labelColor,EnumGrenadeShell... compatibleShells) {
		throw new AssertionError();
	}
	private static EnumGrenadeFilling NULL;
	@Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/hbm/items/weapon/grenade/ItemGrenadeFilling$EnumGrenadeFilling;values()[Lcom/hbm/items/weapon/grenade/ItemGrenadeFilling$EnumGrenadeFilling;", shift = At.Shift.BEFORE))
	private static void leafia$extendEnum(CallbackInfo ci) {
		int base = $VALUES.length;
		var if_null = leafia$constructor("NULL", base, (grenade)->{
			BlockPos pos = new BlockPos(grenade.posX,grenade.posY,grenade.posZ);
			for (int x = -3; x <= 3; x++) {
				for (int y = -3; y <= 3; y++) {
					for (int z = -3; z <= 3; z++)
						grenade.world.setBlockState(pos.add(x,y,z),Blocks.AIR.getDefaultState(),18);
				}
			}
			AxisAlignedBB aabb = new AxisAlignedBB(pos.getX()-3,pos.getY()-3,pos.getZ()-3,pos.getX()+4,pos.getY()+4,pos.getZ()+4);
			for (Entity entity : grenade.world.getEntitiesWithinAABBExcludingEntity(null,aabb))
				SlopTE.tryKill(entity);
		},0xc2c2c2,0x383838,EnumGrenadeShell.FRAG);
		var ext = Arrays.copyOf($VALUES, base + 1);
		ext[base] = if_null;
		$VALUES = VALUES = ext;
		NULL = if_null;
	}
	@Inject(method = "valueOf", at = @At("HEAD"), cancellable = true)
	private static void leafia$valueOf(String name, CallbackInfoReturnable<EnumGrenadeFilling> cir) {
		switch (name) {
			case "NULL":
				cir.setReturnValue(NULL);
				return;
			default:
		}
	}
}
