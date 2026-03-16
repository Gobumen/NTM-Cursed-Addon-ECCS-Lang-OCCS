package com.leafia.contents.building.generic_doors.special.reactor_door;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.control_panel.ControlEvent;
import com.hbm.inventory.control_panel.ControlEventSystem;
import com.hbm.inventory.control_panel.DataValueFloat;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.leafia.CommandLeaf;
import com.leafia.init.LeafiaSoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.function.Consumer;

public class ReactorDoorTE extends TileEntityDoorGeneric {
	@Override
	public void update() {
		if (getDoorType() == null && this.getBlockType() instanceof BlockDoorGeneric)
			setDoorType(((BlockDoorGeneric) this.getBlockType()).type);
		Consumer<TileEntityDoorGeneric> update = getDoorType().onDoorUpdate();
		if (update != null)
			update.accept(this);
		if (state == DoorState.OPENING) {
			if (openTicks == 0)
				world.playSound(null,pos,LeafiaSoundEvents.reactor_door_handle,SoundCategory.BLOCKS,1,1);
			if (openTicks == 43) {
				PacketThreading.createSendToAllTrackingThreadedPacket(
						new CommandLeaf.ShakecamPacket(
								new String[]{
										"type=smooth",
										"intensity=0.15",
										"duration=0.5",
										"speed=1",
										"blurDulling=50",
										"bloomDulling=50",
										"range=5",
										"curve=0.5"
								}).setPos(pos),
						new NetworkRegistry.TargetPoint(
								world.provider.getDimension(),pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,10)
				);
				world.playSound(null,pos,LeafiaSoundEvents.reactor_door_open,SoundCategory.BLOCKS,3,1);
			}
			openTicks++;
			if (openTicks >= getDoorType().timeToOpen()) {
				openTicks = getDoorType().timeToOpen();
			}
		} else if (state == DoorState.CLOSING) {
			if (openTicks == getDoorType().timeToOpen()-23)
				world.playSound(null,pos,LeafiaSoundEvents.reactor_door_close,SoundCategory.BLOCKS,1,1);
			if (openTicks == getDoorType().timeToOpen()-44-2) { // why is it off sync
				PacketThreading.createSendToAllTrackingThreadedPacket(
						new CommandLeaf.ShakecamPacket(
								new String[]{
										"type=smooth",
										"intensity=0.35",
										"duration=0.75",
										"speed=8",
										"blurDulling=50",
										"bloomDulling=50",
										"range=5",
										"curve=0.5"
								}).setPos(pos),
						new NetworkRegistry.TargetPoint(
								world.provider.getDimension(),pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,10)
				);
			}
			if (openTicks == getDoorType().timeToOpen()-43-23)
				world.playSound(null,pos,LeafiaSoundEvents.reactor_door_handle,SoundCategory.BLOCKS,3,1);
			openTicks--;
			if (openTicks <= 0) {
				openTicks = 0;
			}
		}

		if (!world.isRemote) {
			int[][] ranges = getDoorType().getDoorOpenRanges();
			ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset);
			if (state == DoorState.OPENING) {
				for (int i = 0; i < ranges.length; i++) {
					int[] range = ranges[i];
					BlockPos startPos = new BlockPos(range[0], range[1], range[2]);
					float time = getDoorType().getDoorRangeOpenTime(openTicks, i);
					for (int j = 0; j < Math.abs(range[3]); j++) {
						if ((float) j / (Math.abs(range[3] - 1)) > time) break;
						for (int k = 0; k < range[4]; k++) {
							new BlockPos(0, 0, 0);
							BlockPos add = switch (EnumFacing.Axis.values()[range[5]]) {
								case X -> new BlockPos(0, k, Math.signum(range[3]) * j);
								case Y -> new BlockPos(k, Math.signum(range[3]) * j, 0);
								case Z -> new BlockPos(Math.signum(range[3]) * j, k, 0);
							};
							Rotation r = dir.getBlockRotation();
							if (dir.toEnumFacing().getAxis() == EnumFacing.Axis.X) r = r.add(Rotation.CLOCKWISE_180);
							BlockPos finalPos = startPos.add(add).rotate(r).add(pos);
							if (finalPos.equals(this.pos)) {
								this.shouldUseBB = true;
							} else {
								((BlockDummyable) getBlockType()).makeExtra(world, finalPos.getX(), finalPos.getY(), finalPos.getZ());
							}
						}
					}
				}
			} else if (state == DoorState.CLOSING) {
				for (int i = 0; i < ranges.length; i++) {
					int[] range = ranges[i];
					BlockPos startPos = new BlockPos(range[0], range[1], range[2]);
					float time = getDoorType().getDoorRangeOpenTime(openTicks, i);
					for (int j = Math.abs(range[3]) - 1; j >= 0; j--) {
						if ((float) j / (Math.abs(range[3] - 1)) < time) break;
						for (int k = 0; k < range[4]; k++) {
							new BlockPos(0, 0, 0);
							BlockPos add = switch (EnumFacing.Axis.values()[range[5]]) {
								case X -> new BlockPos(0, k, Math.signum(range[3]) * j);
								case Y -> new BlockPos(k, Math.signum(range[3]) * j, 0);
								case Z -> new BlockPos(Math.signum(range[3]) * j, k, 0);
							};
							Rotation r = dir.getBlockRotation();
							if (dir.toEnumFacing().getAxis() == EnumFacing.Axis.X) r = r.add(Rotation.CLOCKWISE_180);
							BlockPos finalPos = startPos.add(add).rotate(r).add(pos);
							if (finalPos.equals(this.pos)) {
								this.shouldUseBB = false;
							} else {
								((BlockDummyable) getBlockType()).removeExtra(world, finalPos.getX(), finalPos.getY(), finalPos.getZ());
							}
						}
					}
				}
			}
			if (state == DoorState.OPENING && openTicks == getDoorType().timeToOpen()) {
				state = DoorState.OPEN;
				broadcastControlEvt();
			}
			if (state == DoorState.CLOSING && openTicks == 0) {
				state = DoorState.CLOSED;
				broadcastControlEvt();

				// With door finally closed, mark chunk for rad update since door is now rad resistant
				// No need to update when open as well, as opening door should update
				RadiationSystemNT.markSectionsForRebuild(world, getOccupiedSections());
			}
			//PacketDispatcher.wrapper.sendToAllAround(new TEDoorAnimationPacket(pos, (byte) state.ordinal(), (byte) (shouldUseBB ? 1 : 0)),
			//        new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 100));

			this.networkPackNT(100);
		}
	}

	private void broadcastControlEvt() {
		ControlEventSystem.get(world).broadcastToSubscribed(this, ControlEvent.newEvent("door_open_state").setVar("state",
				new DataValueFloat(state.ordinal())));
	}
}
