package com.leafia.contents.machines.misc.modular_turbine;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IPersistentNBT;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE;
import com.leafia.contents.machines.misc.modular_turbine.ports.IMTPortBlock;
import com.leafia.contents.machines.misc.modular_turbine.ports.MTComponentPortTE;
import com.leafia.dev.blocks.blockbase.AddonBlockDummyable;
import com.leafia.dev.container_utility.LeafiaPacket;
import com.leafia.dev.machine.MachineTooltip;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ModularTurbineBlockBase extends AddonBlockDummyable implements IToolable, ILookOverlay, IMTStageUpgradeContributor, IMTMachineUpgradeContributor {
	public ModularTurbineBlockBase(String s) {
		super(Material.IRON,s);
		setHardness(5);
		setResistance(10);
		setCreativeTab(MainRegistry.machineTab);
	}
	public abstract int shaftHeight();
	public enum TurbineComponentType {
		BLADES(true),
		SEPARATOR,PORT(true),
		INLINE_PORT, // special
		FLYWHEEL,
		SHAFT_ONLY,
		GENERATOR,
		;
		public final boolean isSeparator;
		TurbineComponentType(boolean needsSeparation) {
			this.isSeparator = !needsSeparation;
		}
		TurbineComponentType() {
			isSeparator = true;
		}
	}
	@Override
	public void addInformation(ItemStack stack,@Nullable World worldIn,List<String> tooltip,ITooltipFlag flagIn) {
		MachineTooltip.addWIP(tooltip);
		tooltip.add(TextFormatting.AQUA+I18nUtil.resolveKey("info.turbine.weight","+"+weight()+" WU"));
		super.addInformation(stack,worldIn,tooltip,flagIn);
	}
	public abstract TurbineComponentType componentType();
	/// Sizes this component can connect to
	/// <p>Empty array means all components.
	public abstract int[] canConnectTo();
	public abstract int size();
	public abstract double weight();
/*
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.SOLID;
	}
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
	{
		return getRenderLayer() == layer;
	}*/

	@Override
	public void onBlockPlacedBy(@NotNull World world, @NotNull BlockPos pos, @NotNull IBlockState state, @NotNull EntityLivingBase player, @NotNull ItemStack itemStack) {
		if(!(player instanceof EntityPlayer pl))
			return;
		safeRem = true;
		world.setBlockToAir(pos);
		safeRem = false;

		EnumHand hand = pl.getHeldItemMainhand() == itemStack ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;

		double ang = player.rotationYaw;
		if (player.isSneaking())
			ang += 180;

		int i = MathHelper.floor((ang) * 4.0F / 360.0F + 0.5D) & 3;
		int o = -getOffset();
		pos = new BlockPos(pos.getX(), pos.getY() + getHeightOffset(), pos.getZ());

		ForgeDirection dir = switch (i) {
			case 0 -> ForgeDirection.getOrientation(2);
			case 1 -> ForgeDirection.getOrientation(5);
			case 2 -> ForgeDirection.getOrientation(3);
			case 3 -> ForgeDirection.getOrientation(4);
			default -> ForgeDirection.NORTH;
		};

		dir = getDirModified(dir);

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if(!checkRequirement(world, x, y, z, dir, o)) {
			if(!pl.capabilities.isCreativeMode) {
				ItemStack stack = pl.inventory.mainInventory.get(pl.inventory.currentItem);
				Item item = Item.getItemFromBlock(this);

				if(stack.isEmpty()) {
					pl.inventory.mainInventory.set(pl.inventory.currentItem, new ItemStack(this));
				} else {
					if(stack.getItem() != item || stack.getCount() == stack.getMaxStackSize()) {
						pl.inventory.addItemStackToInventory(new ItemStack(this));
					} else {
						pl.getHeldItem(hand).grow(1);
					}
				}
			}

			return;
		}

		if(!world.isRemote){
			BlockPos cur = new BlockPos(x + dir.offsetX * o , y + dir.offsetY * o, z + dir.offsetZ * o);
			int meta = getMetaForCore(world, cur, pl, dir.ordinal() + offset);
			world.setBlockState(cur, this.getDefaultState().withProperty(META, meta), 3);
			IPersistentNBT.onBlockPlacedBy(world, cur, itemStack);
			fillSpace(world, x, y, z, dir, o);
		}
		pos = new BlockPos(pos.getX(), pos.getY() - getHeightOffset(), pos.getZ());
		world.scheduleUpdate(pos, this, 1);
		world.scheduleUpdate(pos, this, 2);
	}
	@SideOnly(Side.CLIENT)
	@Override
	public void printHook(RenderGameOverlayEvent.Pre event,World world,BlockPos pos) {
		List<String> texts = new ArrayList<>();
		BlockPos core = findCore(world,pos);
		if (core != null) {
			if (world.getTileEntity(core) instanceof ModularTurbineComponentTE te) {
				if (te.core == null)
					texts.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]"+I18nUtil.resolveKey("info.turbine.assembly.unassembled"));
				if (te instanceof MTComponentPortTE port) {
					texts.add(I18nUtil.resolveKey("info.turbine.identifier",port.identifier != null ? port.identifier.getLocalizedName() : "N/A"));
					texts.add(I18nUtil.resolveKey("info.turbine.decompression",I18nUtil.resolveKey(port.decompress ? "info.turbine.state.enabled" : "info.turbine.state.disabled")));
					if (te.assembly != null) {
						texts.add(TextFormatting.GREEN+"-> "+TextFormatting.RESET+te.assembly.input.getTankType().getLocalizedName()+": "+te.assembly.input.getFill()+"/"+te.assembly.input.getMaxFill()+"mB");
						texts.add(TextFormatting.RED+"<- "+TextFormatting.RESET+te.assembly.output.getTankType().getLocalizedName()+": "+te.assembly.output.getFill()+"/"+te.assembly.output.getMaxFill()+"mB");
					}
				}
			}
		}
		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xFF55FF, 0x3F153F, texts);
	}
	@Override
	public boolean onBlockActivated(World world,BlockPos pos,IBlockState state,EntityPlayer player,EnumHand hand,EnumFacing facing,float hitX,float hitY,float hitZ) {
		if (this instanceof IMTPortBlock) {
			BlockPos core = findCore(world,pos);
			if (core != null && world.getTileEntity(core) instanceof MTComponentPortTE port) {
				ItemStack stack = player.getHeldItem(hand);
				if (stack.getItem() instanceof IItemFluidIdentifier identifier) {
					FluidType type = identifier.getType(world,core.getX(),core.getY(),core.getZ(),stack);
					if (MTCoreTE.steamTypes.contains(type)) {
						if (port.identifier == null || !port.identifier.equals(type)) {
							if (!world.isRemote) {
								port.identifier = type;
								LeafiaPacket._start(port)
										.__write(MTPacketId.PORT_IDENTIFIER.id,port.identifier.getName())
										.__sendToAffectedClients();
							}
							if (!world.isRemote && port.core != null)
								port.core.disassemble();
							return true;
						}
					}
				}
			}
		}
		return super.onBlockActivated(world,pos,state,player,hand,facing,hitX,hitY,hitZ);
	}
	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, ToolType tool) {
		return onScrew(world, player, new BlockPos(x, y, z), side, fX, fY, fZ, hand, tool);
	}
	@Override
	public boolean onScrew(World world,EntityPlayer player,BlockPos pos,EnumFacing side,float fX,float fY,float fZ,EnumHand hand,ToolType tool) {
		if (this instanceof IMTPortBlock) {
			BlockPos core = findCore(world,pos);
			if (core != null && world.getTileEntity(core) instanceof MTComponentPortTE port) {
				boolean changed = false;
				if (player.isSneaking()) {
					if (port.identifier != null) {
						changed = true;
						if (!world.isRemote) {
							port.identifier = null;
							LeafiaPacket._start(port)
									.__write(MTPacketId.PORT_IDENTIFIER.id,null)
									.__sendToAffectedClients();
						}
					}
				} else {
					if (!world.isRemote) {
						port.decompress = !port.decompress;
						LeafiaPacket._start(port)
								.__write(MTPacketId.PORT_DECOMPRESSION.id,port.decompress)
								.__sendToAffectedClients();
					}
					changed = true;
				}
				if (changed) {
					if (!world.isRemote && port.core != null)
						port.core.disassemble();
					return true;
				}
			}
		}
		return false;
	}
}
