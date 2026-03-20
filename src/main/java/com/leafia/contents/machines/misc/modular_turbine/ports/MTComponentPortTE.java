package com.leafia.contents.machines.misc.modular_turbine.ports;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.leafia.contents.machines.misc.modular_turbine.MTPacketId;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineComponentTE;
import com.leafia.dev.container_utility.LeafiaPacket;
import com.leafia.dev.container_utility.LeafiaPacketReceiver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;

public class MTComponentPortTE extends ModularTurbineComponentTE implements ITickable, IFluidStandardSenderMK2, IFluidStandardReceiverMK2, LeafiaPacketReceiver {
	public FluidType identifier;
	public boolean decompress = false;
	boolean loaded = true;
	@Override
	public void onChunkUnload() {
		loaded = false;
		super.onChunkUnload();
	}
	@Override
	public @NotNull FluidTankNTM[] getReceivingTanks() {
		if (assembly != null)
			return new FluidTankNTM[]{ assembly.input };
		return new FluidTankNTM[0];
	}
	@Override
	public @NotNull FluidTankNTM[] getSendingTanks() {
		if (assembly != null)
			return new FluidTankNTM[]{ assembly.output };
		return new FluidTankNTM[0];
	}
	@Override
	public FluidTankNTM[] getAllTanks() {
		if (assembly != null)
			return new FluidTankNTM[]{ assembly.input,assembly.output };
		return new FluidTankNTM[0];
	}
	@Override
	public boolean isLoaded() {
		return loaded;
	}
	IMTPortBlock port = null;
	boolean syncNeeded = true;
	@Override
	public void update() {
		if (!world.isRemote) {
			if (port == null && world.getBlockState(pos).getBlock() instanceof IMTPortBlock p)
				port = p;
			if (assembly != null) {
				for (DirPos conPos : port.getConPos(pos,EnumFacing.byIndex(getBlockMetadata()-10).getOpposite())) {
					trySubscribe(assembly.typeIn,world,conPos);
					tryProvide(assembly.output,world,conPos);
				}
			}
			if (syncNeeded) {
				syncNeeded = false;
				LeafiaPacket._start(this)
						.__write(MTPacketId.PORT_IDENTIFIER.id,identifier != null ? identifier.getName() : null)
						.__write(MTPacketId.PORT_DECOMPRESSION.id,decompress)
						.__sendToAffectedClients();
			}
		}
	}
	@Override
	public String getPacketIdentifier() {
		return "MT_PORT";
	}
	@Override
	public void onReceivePacketLocal(byte key,Object value) {
		if (key == MTPacketId.PORT_IDENTIFIER.id) {
			if (value == null) identifier = null;
			else identifier = Fluids.fromName((String)value);
		} else if (key == MTPacketId.PORT_DECOMPRESSION.id)
			decompress = (boolean)value;
	}
	@Override
	public void onReceivePacketServer(byte key,Object value,EntityPlayer plr) { }
	@Override
	public void onPlayerValidate(EntityPlayer plr) {
		LeafiaPacket._start(this)
				.__write(MTPacketId.PORT_IDENTIFIER.id,identifier != null ? identifier.getName() : null)
				.__write(MTPacketId.PORT_DECOMPRESSION.id,decompress)
				.__sendToClient(plr);
	}
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey("identifier"))
			identifier = Fluids.fromName(compound.getString("identifier"));
		decompress = compound.getBoolean("decompress");
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		if (identifier != null)
			compound.setString("identifier",identifier.getName());
		compound.setBoolean("decompress",decompress);
		return super.writeToNBT(compound);
	}
}
