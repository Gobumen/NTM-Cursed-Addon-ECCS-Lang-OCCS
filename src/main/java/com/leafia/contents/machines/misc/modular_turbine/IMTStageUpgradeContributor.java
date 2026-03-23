package com.leafia.contents.machines.misc.modular_turbine;

import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreData.StageUpgradeSummary;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.TurbineAssembly;

public interface IMTStageUpgradeContributor {
	default void contributeStageUpgradeOffsets(StageUpgradeSummary summary,MTCoreTE core,TurbineAssembly assembly,ModularTurbineComponentTE component) { }
}
