package com.leafia.contents.machines.misc.modular_turbine;

import com.hbm.render.loader.WaveFrontObjectVAO;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase.TurbineComponentType;
import com.leafia.transformer.LeafiaGls;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import static com.leafia.AddonBase.getIntegrated;
import static com.leafia.init.ResourceInit.getVAO;

public class ModularTurbineComponentRender extends TileEntitySpecialRenderer<ModularTurbineComponentTE> {
	static final String basePath = "machines/modular_turbines/";
	public static final WaveFrontObjectVAO mdl = getVAO(getIntegrated(basePath+"export.obj"));
	public static final ResourceLocation tex0 = getIntegrated(basePath+"texture0.png");
	public static final ResourceLocation tex1 = getIntegrated(basePath+"texture1.png");
	public static final WaveFrontObjectVAO genMdl = getVAO(getIntegrated(basePath+"gen3x3.obj"));
	public static final ResourceLocation gen3x3tetx = getIntegrated(basePath+"generator3x3.png");
	@Override
	public void render(ModularTurbineComponentTE te,double x,double y,double z,float partialTicks,int destroyStage,float alpha) {
		LeafiaGls.pushMatrix();
		LeafiaGls.translate(x+0.5,y,z+0.5);
		switch(te.getBlockMetadata()-10) {
			case 2 -> LeafiaGls.rotate(180,0F,1F,0F);
			case 3 -> LeafiaGls.rotate(0,0F,1F,0F);
			case 4 -> LeafiaGls.rotate(270,0F,1F,0F);
			case 5 -> LeafiaGls.rotate(90,0F,1F,0F);
		}
		LeafiaGls.shadeModel(GL11.GL_SMOOTH);
		if (te.getBlockType() instanceof ModularTurbineBlockBase block) {
			if (block.componentType() == TurbineComponentType.BLADES) {
				String name = "Tube"+block.size();
				bindTexture(tex0);
				LeafiaGls.disableCull();
				mdl.renderPart(name+".001");
				LeafiaGls.enableCull();
				bindTexture(tex1);
				mdl.renderPart(name);
			} else if (block.componentType() == TurbineComponentType.FLYWHEEL) {
				String name = "Tube"+block.size();
				bindTexture(tex0);
				LeafiaGls.disableCull();
				mdl.renderPart(name+".001");
				LeafiaGls.enableCull();
			} else if (block.componentType() == TurbineComponentType.GENERATOR) {
				switch(block.size()) {
					case 3 -> {
						bindTexture(gen3x3tetx);
						genMdl.renderAll();
					}
				}
			} else {
				bindTexture(tex1);
				switch(block.size()) {
					case 2: case 3:
						mdl.renderPart("Block2_3");
						break;
					case 5:
						mdl.renderPart("Block5");
						break;
					case 7:
						mdl.renderPart("Block7");
						break;
					case 9:
						mdl.renderPart("Block9");
						break;
				}
			}
		}
		LeafiaGls.shadeModel(GL11.GL_FLAT);
		LeafiaGls.popMatrix();
	}
}
