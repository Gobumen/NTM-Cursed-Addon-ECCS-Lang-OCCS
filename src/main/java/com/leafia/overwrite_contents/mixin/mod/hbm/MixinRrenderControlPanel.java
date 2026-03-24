package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.inventory.control_panel.Control;
import com.hbm.main.ClientProxy;
import com.hbm.render.tileentity.RenderControlPanel;
import com.hbm.tileentity.machine.TileEntityControlPanel;
import com.leafia.contents.machines.controlpanel.instruments.types.nixie.NixieDisplay;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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
        leafia$renderControlsByInsetType(x, y, z, te, true);
        GlStateManager.color(1,1,1,1);
		GlStateManager.pushMatrix();
		switch (te.panelType) {
			case CUSTOM_PANEL:
				renderCustomPanel(te, x, y, z, partialTicks, destroyStage, alpha);
				break;
			case FRONT_PANEL:
				renderFrontPanel(te, x, y, z, partialTicks, destroyStage, alpha);
				break;
		}
		GlStateManager.popMatrix();
        leafia$renderControlsByInsetType(x, y, z, te, false);
        GlStateManager.popMatrix();
	}

    @Unique
    private static void leafia$renderControlsByInsetType(double x, double y, double z, TileEntityControlPanel te, boolean insetPass) {
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + .5, y, z + .5);
        GlStateManager.translate(-0.5, 0, -0.5);
        te.panel.transform.store(ClientProxy.AUX_GL_BUFFER);
        ClientProxy.AUX_GL_BUFFER.rewind();
        GL11.glMultMatrix(ClientProxy.AUX_GL_BUFFER);
        te.updateTransform();
        for (Control control : te.panel.controls) {
            if (control instanceof NixieDisplay != insetPass) continue;
            float width = control.getSize()[0];
            float length = control.getSize()[1];
            GlStateManager.pushMatrix();
            GlStateManager.translate((width > 1 ? Math.abs(1 - width) / 2 : (width - 1) / 2), 0, (length > 1 ? Math.abs(1 - length) / 2 : (length - 1) / 2));
            control.render();
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
    }
}
