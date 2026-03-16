package com.leafia.contents.building.generic_doors;

import com.hbm.interfaces.IDoor.DoorState;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.IType;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna;
import com.hbm.render.tileentity.door.IRenderDoors;
import com.hbm.tileentity.DoorDecl;
import com.leafia.contents.building.generic_doors.renderers.ReactorDoorRender;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class AddonDoorDecl {
	public static final DoorDecl REACTOR_DOOR = new DoorDecl() {
		@Override
		@SideOnly(Side.CLIENT)
		public IRenderDoors getSEDNARenderer() {
			return ReactorDoorRender.INSTANCE;
		}

		public static final int handleLockDuration = 3;
		public static final double handleLockAmount = 0.25;

		@Override
		public BusAnimationSedna getBusAnimation(DoorState state,byte skinIndex) {
			if(state == DoorState.OPENING) {
				return new BusAnimationSedna()
						.addBus(
								"HANDLE",
								new BusAnimationSequenceSedna()
										.setPos(0,0,0)
										.addPos(0,handleLockAmount,0,handleLockDuration*50,IType.SIN_FULL)
										.addPos(0,1-handleLockAmount,0,(20-handleLockDuration*2)*50)
										.addPos(0,1,0,handleLockDuration*50,IType.SIN_FULL)
						).addBus(
								"DOOR",
								new BusAnimationSequenceSedna()
										.setPos(0,0,0)
										.addPos(0,0,0,43*50)
										.addPos(0,1,0,44*50,IType.SIN_FULL)
						);
			}
			if(state == DoorState.CLOSING) {
				return new BusAnimationSedna()
						.addBus(
								"DOOR",
								new BusAnimationSequenceSedna()
										.setPos(0,1,0)
										.addPos(0,0,0,44*50,IType.SIN_UP)
						)
						.addBus(
								"HANDLE",
								new BusAnimationSequenceSedna()
										.setPos(0,1,0)
										.addPos(0,1,0,(23+43)*50)
										.addPos(0,1-handleLockAmount,0,handleLockDuration*50,IType.SIN_FULL)
										.addPos(0,handleLockAmount,0,(20-handleLockDuration*2)*50)
										.addPos(0,0,0,handleLockDuration*50,IType.SIN_FULL)
						);
			}
			return null;
		}

		@Override public int timeToOpen() { return 86+23; };
		// someone decipher this please
		// update: it's apparently
		//  startX,
		//  startY,
		//  startZ,
		//  amount or something idk, [j]
		//  width/height (depending on axis), [k]
		//  axis
		// it don't support doors any thicker than 1 meter. SMH
		@Override public int[][] getDoorOpenRanges() { return new int[][] { { 0, 0, 1, -2, 2, 0 } }; }
		@Override public int[] getDimensions() { return new int[] { 2, 0, 1, 0, 1, 1 }; }

		@Override
		public AxisAlignedBB getBlockBound(BlockPos relPos,boolean open) {
			/*
			if(!open)
				return new AxisAlignedBB(0, 0, 0.5, 1, 1, 1);
			if(relPos.getY() > 1)
				return new AxisAlignedBB(0, 0.5, 0.5, 1, 1, 1);
			else if(relPos.getY() == 0)
				return new AxisAlignedBB(0, 0, 0.5, 1, 0.1, 1);*/
			return super.getBlockBound(relPos, open);
		};
	};
}
