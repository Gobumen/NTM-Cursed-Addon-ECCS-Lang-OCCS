package com.leafia.contents.machines.controlpanel;

import com.hbm.inventory.control_panel.NodeSystem;
import com.hbm.inventory.control_panel.modular.INodeLoader;
import com.hbm.inventory.control_panel.nodes.Node;
import com.leafia.contents.machines.controlpanel.nodes.*;
import com.leafia.contents.machines.controlpanel.nodes.NodeBulkQuery;
import com.leafia.contents.machines.controlpanel.nodes.bool.NodePulse;
import com.leafia.contents.machines.controlpanel.nodes.string.NodeAddString;
import com.leafia.contents.machines.controlpanel.nodes.string.NodeFormat;
import com.leafia.contents.machines.controlpanel.nodes.string.NodeSIPfx;
import com.leafia.contents.machines.controlpanel.nodes.string.NodeSubString;
import com.leafia.contents.machines.controlpanel.nodes.utility.NodeClock;
import net.minecraft.nbt.NBTTagCompound;

public class AddonNodeLoader implements INodeLoader {
	@Override
	public Node nodeFromNBT(NBTTagCompound nbtTagCompound,NodeSystem nodeSystem) {
		Node node = switch(nbtTagCompound.getString("nodeType")) {
			case "sounder" -> new NodeSounder(0,0);
			case "addString" -> new NodeAddString(0,0);
			case "subString" -> new NodeSubString(0,0);
			case "sipfx" -> new NodeSIPfx(0,0);
			case "format" -> new NodeFormat(0,0);
			case "bulkQuery" -> new NodeBulkQuery(0,0,nodeSystem.parent);
			case "leafia_clock" -> new NodeClock(0,0);
			case "leafia_pulse" -> new NodePulse(0,0);
			default -> null;
		};
		return node;
	}
}
