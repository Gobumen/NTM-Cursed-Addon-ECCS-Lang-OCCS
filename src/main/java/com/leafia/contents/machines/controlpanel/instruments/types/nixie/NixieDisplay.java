package com.leafia.contents.machines.controlpanel.instruments.types.nixie;

import com.hbm.inventory.control_panel.*;
import com.hbm.inventory.control_panel.controls.ControlType;
import com.hbm.inventory.control_panel.controls.configs.SubElementBaseConfig;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.IModelCustom;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.leafia.AddonBase;
import com.leafia.contents.machines.controlpanel.instruments.types.AddonInstrumentModels;
import com.leafia.transformer.LeafiaGls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;


public class NixieDisplay extends Control {

	private float[] color = new float[] {1, 1, 1};
	private int length = 1;
	private boolean isDecimal = false;

	public NixieDisplay(String name,String registryName,ControlPanel panel) {
		super(name,registryName, panel);
		vars.put("value", new DataValueString(""));
		configMap.put("length", new DataValueFloat(length));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public SubElementBaseConfig getConfigSubElement(GuiControlEdit gui,Map<String,DataValue> configs) {
		return new NixieCSE(gui,configs);
	}

	@Override
	public ControlType getControlType() {
		return ControlType.DISPLAY;
	}

	@Override
	public float[] getSize() {
		return new float[] { 1.75F,2.5F,0.0625F };
	}

	@Override
	public void fillBox(float[] box) {
		float width = getSize()[0];
		float height = getSize()[1];
		box[0] = posX;
		box[1] = posY;
		box[2] = posX + width+length-1;
		box[3] = posY + height;
	}

	@Override
	protected void onConfigMapChanged() {
		for (Map.Entry<String, DataValue> e : configMap.entrySet()) {
			if (e.getKey().equals("length")) {
				length = (int) e.getValue().getNumber();
//                    posX = posX + ((digitCount-1)*getSize()[0]);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	void draw(FontRenderer font,float div,String s) {
		LeafiaGls.pushMatrix();
		LeafiaGls.scale(1/div);
		LeafiaGls.translate(-font.getStringWidth(s)/2f+0.5f,-7/2f,0);
		font.drawString(s,0,0,0xffa746);
		LeafiaGls.popMatrix();
	}

	@Override
	public void render() {
		LeafiaGls.shadeModel(GL11.GL_SMOOTH);
		TextureManager texmg = Minecraft.getMinecraft().getTextureManager();
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		boolean unicode = font.getUnicodeFlag();
		font.setUnicodeFlag(true);
		WaveFrontObjectVAO mdl = (WaveFrontObjectVAO)getModel();
		String text = getVar("value").toString();
		float lX = OpenGlHelper.lastBrightnessX;
		float lY = OpenGlHelper.lastBrightnessY;
		LeafiaGls.enableBlend();
		texmg.bindTexture(AddonBase.solid);
		LeafiaGls.blendFunc(SourceFactor.SRC_ALPHA,DestFactor.ONE_MINUS_SRC_ALPHA);
		for (int i = 0; i < length; i++) {
			LeafiaGls.pushMatrix();
			LeafiaGls.color(0.1F,0.1F,0.1F);
			LeafiaGls.translate(posX+i,0,posY);

			String s = "";
			if (i < text.length())
				s = text.substring(i,i+1);

			mdl.renderPart("Border");
			if (i == 0)
				mdl.renderPart("BorderLeft");
			if (i == length-1) {
				LeafiaGls.pushMatrix();
				LeafiaGls.rotate(180,0,1,0);
				mdl.renderPart("BorderLeft");
				LeafiaGls.popMatrix();
			}
			LeafiaGls.color(0.5F,0.5F,0.5F);
			if (i == 0)
				mdl.renderPart("Screws");
			if (i == length-1) {
				LeafiaGls.pushMatrix();
				LeafiaGls.rotate(180,0,1,0);
				mdl.renderPart("Screws");
				LeafiaGls.popMatrix();
			}
			LeafiaGls.color(0.02F,0.02F,0.02F);
			mdl.renderPart("Inside");
			if (i == 0)
				mdl.renderPart("InsideLeft");
			if (i == length-1) {
				LeafiaGls.pushMatrix();
				LeafiaGls.rotate(180,0,1,0);
				mdl.renderPart("InsideLeft");
				LeafiaGls.popMatrix();
			}
			LeafiaGls.blendFunc(SourceFactor.SRC_ALPHA,DestFactor.ONE);
			{
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
				LeafiaGls.pushMatrix();
				LeafiaGls.rotate(90,1,0,0);
				LeafiaGls.translate(0,0,0.562);

				// unicode font offset
				LeafiaGls.translate(0,-0.1,0);

				draw(font,7,s);
				LeafiaGls.translate(0,0,-0.01);
				draw(font,6.5f,s);
				LeafiaGls.translate(0,0,-0.01);
				draw(font,6.4f,s);
				LeafiaGls.popMatrix();
			}
			LeafiaGls.blendFunc(SourceFactor.SRC_ALPHA,DestFactor.ONE_MINUS_SRC_ALPHA);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lX,lY);
			texmg.bindTexture(AddonBase.solid);
			LeafiaGls.color(1F,0.462F,0.076F,0.25F);
			mdl.renderPart("Tube");
			LeafiaGls.popMatrix();
		}
		texmg.bindTexture(AddonBase.invisible);
		LeafiaGls.alphaFunc(GL11.GL_ALWAYS,0);
		for (int i = 0; i < length; i++) {
			LeafiaGls.pushMatrix();
			LeafiaGls.translate(posX+i,0,posY);
			mdl.renderPart("ClipPlane");
			LeafiaGls.popMatrix();
		}
		LeafiaGls.alphaFunc(GL11.GL_GREATER,0);
		font.setUnicodeFlag(unicode);
		LeafiaGls.disableBlend();
		LeafiaGls.shadeModel(GL11.GL_FLAT);
		LeafiaGls.color(1.0F,1.0F,1.0F,1.0F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IModelCustom getModel() {
		return AddonInstrumentModels.nixie;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ResourceLocation getGuiTexture() {
		return ResourceManager.ctrl_display_seven_seg_gui_tex;
	}

	@Override
	public AxisAlignedBB getBoundingBox() {
		return null;
	}

	@Override
	public void populateDefaultNodes(List<ControlEvent> receiveEvents) {
	}

	@Override
	public Control newControl(ControlPanel panel) {
		return new NixieDisplay(name,registryName,panel);
	}
}
