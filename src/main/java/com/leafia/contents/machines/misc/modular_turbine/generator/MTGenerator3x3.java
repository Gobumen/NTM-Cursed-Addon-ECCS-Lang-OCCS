package com.leafia.contents.machines.misc.modular_turbine.generator;

import com.hbm.tileentity.TileEntityProxyCombo;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineComponentTE;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreData.StageUpgradeContext;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreData.StageUpgradeSummary;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MTGenerator3x3 extends ModularTurbineBlockBase implements IMTGeneratorBlock {
	public MTGenerator3x3(String s) {
		super(s);
	}
	@Override
	public int shaftHeight() {
		return 1;
	}
	@Override
	public TurbineComponentType componentType() {
		return TurbineComponentType.GENERATOR;
	}
	@Override
	public int[] canConnectTo() {
		return new int[]{};
	}
	@Override
	public int size() {
		return 3;
	}
	@Override
	public double weight() {
		return 0.1;
	}
	@Override
	public int[] getDimensions() {
		return new int[]{ 2,0,1,1,1,1 };
	}
	@Override
	public int getOffset() {
		return 1;
	}
	@Override
	public @Nullable TileEntity createNewTileEntity(World worldIn,int meta) {
		if (meta >= 12) return new ModularTurbineComponentTE();
		if (meta >= extra) return new TileEntityProxyCombo(false,false,true);
		return null;
	}
	@Override
	public void contributeStageUpgradeOffsets(StageUpgradeSummary summary,MTCoreTE core,StageUpgradeContext context,ModularTurbineComponentTE component) {

	}
}
