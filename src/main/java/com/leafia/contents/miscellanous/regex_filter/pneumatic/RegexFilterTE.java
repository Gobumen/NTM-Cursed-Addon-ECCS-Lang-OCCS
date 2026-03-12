package com.leafia.contents.miscellanous.regex_filter.pneumatic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class RegexFilterTE extends TileEntity {
	public final ItemStackHandler inventory = new ItemStackHandler(1) {};
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		inventory.deserializeNBT(compound.getCompoundTag("inventory"));
	}
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory",inventory.serializeNBT());
		return super.writeToNBT(compound);
	}
	EnumFacing getFacing() {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof RegexFilterBlock)
			return state.getValue(RegexFilterBlock.FACING);
		return EnumFacing.UP;
	}
	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		if ((facing == null || facing.getOpposite().equals(getFacing())) && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return true;
		return super.hasCapability(capability,facing);
	}
	@Override
	public @Nullable <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if ((facing == null || facing.getOpposite().equals(getFacing())) && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
		return super.getCapability(capability,facing);
	}
}
