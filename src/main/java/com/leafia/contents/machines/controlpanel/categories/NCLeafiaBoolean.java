package com.leafia.contents.machines.controlpanel.categories;

import com.hbm.inventory.control_panel.ItemList;
import com.hbm.inventory.control_panel.SubElementNodeEditor;
import com.hbm.inventory.control_panel.modular.INodeMenuCreator;
import com.hbm.inventory.control_panel.nodes.Node;
import com.leafia.contents.machines.controlpanel.nodes.bool.NodePulse;

public class NCLeafiaBoolean implements INodeMenuCreator {
	@Override
	public Node selectItem(String s,float x,float y,SubElementNodeEditor editor) {
		return switch(s) {
			case "Pulse" -> new NodePulse(x,y);
			default -> null;
		};
	}
	@Override
	public void addItems(ItemList itemList,float x,float y,SubElementNodeEditor editor) {
		itemList.addItems("Pulse");
	}
}
