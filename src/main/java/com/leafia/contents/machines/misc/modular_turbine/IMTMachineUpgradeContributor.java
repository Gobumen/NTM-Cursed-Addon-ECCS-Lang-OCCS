package com.leafia.contents.machines.misc.modular_turbine;

import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreData.MachineUpgradeSummary;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE;

public interface IMTMachineUpgradeContributor {
	default void contributeMachineUpgradeOffsets(MachineUpgradeSummary summary,MTCoreTE core,ModularTurbineComponentTE component) { }
}
