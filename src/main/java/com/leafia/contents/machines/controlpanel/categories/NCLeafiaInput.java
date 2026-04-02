package com.leafia.contents.machines.controlpanel.categories;

import com.hbm.inventory.control_panel.ItemList;
import com.hbm.inventory.control_panel.SubElementNodeEditor;
import com.hbm.inventory.control_panel.modular.INodeMenuCreator;
import com.hbm.inventory.control_panel.nodes.Node;
import com.leafia.contents.machines.controlpanel.instruments.types.graph.Graph;
import com.leafia.contents.machines.controlpanel.nodes.NodeBulkQuery;
import com.leafia.contents.machines.controlpanel.nodes.NodeGraphBounds;

public class NCLeafiaInput implements INodeMenuCreator {
	@Override
	public Node selectItem(String s,float x,float y,SubElementNodeEditor editor) {
		return switch(s) {
			case "Bulk Query Block" -> new NodeBulkQuery(x,y,editor.currentSystem.parent);
			case "Graph Bounds" -> new NodeGraphBounds(x,y,editor.currentSystem.parent);
			default -> null;
		};
	}
	@Override
	public void addItems(ItemList list,float x,float y,SubElementNodeEditor editor) {
		list.addItems("Bulk Query Block");
		if (editor.currentSystem.parent instanceof Graph)
			list.addItems("Graph Bounds");
	}
}
