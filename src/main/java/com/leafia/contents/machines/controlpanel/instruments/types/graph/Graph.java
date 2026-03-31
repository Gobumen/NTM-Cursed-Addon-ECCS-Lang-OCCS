package com.leafia.contents.machines.controlpanel.instruments.types.graph;

import com.hbm.inventory.control_panel.Control;
import com.hbm.inventory.control_panel.ControlEvent;
import com.hbm.inventory.control_panel.ControlPanel;
import com.hbm.inventory.control_panel.controls.ControlType;
import com.hbm.render.loader.IModelCustom;
import com.leafia.AddonBase;
import com.leafia.contents.machines.controlpanel.instruments.AddonControlType;
import com.leafia.dev.LeafiaBrush;
import com.leafia.transformer.LeafiaGls;
import com.llib.math.LeafiaColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Graph extends Control {
	float width = 6;
	float height = 4;
	int verticalLineSpan = 1;
	int horizontalLineDivision = 10;
	GraphSkin skin = GraphSkin.LIGHT;
	// we ain't doing enums because fuck those
	public static class GraphSkin {
		public final String name;
		public final boolean emissive;
		public final boolean hasGrid;
		public final int bgColor;
		public final int gridColor;
		GraphSkin(String name,boolean emissive,boolean hasGrid,int bgColor,int gridColor) {
			this.name = name;
			this.emissive = emissive;
			this.hasGrid = hasGrid;
			this.bgColor = bgColor;
			this.gridColor = gridColor;
		}
		static List<GraphSkin> skins = new ArrayList<>();
		public static GraphSkin registerOrGet(String name,boolean emissive,boolean hasGrid,int bgColor,int gridColor) {
			for (GraphSkin skin : skins) {
				if (skin.name.equals(name))
					return skin;
			}
			return new GraphSkin(name,emissive,hasGrid,bgColor,gridColor);
		}
		public static final GraphSkin CONSOLE = registerOrGet("Console",true,false,0,0);
		public static final GraphSkin LIGHT = registerOrGet("Light",false,true,0xFFFFFF,0xCCCCCC);
		public static final GraphSkin DARK = registerOrGet("Dark",true,true,0x1A1A1A,0x3A3A3A);
	}
	public Graph(String name,String registryName,ControlPanel panel) {
		super(name,registryName,panel);
	}
	@Override
	public ControlType getControlType() {
		return AddonControlType.GRAPH;
	}
	@Override
	public float[] getSize() {
		return new float[]{ width,height,0.1f };
	}
	@Override
	public AxisAlignedBB getBoundingBox() {
		return null;
	}
	public double[] values = new double[]{ 0.5,0.3,0.8,0.3,0.8,0.5 };
	@Override
	public void render() {
		LeafiaGls.shadeModel(GL11.GL_SMOOTH);
		TextureManager texmg = Minecraft.getMinecraft().getTextureManager();
		LeafiaGls.pushMatrix();
		LeafiaGls.translate(posX-width/2,0.001,posY-height/2);
		LeafiaBrush brush = LeafiaBrush.instance;
		texmg.bindTexture(AddonBase.solid);
		float lX = OpenGlHelper.lastBrightnessX;
		float lY = OpenGlHelper.lastBrightnessY;
		{
			LeafiaColor bgColor = new LeafiaColor(skin.bgColor);
			LeafiaGls.color(bgColor.getRed(),bgColor.getGreen(),bgColor.getBlue());
			brush.startDrawingQuads();
			brush.addVertexWithUV(0,0,height,0,1);
			brush.addVertexWithUV(width,0,height,1,1);
			brush.addVertexWithUV(width,0,0,1,0);
			brush.addVertexWithUV(0,0,0,0,0);
			brush.draw();
		}
		{
			LeafiaGls.translate(0,0.001,0);
			LeafiaColor gridColor = new LeafiaColor(skin.gridColor);
			LeafiaGls.color(gridColor.getRed(),gridColor.getGreen(),gridColor.getBlue());
			double thickness = 0.02;
			brush.startDrawingQuads();
			for (int i = 0; i < values.length; i+=verticalLineSpan) {
				double x = i/(values.length-1d);
				brush.addVertexWithUV(x*width-thickness,0,height,0,1);
				brush.addVertexWithUV(x*width+thickness,0,height,1,1);
				brush.addVertexWithUV(x*width+thickness,0,0,1,0);
				brush.addVertexWithUV(x*width-thickness,0,0,0,0);
			}
			if (horizontalLineDivision > 0) {
				for (int i = 0; i <= horizontalLineDivision; i++) {
					double y = i/(double)horizontalLineDivision;
					brush.addVertexWithUV(0,0,y*height+thickness,0,1);
					brush.addVertexWithUV(width,0,y*height+thickness,1,1);
					brush.addVertexWithUV(width,0,y*height-thickness,1,0);
					brush.addVertexWithUV(0,0,y*height-thickness,0,0);
				}
			}
			brush.draw();
		}
		{
			LeafiaGls.translate(0,0.001,0);
			LeafiaGls.color(0,1,0);
			if (skin.emissive)
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
			double thickness = 0.05;
			brush.startDrawingQuads();
			double x0 = -1;
			double y0 = -1;
			for (int i = 0; i < values.length; i++) {
				double x = i/(values.length-1d);
				double y = values[i];
				double x1 = x*width;
				double y1 = height*(1-y);
				if (x0 >= 0) {
					brush.addVertexWithUV(x0,0,y0+thickness,0,1);
					brush.addVertexWithUV(x1,0,y1+thickness,1,1);
					brush.addVertexWithUV(x1,0,y1-thickness,1,0);
					brush.addVertexWithUV(x0,0,y0-thickness,0,0);
				}
				x0 = x1;
				y0 = y1;
			}
			brush.draw();
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lX,lY);
		}
		LeafiaGls.color(1,1,1);
		LeafiaGls.popMatrix();
	}
	ResourceLocation dummy = new ResourceLocation("nope");
	@SideOnly(Side.CLIENT)
	@Override
	public IModelCustom getModel() {
		return null;
	}
	@SideOnly(Side.CLIENT)
	@Override
	public ResourceLocation getGuiTexture() {
		return dummy;
	}
	@Override
	public Control newControl(ControlPanel controlPanel) {
		return new Graph(name,registryName,controlPanel);
	}
	@Override
	public void populateDefaultNodes(List<ControlEvent> list) { }
}
