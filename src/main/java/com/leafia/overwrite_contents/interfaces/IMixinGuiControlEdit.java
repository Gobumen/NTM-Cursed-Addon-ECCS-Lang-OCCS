package com.leafia.overwrite_contents.interfaces;

import com.hbm.inventory.control_panel.SubElement;
import com.leafia.contents.machines.controlpanel.ic10.SubElementIC10Editor;

public interface IMixinGuiControlEdit {
	SubElementIC10Editor leafia$ic10Editor();
	void leafia$popElement(); // bruh protected access
	void leafia$pushElement(SubElement e); // bruh protected access
}
