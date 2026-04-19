package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.inventory.control_panel.GuiControlEdit;
import com.hbm.inventory.control_panel.SubElement;
import com.leafia.contents.machines.controlpanel.ic10.SubElementIC10Editor;
import com.leafia.overwrite_contents.interfaces.IMixinGuiControlEdit;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(value = GuiControlEdit.class)
public abstract class MixinGuiControlEdit extends GuiContainer implements IMixinGuiControlEdit {
	@Shadow(remap = false)
	protected abstract void pushElement(SubElement e);

	public MixinGuiControlEdit(Container inventorySlotsIn) {
		super(inventorySlotsIn);
	}
	@Unique SubElementIC10Editor leafia$ic10Editor;
	@Inject(method = "initGui",at = @At(value = "TAIL"),require = 1)
	void onInitGui(CallbackInfo ci) {
		leafia$ic10Editor = new SubElementIC10Editor((GuiControlEdit)(IMixinGuiControlEdit)this);
		leafia$ic10Editor.initGui();
		pushElement(leafia$ic10Editor); // debug
	}
	@Override
	public SubElementIC10Editor leafia$ic10Editor() {
		return leafia$ic10Editor;
	}
	@Override
	public void handleMouseInput() throws IOException {
		if (leafia$ic10Editor != null)
			leafia$ic10Editor.handleMouseInput();
		super.handleMouseInput();
	}
}
