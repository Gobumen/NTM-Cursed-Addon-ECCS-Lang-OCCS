package com.leafia.contents.machines.controlpanel.instruments.types.lever;

import com.hbm.inventory.control_panel.*;
import com.hbm.inventory.control_panel.controls.ControlType;
import com.hbm.inventory.control_panel.nodes.*;
import com.hbm.inventory.control_panel.nodes.NodeMath.Operation;
import com.hbm.render.loader.IModelCustom;
import com.leafia.contents.machines.controlpanel.CCPSyncPacketBase;
import com.leafia.contents.machines.controlpanel.instruments.AddonControlType;
import com.leafia.contents.machines.controlpanel.instruments.types.AddonInstrumentModels;
import com.leafia.dev.custompacket.LeafiaCustomPacket;
import com.leafia.dev.optimization.bitbyte.LeafiaBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class LeverBase extends Control {
	float curLevel = 0.5f;
	public LeverBase(String name,String registryName,ControlPanel panel) {
		super(name,registryName,panel);
		vars.put("position",new DataValueFloat(50f));
	}
	@Override
	public ControlType getControlType() {
		return AddonControlType.LEVER;
	}
	@Override
	public List<String> getOutEvents() {
		return Collections.singletonList("ctrl_press");
	}
	@Override
	@SideOnly(Side.CLIENT)
	public IModelCustom getModel() {
		return AddonInstrumentModels.lever;
	}
	static final float increment = 200/7f;
	@Override
	public void receiveEvent(ControlEvent evt) {
		if (evt.name.equals("tick")) {
			vars.put("position",new DataValueFloat(Math.max(Math.min(vars.get("position").getNumber(),100),0)));
			float tgtLevel = vars.get("position").getNumber();
			float delta = tgtLevel-curLevel;
			float difference = Math.abs(delta);
			if (difference > 0) {
				if (difference <= increment)
					curLevel = tgtLevel;
				else {
					float direction = Math.signum(delta);
					curLevel += direction*increment;
				}
			}
			BlockPos pos = panel.parent.getControlPos();
			CCPLeverSyncPacket packet = new CCPLeverSyncPacket(
					pos,
					panel.controls.indexOf(this),
					curLevel
			);
			LeafiaCustomPacket.__start(packet).__sendToAllAround(
					panel.parent.getControlWorld().provider.getDimension(),
					pos,256
			);
		}
		super.receiveEvent(evt);
	}
	public static class CCPLeverSyncPacket extends CCPSyncPacketBase {
		float level = 0;
		public CCPLeverSyncPacket() { }
		public CCPLeverSyncPacket(BlockPos pos,int idx,float level) {
			super(pos,idx);
			this.level = level;
		}
		@Override
		public void encode(LeafiaBuf buf) {
			super.encode(buf);
			buf.writeFloat(level);
		}
		@Override
		public @Nullable Consumer<MessageContext> decode(LeafiaBuf buf) {
			boolean success = load(buf);
			float level = buf.readFloat();
			return (ctx)->{
				if (!success) return;
				if (instrument instanceof LeverBase sw)
					sw.curLevel = level;
			};
		}
	}
	@Override
	public void populateDefaultNodes(List<ControlEvent> receiveEvents) {
		NodeSystem ctrl_press = new NodeSystem(this);
		{
			Map<String, DataValue> vars = new HashMap<>(receiveEvents.get(0).vars);
			vars.put("from index", new DataValueFloat(0));
			NodeInput node0 = new NodeInput(170, 100, "Event Data").setVars(vars);
			ctrl_press.addNode(node0);
			NodeGetVar node1 = new NodeGetVar(170, 150, this).setData("position", false);
			ctrl_press.addNode(node1);
			NodeConditional node2 = new NodeConditional(230, 110);
			node2.inputs.get(0).setData(node0, 1, true);
			node2.inputs.get(1).setDefault(new DataValueFloat(-10f));
			node2.inputs.get(2).setDefault(new DataValueFloat(10f));
			ctrl_press.addNode(node2);
			NodeMath node3 = new NodeMath(290, 130).setData(NodeMath.Operation.ADD);
			node3.inputs.get(0).setData(node2, 0, true);
			node3.inputs.get(1).setData(node1, 0, true);
			ctrl_press.addNode(node3);
			NodeSetVar node5 = new NodeSetVar(350, 130, this).setData("position", false);
			node5.inputs.get(0).setData(node3, 0, true);
			ctrl_press.addNode(node5);
		}
		receiveNodeMap.put("ctrl_press", ctrl_press);
	}
}
