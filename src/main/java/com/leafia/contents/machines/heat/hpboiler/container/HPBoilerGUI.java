package com.leafia.contents.machines.heat.hpboiler.container;

import com.leafia.contents.machines.heat.hpboiler.HPBoilerTE;
import com.leafia.contents.miscellanous.slop.SlopTE;
import com.leafia.dev.LeafiaClientUtil;
import com.leafia.dev.container_utility.LeafiaPacket;
import com.leafia.dev.gui.LCEGuiInfoContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

import static com.leafia.init.ResourceInit.getIntegrated;

public class HPBoilerGUI extends LCEGuiInfoContainer {

	private static ResourceLocation texture = getIntegrated("ngf_hpboiler/gui.png");

	private HPBoilerTE entity;
	public HPBoilerGUI(InventoryPlayer invPlayer,HPBoilerTE entity) {
		super(new HPBoilerContainer(invPlayer,entity));
		this.entity = entity;
		this.xSize = 176;
		this.ySize = 185;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX,mouseY,f);
		LeafiaClientUtil.renderTankInfo(entity.input,this,mouseX,mouseY,guiLeft+44,guiTop+17,16,52);
		LeafiaClientUtil.renderTankInfo(entity.output,this,mouseX,mouseY,guiLeft+116,guiTop+17,16,52);
		super.renderHoveredToolTip(mouseX,mouseY);
	}

	protected void mouseClicked(int x,int y,int i) throws IOException {
		super.mouseClicked(x,y,i);
    }
	
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.entity.hasCustomName() ? this.entity.getName() : I18n.format(this.entity.getDefaultName());

		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawDefaultBackground();
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
		entity.input.renderTank(guiLeft+44,guiTop+17+52,zLevel,16,52);
		entity.output.renderTank(guiLeft+116,guiTop+17+52,zLevel,16,52);
	}
}
