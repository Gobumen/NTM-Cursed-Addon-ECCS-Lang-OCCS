package com.leafia.contents.machines.controlpanel.categories;

import com.hbm.inventory.control_panel.ItemList;
import com.hbm.inventory.control_panel.SubElementNodeEditor;
import com.hbm.inventory.control_panel.modular.INodeMenuCreator;
import com.hbm.inventory.control_panel.nodes.Node;
import com.leafia.contents.machines.controlpanel.nodes.utility.NodeCache;
import com.leafia.contents.machines.controlpanel.nodes.utility.NodeClock;
import com.leafia.contents.machines.controlpanel.nodes.utility.NodeParseFloat;
import com.leafia.contents.machines.controlpanel.nodes.utility.NodeSummarizer;

public class NCLeafiaUtility implements INodeMenuCreator {
	@Override
	public Node selectItem(String s,float x,float y,SubElementNodeEditor editor) {
		return switch(s) {
			case "Cache" -> new NodeCache(x,y);
			case "Clock" -> new NodeClock(x,y);
			case "Summarizer" -> new NodeSummarizer(x,y);
			case "Parse Number" -> new NodeParseFloat(x,y);
			default -> null;
		};
	}
	@Override
	public void addItems(ItemList itemList,float x,float y,SubElementNodeEditor editor) {
		itemList.addItems("Cache");
		itemList.addItems("Clock");
		itemList.addItems("Parse Number");
		itemList.addItems("Summarizer");
	}
}
