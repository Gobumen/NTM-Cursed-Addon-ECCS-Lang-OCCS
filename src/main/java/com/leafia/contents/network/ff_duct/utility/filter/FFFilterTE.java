package com.leafia.contents.network.ff_duct.utility.filter;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.I18nUtil;
import com.leafia.contents.AddonFluids;
import com.leafia.contents.machines.reactors.lftr.components.MSRTEBase;
import com.leafia.contents.machines.reactors.lftr.components.element.MSRElementTE.MSRFuel;
import com.leafia.contents.network.ff_duct.FFDuctTE;
import com.leafia.contents.network.ff_duct.uninos.IFFProvider;
import com.leafia.contents.network.ff_duct.uninos.IFFReceiver;
import com.leafia.contents.network.ff_duct.utility.FFDuctUtilityBase;
import com.leafia.contents.network.ff_duct.utility.FFDuctUtilityTEBase;
import com.leafia.dev.container_utility.LeafiaPacket;
import com.llib.exceptions.LeafiaDevFlaw;
import com.llib.group.LeafiaMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class FFFilterTE extends FFDuctUtilityTEBase implements ITickable, IFluidHandler, IFFProvider {
	FluidTank tank = new FluidTank(10000);
	MSRFuel filter = MSRFuel.u235;
	@Override
	public void setType(FluidType type) {
		super.setType(type);
		tank.drain(tank.getCapacity()*2,true);
	}

	@Override
	public boolean hasCapability(Capability<?> capability,@Nullable EnumFacing facing) {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof FFFilterBlock && capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			EnumFacing face = state.getValue(FFDuctUtilityBase.FACING);
			return facing == null || facing.getAxis().equals(face.getAxis());
		}
		return super.hasCapability(capability,facing);
	}

	@Override
	public @Nullable <T> T getCapability(Capability<T> capability,@Nullable EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		return super.getCapability(capability,facing);
	}

	void doUpdate() {
		if (getType().getFF() == null) return;
		if (!world.isRemote) {
			IBlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof FFFilterBlock) {
				EnumFacing facing = state.getValue(FFDuctUtilityBase.FACING);
				TileEntity behind = world.getTileEntity(pos.offset(facing,-1));
				if (behind instanceof IFFProvider prov) {

					FluidStack stack = prov.drain(tank.getCapacity()-tank.getFluidAmount(),false);
					if (stack != null && AddonFluids.fromFF(stack.getFluid()) == getType() && prov.getSendingTank(stack) != null && tank.getFluidAmount() == 0) {
						if (stack.tag != null) {
							NBTTagCompound tag = MSRTEBase.nbtProtocol(stack.tag);
							Map<String,Double> mixture = MSRTEBase.readMixture(tag);
							double mix = mixture.getOrDefault(filter.name(),0d);
							double mixTotal = 0;
							for (Double value : mixture.values())
								mixTotal += value;
							if (mixTotal > 0) {
								int amt = (int)(stack.amount*mix/mixTotal);
								if (amt > 0) {
									double transferAmt = mix*(amt/(stack.amount*mix/mixTotal));
									if (transferAmt > mix)
										throw new LeafiaDevFlaw("transferAmt higher than mix. Leafia is a dumbass.");

									if (transferAmt > 0) {
										Map<String,Double> mixture2 = new LeafiaMap<>();
										mixture2.put(filter.name(),transferAmt);
										mixture.put(filter.name(),mix-transferAmt);

										FluidStack fillStack = new FluidStack(stack.getFluid(),amt,MSRTEBase.writeMixture(mixture2,new NBTTagCompound()));

										// this is so jankshit
										stack = prov.getSendingTank(stack).getFluid();
										if (stack == null)
											throw new LeafiaDevFlaw("Got null FluidStack after processing. How?!");
										stack.amount -= fillStack.amount;
										stack.tag = MSRTEBase.writeMixture(mixture,new NBTTagCompound());
										prov.getSendingTank(stack).setFluid(stack);

										tank.fill(fillStack,true);
									}
								}
							}
						}
					}
					tryProvide(tank,world,pos.offset(facing),ForgeDirection.getOrientation(facing));
				}
			}
		}
	}

	@Override
	public void update() {
		doUpdate();
		LeafiaPacket packet = LeafiaPacket._start(this).__write(0,tank.getFluidAmount());
		if (tank.getFluid() != null && tank.getFluid().tag != null)
			packet.__write(1,tank.getFluid().tag);
		packet.__write(2,filter.ordinal());
		packet.__sendToAffectedClients();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInfo(List<String> info) {
		info.add(TextFormatting.GRAY+I18nUtil.resolveKey("tile.ff_pump.buffer",tank.getFluidAmount()+"mB"));
		info.add(TextFormatting.GRAY+I18nUtil.resolveKey("tile.ff_filter.filter",I18nUtil.resolveKey("tile.msr.fuel."+filter.name())));
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		tank.readFromNBT(compound.getCompoundTag("buffer"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("buffer",tank.writeToNBT(new NBTTagCompound()));
		compound.setString("filter",filter.name());
		return super.writeToNBT(compound);
	}

	@Override
	public String getPacketIdentifier() {
		return "FF_FILTER";
	}
	@Override
	public void onReceivePacketLocal(byte key,Object value) {
		super.onReceivePacketLocal(key,value);
		if (key == 0) {
			if (getType().getFF() == null)
				tank.setFluid(null);
			else
				tank.setFluid(new FluidStack(getType().getFF(),(int) value));
		} else if (key == 1 && tank.getFluid() != null)
			tank.getFluid().tag = (NBTTagCompound)value;
		else if (key == 2)
			filter = MSRFuel.values()[(int)value];
	}
	@Override
	public FluidTank getSendingTank(FluidStack stack) {
		return tank;
	}
	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[0];
	}
	@Override
	public int fill(FluidStack resource,boolean doFill) {
		return 0;
	}
	@Override
	public @Nullable FluidStack drain(FluidStack resource,boolean doDrain) {
		return null;
	}
	@Override
	public @Nullable FluidStack drain(int maxDrain,boolean doDrain) {
		return null;
	}
}
