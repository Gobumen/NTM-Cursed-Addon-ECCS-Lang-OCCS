package com.leafia.contents.machines.controlpanel.instruments.types.starbound;

import com.hbm.inventory.control_panel.*;
import com.hbm.inventory.control_panel.controls.ControlType;
import com.hbm.inventory.control_panel.nodes.*;
import com.hbm.inventory.control_panel.nodes.NodeMath.Operation;
import com.hbm.render.loader.IModelCustom;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.leafia.contents.machines.controlpanel.instruments.types.AddonInstrumentModels;
import com.leafia.contents.machines.controlpanel.nodes.NodeSounder;
import com.leafia.contents.machines.controlpanel.nodes.NodeSounder.NodeSnd;
import com.leafia.transformer.LeafiaGls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static com.leafia.AddonBase.getIntegrated;

public class LargeButton extends Control {
	public LargeButton(String name,String registryName,ControlPanel panel) {
		super(name,registryName,panel);
		vars.put("isPushed",new DataValueFloat(0));
		vars.put("isLit",new DataValueFloat(0));
	}
	@Override
	public ControlType getControlType() {
		return ControlType.BUTTON;
	}
	@Override
	public float[] getSize() {
		return new float[]{ 6,6,0.5f };
	}
	@SideOnly(Side.CLIENT)
	@Override
	public IModelCustom getModel() {
		return AddonInstrumentModels.large_button;
	}
	static final ResourceLocation palette = getIntegrated("control_panel/instruments/largeswitch_palette.png");
	@Override
	public void render() {
		LeafiaGls.shadeModel(GL11.GL_SMOOTH);
		LeafiaGls.color(1,1,1);
		TextureManager texmg = Minecraft.getMinecraft().getTextureManager();
		WaveFrontObjectVAO mdl = (WaveFrontObjectVAO)getModel();
		texmg.bindTexture(palette);
		LeafiaGls.pushMatrix();
		LeafiaGls.translate(posX,0,posY);
		LeafiaGls.scale(3); // that was tiny as shit
		mdl.renderAllExcept("Circle.001");
		float lX = OpenGlHelper.lastBrightnessX;
		float lY = OpenGlHelper.lastBrightnessY;
		if (!vars.get("isLit").getBoolean())
			LeafiaGls.color(0.5f,0.5f,0.5f);
		else
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
		if (vars.get("isPushed").getBoolean())
			LeafiaGls.translate(0,-0.125,0);
		mdl.renderPart("Circle.001");
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lX,lY);
		LeafiaGls.popMatrix();
		LeafiaGls.shadeModel(GL11.GL_FLAT);
		LeafiaGls.color(1,1,1);
	}
	@Override
	public List<String> getOutEvents() {
		return Collections.singletonList("ctrl_press");
	}
	static final ResourceLocation guiRl = getIntegrated("control_panel/instruments/starboundbuttonlol.png");
	@SideOnly(Side.CLIENT)
	@Override
	public ResourceLocation getGuiTexture() {
		return guiRl;
	}
	@Override
	public Control newControl(ControlPanel controlPanel) {
		return new LargeButton(name,registryName,controlPanel);
	}
	/// mostly stolen code uwu
	@Override
	public void populateDefaultNodes(List<ControlEvent> receiveEvents) {
		NodeSystem ctrl_press = new NodeSystem(this);
		{
			NodeSounder node0 = new NodeSounder(230,90-(140-90));
			node0.snd = NodeSnd.BUTTON;
			ctrl_press.addNode(node0);

			NodeMath nodeBruh = new NodeMath(170,100).setData(Operation.ADD);
			nodeBruh.inputs.get(0).setDefault(new DataValueFloat(1));
			ctrl_press.addNode(nodeBruh); // the set variable node values doesn't get saved. BRUH

			NodeSetVar node2 = new NodeSetVar(230, 140, this).setData("isLit", false);
			node2.inputs.get(0).setData(nodeBruh,0,true);
			ctrl_press.addNode(node2);
			NodeSetVar node3 = new NodeSetVar(230, 90, this).setData("isPushed", false);
			node3.inputs.get(0).setData(nodeBruh,0,true);
			ctrl_press.addNode(node3);
		}
		receiveNodeMap.put("ctrl_press", ctrl_press);
		NodeSystem tick = new NodeSystem(this);
		{
			NodeGetVar node0 = new NodeGetVar(170, 100, this).setData("isPushed", false);
			tick.addNode(node0);
			NodeBuffer node1 = new NodeBuffer(230, 120);
			node1.inputs.get(0).setData(node0, 0, true);
			node1.inputs.get(1).setDefault(new DataValueFloat(90));
			tick.addNode(node1);
			NodeFunction node2 = new NodeFunction(290, 130);
			NodeSystem node2_subsystem = new NodeSystem(this);
			{
				node2_subsystem.addNode(new NodeSetVar(290, 90, this).setData("isPushed", false));
				node2_subsystem.addNode(new NodeSetVar(290, 140, this).setData("isLit", false));
			}
			node2.inputs.get(0).setData(node1, 0, true);
			tick.subSystems.put(node2, node2_subsystem);
			tick.addNode(node2);
		}
		receiveNodeMap.put("tick", tick);
	}
}
