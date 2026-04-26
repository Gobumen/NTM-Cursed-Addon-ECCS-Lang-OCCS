package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.fluid.CoriumFinite;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.leafia.contents.AddonBlocks;
import com.leafia.contents.fluids.BaleCoriumFluid.BaleCoriumFluidBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(value = CoriumFinite.class)
public class MixinCoriumFinite extends BlockFluidFinite {
	@Shadow(remap = false)
	@Final
	private Random rand;

	public MixinCoriumFinite(Fluid fluid,Material material) {
		super(fluid,material);
	}
	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(IBlockState stateIn,World world,BlockPos pos,Random rand) {
		super.randomDisplayTick(stateIn,world,pos,rand);
		if (rand.nextInt(5) == 0) {
			if (rand.nextInt(20) == 0)
				world.playSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.7F + rand.nextFloat() * 0.25F, false);
			{
				int amt = rand.nextInt(2)+1;
				for (int i = 0; i < amt; i++)
					world.spawnParticle(
							EnumParticleTypes.LAVA,
							pos.getX()+rand.nextFloat(),pos.getY()+rand.nextFloat(),pos.getZ()+rand.nextFloat(),
							0,0,0
					);
			}
			{
				int amt = rand.nextInt(3);
				for (int i = 0; i < amt; i++)
					world.spawnParticle(
							EnumParticleTypes.SMOKE_NORMAL,
							pos.getX()+rand.nextFloat(),pos.getY()+rand.nextFloat(),pos.getZ()+rand.nextFloat(),
							rand.nextDouble()/2-0.25,rand.nextDouble()/2-0.25,rand.nextDouble()/2-0.25
					);
			}
			{
				int amt = rand.nextInt(3);
				for (int i = 0; i < amt; i++)
					world.spawnParticle(
							EnumParticleTypes.SMOKE_LARGE,
							pos.getX()+rand.nextFloat(),pos.getY()+rand.nextFloat(),pos.getZ()+rand.nextFloat(),
							rand.nextDouble()/2-0.25,rand.nextDouble()/2-0.25,rand.nextDouble()/2-0.25
					);
			}
		}
		if (rand.nextInt(100) == 0)
			world.playSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.7F + rand.nextFloat() * 0.25F, false);
	}
	protected boolean isSurroundingBlockFlammable(World worldIn, BlockPos pos)
	{
		for (EnumFacing enumfacing : EnumFacing.values())
		{
			if (this.getCanBlockBurn(worldIn, pos.offset(enumfacing)))
			{
				return true;
			}
		}

		return false;
	}
	private boolean getCanBlockBurn(World worldIn, BlockPos pos)
	{
		return pos.getY() >= 0 && pos.getY() < 256 && !worldIn.isBlockLoaded(pos) ? false : worldIn.getBlockState(pos).getMaterial().getCanBurn();
	}
	/**
	 * @author ntmleafia
	 * @reason rad adjust & make it solidify even if theres corium below
	 */
	@Overwrite
	public void updateTick(@NotNull World world,@NotNull BlockPos pos,@NotNull IBlockState state,@NotNull Random random) {
		super.updateTick(world, pos, state, random);
		boolean isBalefire = this == AddonBlocks.fluid_balecorium;
		ChunkRadiationManager.proxy.incrementRad(world, pos, isBalefire ? 500 : 50);
		if (!world.isRemote) {
			if (random.nextInt(10) == 0/* && world.getBlockState(pos.down()).getBlock() != this*/) {
				if (random.nextInt(3) == 0) {
					world.setBlockState(pos,ModBlocks.block_corium.getDefaultState());
				} else {
					world.setBlockState(pos,ModBlocks.block_corium_cobble.getDefaultState());
				}
			}
			if (world.getGameRules().getBoolean("doFireTick")) {
				IBlockState fireState = isBalefire ? ModBlocks.balefire.getDefaultState() : Blocks.FIRE.getDefaultState();
				int i = rand.nextInt(3);

				if (i > 0) {
					BlockPos blockpos = pos;

					for (int j = 0; j < i; ++j) {
						blockpos = blockpos.add(rand.nextInt(3)-1,1,rand.nextInt(3)-1);

						if (blockpos.getY() >= 0 && blockpos.getY() < world.getHeight() && !world.isBlockLoaded(blockpos)) {
							return;
						}

						IBlockState block = world.getBlockState(blockpos);

						if (block.getBlock().isAir(block,world,blockpos)) {
							if (this.isSurroundingBlockFlammable(world,blockpos)) {
								world.setBlockState(blockpos,ForgeEventFactory.fireFluidPlaceBlockEvent(world,blockpos,pos,fireState));
								return;
							}
						} else if (block.getMaterial().blocksMovement()) {
							return;
						} else {
							IBlockState state1 = world.getBlockState(blockpos);
							Block bock = state1.getBlock();
							if (bock == Blocks.COBBLESTONE || bock == Blocks.STONE || bock == AddonBlocks.baleonitite || bock == Blocks.MONSTER_EGG || state1.getMaterial().equals(Material.ROCK)) {
								int rng = (rand.nextInt(3)+1)^2;
								if (rng <= 1) world.setBlockState(blockpos,AddonBlocks.baleonitite.getDefaultState().withProperty(BlockMeta.META,2));
								else if (rng <= 4) world.setBlockState(blockpos,AddonBlocks.baleonitite.getDefaultState().withProperty(BlockMeta.META,1));
								else if (rng <= 9) world.setBlockState(blockpos,AddonBlocks.baleonitite.getDefaultState().withProperty(BlockMeta.META,0));
							}
						}
					}
				} else {
					for (int k = 0; k < 3; ++k) {
						BlockPos blockpos1 = pos.add(rand.nextInt(3)-1,0,rand.nextInt(3)-1);

						if (blockpos1.getY() >= 0 && blockpos1.getY() < 256 && !world.isBlockLoaded(blockpos1)) {
							return;
						}

						if (world.isAirBlock(blockpos1.up()) && this.getCanBlockBurn(world,blockpos1)) {
							world.setBlockState(blockpos1.up(),ForgeEventFactory.fireFluidPlaceBlockEvent(world,blockpos1.up(),pos,fireState));
						} else {
							IBlockState state1 = world.getBlockState(blockpos1);
							Block bock = state1.getBlock();
							if (bock == Blocks.COBBLESTONE || bock == Blocks.STONE || bock == AddonBlocks.baleonitite || bock == Blocks.MONSTER_EGG || state1.getMaterial().equals(Material.ROCK)) {
								int rng = (rand.nextInt(3)+1)^2;
								if (rng <= 1) world.setBlockState(blockpos1,AddonBlocks.baleonitite.getDefaultState().withProperty(BlockMeta.META,2));
								else if (rng <= 4) world.setBlockState(blockpos1,AddonBlocks.baleonitite.getDefaultState().withProperty(BlockMeta.META,1));
								else if (rng <= 9) world.setBlockState(blockpos1,AddonBlocks.baleonitite.getDefaultState().withProperty(BlockMeta.META,0));
							}
						}
					}
				}
			}
		}
	}
}
