package com.leafia.contents.machines.elevators.floors;

import com.hbm.render.loader.WaveFrontObjectVAO;
import com.leafia.contents.AddonBlocks;
import com.leafia.contents.machines.elevators.car.ElevatorRender.S6;
import com.leafia.contents.machines.elevators.car.ElevatorRender.Skylift;
import com.leafia.dev.LeafiaBrush;
import com.leafia.transformer.LeafiaGls;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

import com.leafia.contents.AddonBlocks.Elevators;
import static com.leafia.contents.machines.elevators.car.ElevatorRender.model;

public class EvFloorRender extends TileEntitySpecialRenderer<EvFloorTE> {
	WaveFrontObjectVAO mdl = model("floor_walled");
	@Override
	public void render(EvFloorTE te,double x,double y,double z,float partialTicks,int destroyStage,float alpha) {
		LeafiaGls.pushMatrix();
		LeafiaGls.translate(x+0.5,y,z+0.5);
		switch(te.getBlockMetadata() - 10) {
			case 2:
				GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 3:
				GL11.glRotatef(0, 0F, 1F, 0F); break;
			case 4:
				GL11.glRotatef(270, 0F, 1F, 0F); break;
			case 5:
				GL11.glRotatef(90, 0F, 1F, 0F); break;
		}
		Block bluk = te.getBlockType();
		if (bluk == Elevators.s6_floor)
			bindTexture(S6.door);
		if (bluk == Elevators.skylift_floor)
			bindTexture(Skylift.frame);
		mdl.renderPart("Frames");
		float door = te.open.get();
		if (bluk == Elevators.skylift_floor)
			bindTexture(Skylift.door);
		LeafiaGls.pushMatrix();
		LeafiaGls.translate(-0.440625f*door,0,0);
		mdl.renderPart("DoorL");
		LeafiaGls.popMatrix();
		LeafiaGls.pushMatrix();
		LeafiaGls.translate(0.440625f*door,0,0);
		mdl.renderPart("DoorR");
		LeafiaGls.popMatrix();
		if (bluk == Elevators.skylift_floor)
			bindTexture(Skylift.frame);

		boolean on = false;
		if (te.pulley != null && te.pulley.elevator != null)
			on = te.pulley.elevator.enabledButtons.contains("floor"+te.floor) || te.pulley.elevator.clickedButtons.containsKey("floor"+te.floor);
		if (bluk == Elevators.s6_floor)
			bindTexture(on ? S6.buttonOn : S6.buttonOff);
		else if (bluk == Elevators.skylift_floor)
			bindTexture(on ? Skylift.buttonOn : Skylift.buttonOff);
		LeafiaBrush brush = LeafiaBrush.instance;
		float staticX = 0.125f*5;
		float staticY = 0.125f*8;
		float staticZ = 0.501f;
		brush.startDrawingQuads();
		if (bluk == Elevators.s6_floor) {
			brush.addVertexWithUV(staticX+0.15/16d,staticY+0.15/16d,staticZ,0,1);
			brush.addVertexWithUV(staticX+0.85/16d,staticY+0.15/16d,staticZ,1,1);
			brush.addVertexWithUV(staticX+0.85/16d,staticY+0.85/16d,staticZ,1,0);
			brush.addVertexWithUV(staticX+0.15/16d,staticY+0.85/16d,staticZ,0,0);
		}
		if (bluk == Elevators.skylift_floor) {
			brush.addVertexWithUV(staticX-0.25/16d,staticY-0.25/16d,staticZ,0,1);
			brush.addVertexWithUV(staticX+1.25/16d,staticY-0.25/16d,staticZ,1,1);
			brush.addVertexWithUV(staticX+1.25/16d,staticY+1.25/16d,staticZ,1,0);
			brush.addVertexWithUV(staticX-0.25/16d,staticY+1.25/16d,staticZ,0,0);
		}
		brush.draw();

		LeafiaGls.popMatrix();
	}
}
