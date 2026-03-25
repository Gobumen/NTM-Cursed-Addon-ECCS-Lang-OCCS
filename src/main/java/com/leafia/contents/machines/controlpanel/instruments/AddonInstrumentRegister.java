package com.leafia.contents.machines.controlpanel.instruments;

import com.hbm.inventory.control_panel.Control;
import com.hbm.inventory.control_panel.ControlRegistry;
import com.leafia.contents.machines.controlpanel.instruments.types.nixie.NixieDisplay;
import com.leafia.contents.machines.controlpanel.instruments.types.starbound.LargeButton;
import com.leafia.contents.machines.controlpanel.instruments.types.starbound.LargeSwitch;

import java.util.List;

public class AddonInstrumentRegister {
	static final List<Control> reg = ControlRegistry.addonControls;
	public static void register() {
		reg.add(new NixieDisplay("Nixie Tube","leafia_nixie",null));
		reg.add(new LargeSwitch("Large Switch","leafia_sbswitch",null));
		reg.add(new LargeButton("Large Button","leafia_sbbutton",null));
	}
}
