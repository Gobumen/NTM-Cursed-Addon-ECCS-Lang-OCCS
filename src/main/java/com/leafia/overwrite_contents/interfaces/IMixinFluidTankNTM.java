package com.leafia.overwrite_contents.interfaces;

import net.minecraft.nbt.NBTTagCompound;

public interface IMixinFluidTankNTM {

    NBTTagCompound leafia$getAddonNbt();

    void leafia$setAddonNbt(NBTTagCompound nbt);
}
