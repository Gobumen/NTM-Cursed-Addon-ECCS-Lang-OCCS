package com.leafia.contents.machines.controlpanel.nodes.pack;

import com.hbm.inventory.control_panel.Control;
import com.hbm.inventory.control_panel.NodeButton;
import com.hbm.inventory.control_panel.NodeConnection;
import com.hbm.inventory.control_panel.NodeSystem;
import com.hbm.inventory.control_panel.nodes.Node;
import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.types.DataValue.DataType;
import com.hbm.inventory.control_panel.types.DataValueComposite;
import com.hbm.inventory.control_panel.types.DataValueString;
import net.minecraft.nbt.NBTTagCompound;

public class NodeIC10 extends Node {
	public Control ctrl;
	public NodeIC10(float x,float y,Control ctrl) {
		super(x,y);
		this.ctrl = ctrl;
		otherElements.add(new NodeButton("Edit Code",this,otherElements.size()));
		outputs.add(new NodeConnection("Output",this,outputs.size(),false,DataType.COMPOSITE,new DataValueComposite()));
		outputs.add(new NodeConnection("Error",this,outputs.size(),false,DataType.STRING,new DataValueString("")));
		inputs.add(new NodeConnection("Input",this,inputs.size(),true,DataType.COMPOSITE,new DataValueComposite()));
		recalcSize();
	}
	@Override
	public DataValue evaluate(int i) {
		if (i == 0)
			return new DataValueComposite();
		else
			return new DataValueString("WIP");
	}
	@Override
	public float[] getColor() {
		return DataType.COMPOSITE.getColor();
	}
	@Override
	public String getDisplayName() {
		return "IC10 (WIP)";
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag,NodeSystem sys) {
		tag.setString("nodeType","leafia_ic10");
		return super.writeToNBT(tag,sys);
	}
}
