package com.leafia.contents.building.generic_doors.special.reactor_door;

import com.hbm.tileentity.DoorDecl;
import com.leafia.contents.building.generic_doors.AddonDoorGeneric;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ReactorDoorBlock extends AddonDoorGeneric {
	public ReactorDoorBlock(Material materialIn,DoorDecl type,boolean isRadResistant,String s) {
		super(materialIn,type,isRadResistant,s);
	}
	@Override
	public TileEntity createNewTileEntity(World worldIn,int meta) {
		return meta >= 12 ? new ReactorDoorTE() : null;
	}
}
