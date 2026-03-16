package com.leafia.contents.building.generic_doors.renderers;

import com.hbm.interfaces.IDoor.DoorState;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.hbm.render.tileentity.door.IRenderDoors;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.leafia.transformer.LeafiaGls;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.nio.DoubleBuffer;

import static com.leafia.init.ResourceInit.getIntegrated;
import static com.leafia.init.ResourceInit.getVAO;

public class ReactorDoorRender implements IRenderDoors {
	public static final ReactorDoorRender INSTANCE = new ReactorDoorRender();
	public static final WaveFrontObjectVAO mdl = getVAO(getIntegrated("doors/reactordoor/reactordoorfinal.obj"));
	public static final ResourceLocation tex = getIntegrated("doors/reactordoor/reactordoor.png");
	public static final ResourceLocation test = new ResourceLocation("hbm","textures/blocks/brick_concrete.png");
	@Override
	public void render(TileEntityDoorGeneric te,DoubleBuffer bufForClipping) {
		LeafiaGls.rotate(90,0,1,0);
		LeafiaGls.translate(0,0,-0.5);
		Minecraft.getMinecraft().getTextureManager().bindTexture(test);
		mdl.renderPart("FrameA");
		mdl.renderPart("FrameB");
		Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
		mdl.renderPart("Wall");

		double handleRatio = 0;
		double doorRatio = 0;
		if (te.state == DoorState.OPEN) {
			handleRatio = 1;
			doorRatio = 1;
		}
		if (te.currentAnimation != null) {
			handleRatio = IRenderDoors.getRelevantTransformation("HANDLE",te.currentAnimation)[1];
			doorRatio = IRenderDoors.getRelevantTransformation("DOOR",te.currentAnimation)[1];
		}

		LeafiaGls.translate(-0.625,0,0.5);
		LeafiaGls.rotate((float)(-140*doorRatio),0,1,0);
		LeafiaGls.translate(0.625,0,-0.5);
		mdl.renderPart("Door");

		LeafiaGls.translate(0,1.0625,0);
		LeafiaGls.rotate((float)(-90*handleRatio),0,0,1);
		LeafiaGls.translate(0,-1.0625,0);
		mdl.renderPart("Handle");
	}
}
