package com.leafia.contents.machines.elevators;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.leafia.contents.machines.elevators.car.ElevatorEntity;
import com.leafia.contents.machines.elevators.weight.EvWeightEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

public class EvPulleyTE extends TileEntity implements IEnergyReceiverMK2, ITickable {
	public ElevatorEntity elevator;
	public EvWeightEntity counterweight;
	public double setupDistCabin = -1;
	public double setupDistWeight = -1;

	@Override
	public void update() {
		if (!world.isRemote) {
			if (elevator != null) {
				if (!elevator.isEntityAlive()) {
					setupDistCabin = -1;
					elevator = null;
				}
			}
			if (counterweight != null) {
				if (!counterweight.isEntityAlive()) {
					setupDistWeight = -1;
					counterweight = null;
				}
			}
			if (counterweight != null && elevator != null) {
				double distEv = pos.getY()-elevator.posY;
				double offset = distEv-setupDistCabin;
				counterweight.posY = pos.getY()-setupDistWeight+offset;
			}
		}
	}

	@Override
	public void setPower(long power) {

	}

	@Override
	public long getPower() {
		return 0;
	}

	@Override
	public long getMaxPower() {
		return 0;
	}

	boolean loaded = true;

	@Override
	public boolean isLoaded() {
		return loaded;
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		loaded = false;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if (compound.hasKey("setupDistCabin"))
			setupDistCabin = compound.getDouble("setupDistCabin");
		if (compound.hasKey("setupDistWeight"))
			setupDistCabin = compound.getDouble("setupDistWeight");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setDouble("setupDistCabin",setupDistCabin);
		compound.setDouble("setupDistWeight",setupDistWeight);
		return super.writeToNBT(compound);
	}
}
