package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials.RBMKKeys;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRod;
import com.leafia.overwrite_contents.interfaces.IMixinTileEntityRBMKBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import scala.tools.asm.Opcodes;

@Mixin(value = TileEntityRBMKRod.class)
public abstract class MixinTileEntityRBMKRod extends TileEntityRBMKBase {
	@Shadow(remap = false) public boolean hasRod;

	@Redirect(method = "update",at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKRod;maxHeat()D",remap = false),require = 1)
	double leafia$onGetMaxHeat(TileEntityRBMKRod instance) {
		if (((IMixinTileEntityRBMKBase)this).leafia$getDamage() > IMixinTileEntityRBMKBase.maxDamage)
			return -100; // fuck you
		if (world.getGameRules().getBoolean(RBMKKeys.KEY_DISABLE_MELTDOWNS.keyString))
			return maxHeat();
		return Double.POSITIVE_INFINITY;
	}
	@Redirect(method = "update",at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKRod;meltdown()V",remap = false),require = 1)
	void leafia$onMeltdown(TileEntityRBMKRod instance) {
		// do nothing
	}
	@Redirect(method = "serialize",at = @At(value = "FIELD", target = "Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKRod;hasRod:Z",opcode = Opcodes.GETFIELD),remap = false,require = 1)
	boolean leafia$onSerialize(TileEntityRBMKRod instance) {
		if (!(instance.inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod))
			return false; // failsafe
		return hasRod;
	}
}
