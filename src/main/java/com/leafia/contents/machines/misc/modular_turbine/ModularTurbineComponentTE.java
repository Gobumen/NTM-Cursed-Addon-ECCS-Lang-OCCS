package com.leafia.contents.machines.misc.modular_turbine;

import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.TurbineAssembly;
import net.minecraft.tileentity.TileEntity;

public class ModularTurbineComponentTE extends TileEntity implements IMTStageUpgradeContributor, IMTMachineUpgradeContributor {
	public MTCoreTE core;
	public TurbineAssembly assembly;
	@Override
	public void invalidate() {
		if (core != null)
			core.disassemble();
		super.invalidate();
	}
}
