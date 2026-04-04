package com.leafia.contents.machines.misc.modular_turbine.core;

import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.ReturnCodeError;
import com.leafia.dev.custompacket.LeafiaCustomPacket;
import com.leafia.dev.custompacket.LeafiaCustomPacketEncoder;
import com.leafia.dev.optimization.bitbyte.LeafiaBuf;
import com.leafia.passive.rendering.TopRender.Highlight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MTCoreBlock extends ModularTurbineBlockBase {
	public MTCoreBlock(String s) {
		super(s);
	}
	@Override
	public int shaftHeight() {
		return 0;
	}
	@Override
	public TurbineComponentType componentType() {
		return null;
	}
	@Override
	public int[] canConnectTo() {
		return new int[]{}; // doesn't matter for the core
	}
	@Override
	public int size() {
		return 0; // doesn't matter for the core
	}
	@Override
	public double weight() {
		return 0;
	}
	@Override
	public int[] getDimensions() {
		return new int[]{ 0,0,0,0,0,0 };
	}
	@Override
	public int getOffset() {
		return 0;
	}
	@Override
	public @Nullable TileEntity createNewTileEntity(World worldIn,int meta) {
		return new MTCoreTE();
	}
	public static class TurbineErrorHighlight implements LeafiaCustomPacketEncoder {
		BlockPos posA;
		BlockPos posB;
		public TurbineErrorHighlight() { }
		public TurbineErrorHighlight(BlockPos posA,BlockPos posB) {
			this.posA = posA;
			this.posB = posB;
		}
		@Override
		public void encode(LeafiaBuf buf) {
			buf.writeVec3i(posA);
			buf.writeVec3i(posB);
		}
		@Override
		public @Nullable Consumer<MessageContext> decode(LeafiaBuf buf) {
			posA = buf.readPos();
			posB = buf.readPos();
			return (ctx)->{
				Highlight highlightA = new Highlight(posA);
				Highlight highlightB = new Highlight(posB);
				highlightA.setLabel("ERROR");
				highlightB.setLabel("ERROR");
				highlightA.setLifetime(8);
				highlightB.setLifetime(8);
				highlightA.setColor(0xFF0000);
				highlightB.setColor(0xFF0000);
				highlightA.ray = new Vec3d(posB).subtract(new Vec3d(posA));
				highlightA.show();
				highlightB.show();
			};
		}
	}
	@Override
	public boolean onBlockActivated(World worldIn,BlockPos pos,IBlockState state,EntityPlayer playerIn,EnumHand hand,EnumFacing facing,float hitX,float hitY,float hitZ) {
		if (worldIn.getTileEntity(pos) instanceof MTCoreTE core) {
			if (core.components.isEmpty()) {
				if (!worldIn.isRemote) {
					AssemblyReturnCode code = core.reassemble();
					if (code instanceof ReturnCodeError error) {
						playerIn.sendMessage(new TextComponentTranslation("chat.turbine.error."+error.reason.name().toLowerCase()).setStyle(new Style().setColor(TextFormatting.RED)));
						LeafiaCustomPacket.__start(new TurbineErrorHighlight(error.errorPosA,error.errorPosB)).__sendToClient(playerIn);
					}
				}
				return true;
			}
		}
		return false;
	}
}
