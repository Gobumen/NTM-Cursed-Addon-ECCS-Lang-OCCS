package com.leafia.contents.building.storage.rack;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.leafia.dev.LeafiaUtil;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Set;

public class RackTE extends TileEntity implements IGUIProvider {
	/// has to be blocks that provides TileEntityCrateBase otherwise it'll do nothing
	public static final Set<String> supportedTEBlocks = new HashSet<>();
	public boolean isSupported(Block block) {
		if (block == ModBlocks.crate_iron) return true;
		if (block == ModBlocks.crate_steel) return true;
		if (block == ModBlocks.crate_tungsten) return true;
		if (block == ModBlocks.crate_desh) return true;
		if (block == ModBlocks.crate_template) return true;
		return false;
	}
	public Container provideContainer(int id,EntityPlayer player,World world,int x,int y,int z) {
		if (id >= 0 && id < 4) {

		}
		return null;
	}
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int id,EntityPlayer player,World world,int x,int y,int z) {
		if (id >= 0 && id < 4) {
		}
		return null;
	}
	public final Block[] crates = new Block[4];
	public final int[] metas = new int[4];
	public boolean storeCrate(ItemStack item,int index) {
		if (item.getItem() instanceof ItemBlock ib) {
			Block block = ib.getBlock();
			IBlockState state = block.getStateFromMeta(item.getMetadata());
			if (LeafiaUtil.isSolidVisibleCube(state)) {
				crates[index] = block;
				metas[index] = item.getMetadata();
				return true;
			}
		}
		return false;
	}
	public ItemStack removeCrate(int index) {
		Block block = crates[index];
		int meta = metas[index];
		crates[index] = null;
		metas[index] = 0;
		if (block != null) {
			ItemStack stack = new ItemStack(block,1,meta);
			return stack;
		}
		return ItemStack.EMPTY;
	}
	public RackTE() {
		storeCrate(new ItemStack(ModBlocks.crate_iron),0);
	}
	AxisAlignedBB bb = null;
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if (bb == null) {
			bb = new AxisAlignedBB(
					getPos().getX()-1,
					getPos().getY(),
					getPos().getZ()-1,
					getPos().getX()+2,
					getPos().getY()+2,
					getPos().getZ()+2
			);
		}
		return bb;
	}
}
