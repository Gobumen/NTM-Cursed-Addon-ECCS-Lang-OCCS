package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.main.ClientProxy;
import com.hbm.render.tileentity.RenderControlPanel;
import com.hbm.tileentity.machine.TileEntityControlPanel;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderControlPanel.class)
public abstract class MixinRrenderControlPanel {
	@Shadow(remap = false)
	public abstract void renderCustomPanel(TileEntityControlPanel te,double x,double y,double z,float partialTicks,int destroyStage,float alpha);

	@Shadow(remap = false)
	public abstract void renderFrontPanel(TileEntityControlPanel te,double x,double y,double z,float partialTicks,int destroyStage,float alpha);

	/**
	 * @author ntmleafia
	 * @reason for inset instruments
	 */
	@Overwrite(remap = false)
	public void render(
			TileEntityControlPanel te,
			double x,
			double y,
			double z,
			float partialTicks,
			int destroyStage,
			float alpha) {
		GlStateManager.pushMatrix();

		GlStateManager.enableRescaleNormal();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + .5, y, z + .5);
		GlStateManager.translate(-0.5, 0, -0.5);
		te.panel.transform.store(ClientProxy.AUX_GL_BUFFER);
		ClientProxy.AUX_GL_BUFFER.rewind();
		GL11.glMultMatrix(ClientProxy.AUX_GL_BUFFER);
		te.updateTransform();
		te.panel.render();
		GlStateManager.popMatrix();
		GlStateManager.disableRescaleNormal();

		GlStateManager.color(1,1,1,1);

		switch (te.panelType) {
			case CUSTOM_PANEL:
				renderCustomPanel(te, x, y, z, partialTicks, destroyStage, alpha);
				break;
			case FRONT_PANEL:
				renderFrontPanel(te, x, y, z, partialTicks, destroyStage, alpha);
				break;
		}

		GlStateManager.popMatrix();
	}
}
