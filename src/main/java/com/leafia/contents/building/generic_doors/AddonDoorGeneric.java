package com.leafia.contents.building.generic_doors;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import com.leafia.contents.AddonBlocks;
import net.minecraft.block.material.Material;

public class AddonDoorGeneric extends BlockDoorGeneric {
	public AddonDoorGeneric(Material materialIn,DoorDecl type,boolean isRadResistant,String s) {
		super(materialIn,type,isRadResistant,s);
		ModBlocks.ALL_BLOCKS.remove(this);
		AddonBlocks.ALL_BLOCKS.add(this);
	}
}
