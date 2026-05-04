package com.leafia.contents.machines.reactors.rbmk.columns.control_auto.container;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerRBMKControlAuto;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlAuto;
import com.hbm.util.BobMathUtil;
import com.leafia.Tags;
import com.leafia.dev.gui.FiaUIRect;
import com.leafia.dev.gui.LCEGuiInfoContainer;
import com.leafia.overwrite_contents.interfaces.IMixinTileEntityRBMKControlAuto;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.math.NumberUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

import static com.hbm.util.SoundUtil.playClickSound;

public class RBMKControlAutoGUI extends LCEGuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(Tags.MODID,"textures/gui/reactors/rbmk_control_auto_az5able.png");
	private TileEntityRBMKControlAuto rod;

	private GuiTextField[] fields;

	public RBMKControlAutoGUI(InventoryPlayer invPlayer,TileEntityRBMKControlAuto tedf) {
		super(new ContainerRBMKControlAuto(invPlayer, tedf));
		rod = tedf;
		
		fields = new GuiTextField[4];
		
		this.xSize = 176;
		this.ySize = 186;
	}

	static final int offsA = 13;
	static final int offsB = 16;
	FiaUIRect rectBulb;
	FiaUIRect rectAll;
	FiaUIRect rectOne;
	
	public void initGui() {
		super.initGui();
		rectBulb = new FiaUIRect(this,151,34,10,11);
		rectAll = new FiaUIRect(this,151,54,10,10);
		rectOne = new FiaUIRect(this,151,70,10,10);
		Keyboard.enableRepeatEvents(true);
		
		for(int i = 0; i < 4; i++) {
			this.fields[i] = new GuiTextField(0, this.fontRenderer, guiLeft + 30 - offsA, guiTop + 27 + 11 * i, 26, 6);
			this.fields[i].setTextColor(-1);
			this.fields[i].setDisabledTextColour(-1);
			this.fields[i].setEnableBackgroundDrawing(false);
			
			if(i < 2)
				this.fields[i].setMaxStringLength(3);
			else
				this.fields[i].setMaxStringLength(4);
		}

		this.fields[0].setText(String.valueOf((int)rod.levelUpper));
		this.fields[1].setText(String.valueOf((int)rod.levelLower));
		this.fields[2].setText(String.valueOf((int)rod.heatUpper));
		this.fields[3].setText(String.valueOf((int)rod.heatLower));
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 124 - offsB, guiTop + 29, 16, 56, mouseX, mouseY, new String[]{ (int)(rod.level * 100) + "%" } );
		
		String func = "Function: ";
		
		switch(rod.function) {
		case LINEAR: func += "Linear"; break;
		case QUAD_UP: func += "Quadratic"; break;
		case QUAD_DOWN: func += "Inverse Quadratic"; break;
		}

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 58 - offsA, guiTop + 26, 28, 19, mouseX, mouseY, new String[]{ func } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 61 - offsA, guiTop + 48, 22, 10, mouseX, mouseY, new String[]{ "Select linear interpolation" } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 61 - offsA, guiTop + 59, 22, 10, mouseX, mouseY, new String[]{ "Select quadratic interpolation" } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 61 - offsA, guiTop + 70, 22, 10, mouseX, mouseY, new String[]{ "Select inverse quadratic interpolation" } );

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 28 - offsA, guiTop + 26, 30, 10, mouseX, mouseY, new String[]{ "Level at max heat", "Should be smaller than level at min heat" } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 28 - offsA, guiTop + 37, 30, 10, mouseX, mouseY, new String[]{ "Level at min heat", "Should be larger than level at min heat" } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 28 - offsA, guiTop + 48, 30, 10, mouseX, mouseY, new String[]{ "Max heat", "Must be larger than min heat" } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 28 - offsA, guiTop + 59, 30, 10, mouseX, mouseY, new String[]{ "Min heat", "Must be smaller than max heat" } );
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 28 - offsA, guiTop + 70, 30, 10, mouseX, mouseY, new String[]{ "Save parameters" } );

		IMixinTileEntityRBMKControlAuto mixin = (IMixinTileEntityRBMKControlAuto)rod;
		if (rectBulb.isMouseIn(mouseX,mouseY))
			drawHoveringText(mixin.leafia$getScrammed() ? TextFormatting.GOLD+"Rod is shut off" : "Rod is active",mouseX,mouseY);

		if (rectAll.isMouseIn(mouseX,mouseY))
			drawHoveringText("Acknowledge All",mouseX,mouseY);

		if (rectOne.isMouseIn(mouseX,mouseY))
			drawHoveringText("Acknowledge",mouseX,mouseY);

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int i) throws IOException {
		super.mouseClicked(x, y, i);
		
		for(int j = 0; j < 4; j++) {
			this.fields[j].mouseClicked(x, y, i);
		}
		if (i == 0) {
			if (rectOne.isMouseIn(x,y)) {
				IMixinTileEntityRBMKControlAuto mixin = (IMixinTileEntityRBMKControlAuto)rod;
				if (mixin.leafia$getScrammed()) {
					playClick(1);
					NBTTagCompound data = new NBTTagCompound();
					data.setBoolean("acknowledge",false);
					PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data,rod.getPos()));
				} else
					playDenied();
			}

			if (rectAll.isMouseIn(x,y)) {
				playClick(1);
				NBTTagCompound data = new NBTTagCompound();
				data.setBoolean("acknowledge",true);
				PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data,rod.getPos()));
			}

			if(guiLeft + 28 - offsA <= x && guiLeft + 28 + 30 - offsA > x && guiTop + 70 < y && guiTop + 70 +10 >= y) {

				playClickSound();
				NBTTagCompound data = new NBTTagCompound();

				double[] vals = new double[] {0D ,0D, 0D, 0D};

				for(int k = 0; k < 4; k++) {

					double clamp = k < 2 ? 100 : 9999;

					if(NumberUtils.isCreatable(fields[k].getText())) {
						int j = (int)MathHelper.clamp(Double.parseDouble(fields[k].getText()), 0, clamp);
						fields[k].setText(j + "");
						vals[k] = j;
					} else {
						fields[k].setText("0");
					}
				}

				data.setDouble("levelUpper", vals[0]);
				data.setDouble("levelLower", vals[1]);
				data.setDouble("heatUpper", vals[2]);
				data.setDouble("heatLower", vals[3]);

				PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, rod.getPos()));
			}

			for(int k = 0; k < 3; k++) {

				//manual rod control
				//leafia: fucking lies that's function control
				if(guiLeft + 61 - offsA <= x && guiLeft + 61 + 22 - offsA > x && guiTop + 48 + k * 11 < y && guiTop + 48 + 10 + k * 11 >= y) {

					playClickSound();
					NBTTagCompound data = new NBTTagCompound();
					data.setInteger("function", k);
					PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, rod.getPos()));
				}
			}
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = I18n.format(this.rod.getName());
		
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if (((IMixinTileEntityRBMKControlAuto)rod).leafia$getScrammed() && System.currentTimeMillis() % 500L < 250L)
			drawTexturedModalRect(guiLeft+144,guiTop+26,0,186,24,25);
		
		int height = (int)(56 * (1D - rod.level));
		
		if(height > 0)
			drawTexturedModalRect(guiLeft + 124-offsB, guiTop + 29, 176, 56 - height, 8, height);
		
		int f = rod.function.ordinal();
		drawTexturedModalRect(guiLeft + 59 - offsA, guiTop + 27, 184, f * 19, 26, 19);

		if(rod.isPowered()) {
			drawTexturedModalRect(guiLeft + 136, guiTop + 21, 210, rod.hasPower ? 16 : 0, 16, 16);
		}

		for(int i = 0; i < 4; i++) {
			this.fields[i].drawTextBox();
		}
	}

	@Override
	protected void keyTyped(char c, int i) throws IOException {
		
		for(int j = 0; j < 4; j++) {
			if(this.fields[j].textboxKeyTyped(c, i))
				return;
		}
		
		super.keyTyped(c, i);
	}
}