package com.leafia.contents.machines.controlpanel.nodes.utility;

import com.hbm.inventory.control_panel.DataValue;
import com.hbm.inventory.control_panel.DataValue.DataType;
import com.hbm.inventory.control_panel.DataValueFloat;
import com.hbm.inventory.control_panel.NodeConnection;
import com.hbm.inventory.control_panel.NodeDropdown;
import com.hbm.inventory.control_panel.nodes.Node;
import com.leafia.contents.machines.controlpanel.AddonNodesRegister;
import com.leafia.contents.machines.controlpanel.nodes.NodeBulkQuery.CombineMethod;
import net.minecraft.util.math.MathHelper;

public class NodeSummarizer extends Node {
	public CombineMethod combineMethod = CombineMethod.AVERAGE;
	public float[] internalList = new float[40];
	public int needle = 0;
	public NodeSummarizer(float x,float y) {
		super(x,y);
		NodeDropdown combineSelector = new NodeDropdown(this, otherElements.size(),s -> {
			try {
				combineMethod = CombineMethod.valueOf(s);
			} catch (IllegalArgumentException ignored) {}
			return null;
		}, () -> combineMethod.name());
		this.otherElements.add(combineSelector);
		for (CombineMethod value : CombineMethod.values())
			combineSelector.list.addItems(value.name());
		this.outputs.add(new NodeConnection("Summary",this,outputs.size(),false,DataType.NUMBER,new DataValueFloat(0)));
		this.inputs.add(new NodeConnection("Enable",this,inputs.size(),true,DataType.NUMBER,new DataValueFloat(0)));
		this.inputs.add(new NodeConnection("New Value",this,inputs.size(),true,DataType.NUMBER,new DataValueFloat(0)));
		this.inputs.add(new NodeConnection("Clear",this,inputs.size(),true,DataType.NUMBER,new DataValueFloat(0)));
		this.inputs.add(new NodeConnection("Trim Count",this,inputs.size(),true,DataType.NUMBER,new DataValueFloat(40)));
		recalcSize();
	}
	@Override
	public DataValue evaluate(int i) {
		if (inputs.get(0) == null) return null;
		if (inputs.get(1) == null) return null;
		if (inputs.get(2) == null) return null;
		if (inputs.get(3) == null) return null;
		int length = (int)MathHelper.clamp(inputs.get(3).evaluate().getNumber(),1,500);
		if (length != internalList.length)
			internalList = new float[length];
		if (inputs.get(0).evaluate().getBoolean()) {
			//internalList[needle] = fuck
		}
		float value = 0;
		int total = 0;
		for (float v : internalList) {
			switch(combineMethod) {
				case ADD -> value += v;
				case MULTIPLY -> value *= v;
				case AVERAGE -> {
					value += v;
					total++;
				}
			}
		}
		if (total > 0 && combineMethod == CombineMethod.AVERAGE)
			value /= total;
		return new DataValueFloat(value);
	}
	@Override
	public float[] getColor() {
		return AddonNodesRegister.colorUtility;
	}
	@Override
	public String getDisplayName() {
		return "Summarizer";
	}
}
