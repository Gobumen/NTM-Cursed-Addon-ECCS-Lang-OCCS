package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.render.tileentity.RenderRBMKLid;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRod;
import com.leafia.transformer.LeafiaGls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/// this whole thing was murdered
@Mixin(value = RenderRBMKLid.class)
public abstract class MixinRenderRBMKLid {
	/*
	@Shadow(remap = false)
	protected abstract void renderColumnStack(TileEntityRBMKBase control,int offset);

	@Shadow(remap = false)
	protected abstract void renderLid(TileEntityRBMKBase control,int offset);

	@Shadow(remap = false)
	protected abstract void renderFuelRodStack(TileEntityRBMKBase control,float r,float g,float b,int offset);

	@Shadow(remap = false)
	protected abstract void renderCherenkovEffect(TileEntityRBMKBase control,float r,float g,float b,float a,int offset);
*/
	/*
	@Overwrite(remap = false)
	public void render(TileEntityRBMKBase control,double x,double y,double z,float partialTicks,int destroyStage,float alpha) {
		boolean hasRod = false;
		boolean cherenkov = false;
		float fuelR = 0F;
		float fuelG = 0F;
		float fuelB = 0F;
		float cherenkovR = 0F;
		float cherenkovG = 0F;
		float cherenkovB = 0F;
		float cherenkovA = 0.1F;

		if(control instanceof TileEntityRBMKRod rod) {
			if(rod.hasRod) {
				hasRod = true;
				fuelR = rod.fuelR;
				fuelG = rod.fuelG;
				fuelB = rod.fuelB;
				cherenkovR = rod.cherenkovR;
				cherenkovG = rod.cherenkovG;
				cherenkovB = rod.cherenkovB;
			}
			if(rod.fluxQuantity > 5) {
				cherenkov = true;
				cherenkovA = (float) Math.max(0.25F, Math.log(rod.fluxQuantity) * 0.01F);
			}
		}
		int offset = 1;
		for (int o = 1; o < 16; o++){
			if (control.getWorld().getBlockState(control.getPos().up(o)).getBlock() == control.getBlockType()) {
				offset = o;
				int meta = control.getWorld().getBlockState(control.getPos().up(o)).getBlock().getMetaFromState(control.getWorld().getBlockState(control.getPos().up(o)));
				if (meta > 5 && meta < 12) break;
			} else break;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y, z + 0.5);

		if(!(control.getBlockType() instanceof RBMKBase block)) {
			GlStateManager.popMatrix();
			return;
		}

		Minecraft.getMinecraft().getTextureManager().bindTexture(block.columnTexture);
		LeafiaGls.pushMatrix();
		LeafiaGls.scale(1,1+control.jumpheight/offset,1);
		renderColumnStack(control, offset + 1);
		if(hasRod)
			renderFuelRodStack(control, fuelR, fuelG, fuelB, offset + 1);
		LeafiaGls.popMatrix();

		if(control.hasLid())
			renderLid(control, offset);
		if(cherenkov)
			renderCherenkovEffect(control, cherenkovR, cherenkovG, cherenkovB, cherenkovA, offset);

		GlStateManager.popMatrix();
	}*/
}
