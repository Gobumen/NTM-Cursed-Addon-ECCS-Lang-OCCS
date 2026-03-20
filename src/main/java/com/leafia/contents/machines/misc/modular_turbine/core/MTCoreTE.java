package com.leafia.contents.machines.misc.modular_turbine.core;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.leafia.contents.machines.misc.modular_turbine.MTPacketId;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase.TurbineComponentType;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineComponentTE;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.AssemblyErrorReason;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.ReturnCodeError;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.ReturnCodeSuccess;
import com.leafia.contents.machines.misc.modular_turbine.ports.MTComponentPortTE;
import com.leafia.dev.LeafiaDebug;
import com.leafia.dev.container_utility.LeafiaPacket;
import com.leafia.dev.container_utility.LeafiaPacketReceiver;
import com.llib.math.SIPfx;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MTCoreTE extends TileEntity implements LeafiaPacketReceiver, ITickable {
	public double rps;
	public final List<ModularTurbineComponentTE> components = new ArrayList<>();
	public final List<TurbineAssembly> assemblies = new ArrayList<>();
	public int length = 0;
	public double weight = 0;
	public static final List<FluidType> steamTypes = new ArrayList<>();
	public static FluidType getNextSteam(FluidType type,boolean decompress) {
		int index = steamTypes.indexOf(type);
		if (index == -1) return null;
		if (decompress) {
			if (index+1 < steamTypes.size())
				return steamTypes.get(index+1);
			else
				return null;
		} else
			return type;
	}
	@Override
	public String getPacketIdentifier() {
		return "MT_CORE";
	}
	@Override
	public double affectionRange() {
		return 512;
	}
	public boolean assembleFromNBT(NBTTagCompound compound) {
		disassemble();
		boolean ret = assembleFromNBT_internal(compound);
		if (!ret)
			disassemble();
		return ret;
	}
	protected boolean assembleFromNBT_internal(NBTTagCompound compound) {
		length = compound.getInteger("length");
		weight = compound.getDouble("weight");
		EnumFacing dir = EnumFacing.byIndex(getBlockMetadata()-10).getOpposite();
		Map<Integer,TurbineAssembly> assemblyMap = new HashMap<>();

		// LOAD ASSEMBLIES
		for (NBTBase nbtBase : compound.getTagList("assemblies",10)) {
			if (nbtBase instanceof NBTTagCompound tag) {
				TurbineAssembly assembly = new TurbineAssembly();
				assemblies.add(assembly);
				for (NBTBase nbtBase1 : tag.getTagList("positions",2)) {
					if (nbtBase1 instanceof NBTTagShort p) {
						assembly.positionsInOrder.add(p.getInt());
						assemblyMap.put(p.getInt(),assembly);
					} else
						return false;
				}
				assembly.input = new FluidTankNTM(Fluids.NONE,0);
				assembly.output = new FluidTankNTM(Fluids.NONE,0);
				assembly.input.readFromNBT(tag,"input");
				assembly.output.readFromNBT(tag,"output");
				assembly.typeIn = Fluids.fromName(tag.getString("typeIn"));
				assembly.decompress = tag.getBoolean("decompress");
				assembly.bladeCount = tag.getShort("bladeCount");
				assembly.badBlades = tag.getShort("badBlades");
				assembly.size = tag.getByte("size");
			} else return false;
		}

		// HOOK UP TILE-ENTITIES
		for (int i = 0; i < length; i++) {
			BlockPos offs = new BlockPos(pos).offset(dir,i+1);
			if (world.getBlockState(offs).getBlock() instanceof ModularTurbineBlockBase turbine) {
				BlockPos core = turbine.findCore(world,offs);
				if (core != null) {
					if (world.getTileEntity(core) instanceof ModularTurbineComponentTE te) {
						components.add(te);
						te.core = this;
						if (assemblyMap.containsKey(i))
							te.assembly = assemblyMap.get(i);
					} else return false;
				} else return false;
			} else
				return false;
		}
		return true;
	}
	public NBTTagCompound writeAssemblyToNBT(NBTTagCompound compound) {
		compound.setInteger("length",length);
		compound.setDouble("weight",weight);
		NBTTagList assemList = new NBTTagList();
		for (TurbineAssembly assembly : assemblies) {
			NBTTagCompound tag = new NBTTagCompound();
			NBTTagList posList = new NBTTagList();
			for (Integer p : assembly.positionsInOrder)
				posList.appendTag(new NBTTagShort((short)(int)p));
			tag.setTag("positions",posList);
			assembly.input.writeToNBT(tag,"input");
			assembly.output.writeToNBT(tag,"output");
			tag.setString("typeIn",assembly.typeIn.getName());
			tag.setBoolean("decompress",assembly.decompress);
			tag.setShort("bladeCount",(short)assembly.bladeCount);
			tag.setShort("badBlades",(short)assembly.badBlades);
			tag.setByte("size",(byte)assembly.size);
			assemList.appendTag(tag);
		}
		compound.setTag("assemblies",assemList);
		return compound;
	}
	@Override
	public void onReceivePacketLocal(byte key,Object value) {
		if (key == MTPacketId.CORE_ASSEMBLY_SYNC.id) {
			disassemble();
			if (value != null)
				assembleFromNBT((NBTTagCompound)value);
		} if (key == MTPacketId.CORE_STEAM_SYNC.id) {
			NBTTagCompound compound = (NBTTagCompound)value;
			NBTTagList list = compound.getTagList("a",10);
			for (int i = 0; i < assemblies.size(); i++) {
				if (i < list.tagCount()) {
					TurbineAssembly assembly = assemblies.get(i);
					NBTTagCompound tag = list.getCompoundTagAt(i);
					assembly.input.readFromNBT(tag,"i");
					assembly.output.readFromNBT(tag,"o");
				}
			}
		}
	}
	@Override
	public void onReceivePacketServer(byte key,Object value,EntityPlayer plr) { }
	@Override
	public void onPlayerValidate(EntityPlayer plr) { }
	@Override
	public void update() {
		if (!world.isRemote) {
			double targetRPS = 0;
			long powerGenerated = 0;
			if (weight > 0) {
				for (TurbineAssembly assembly : assemblies) {
					// CALCULATE AVAILABLE TRANSFER AMOUNT
					int outputOps = assembly.output.getMaxFill()-assembly.output.getFill();
					double division = 1;
					if (assembly.decompress) division = 10;
					FluidType outType = getNextSteam(assembly.typeIn,assembly.decompress);
					if (outType.equals(Fluids.SPENTSTEAM))
						division *= 10;

					// CURRENT TANK FILL
					int inputOps = assembly.input.getFill();

					// AMOUNT TO TRANSFER
					int ops = Math.min(inputOps,(int)(outputOps/division));
					assembly.input.setFill(assembly.input.getFill()-ops);
					assembly.output.setFill((int)(assembly.output.getFill()+ops*division));

					// RPS CALCULATION
					int compression = steamTypes.size()-steamTypes.indexOf(assembly.typeIn)-1-1;
					targetRPS += Math.pow(2,compression)/200*ops;
				}
				targetRPS = Math.pow(targetRPS,0.8);
				//double targetRPSTurb = targetRPS*Math.max(0,Math.pow((100-turbulence*0.85)/100,2));
				double targetRPSTurb = targetRPS-Math.max(rps-10,0)*Math.pow(turbulence/100,1)*9;
				rps = rps+(targetRPSTurb-rps)/weight;

				double generatorPower = 500;
				double subtracted = rps*(Math.pow(generatorPower,0.65)/1_000_000/Math.pow(weight,1.65));
				rps -= subtracted;
				//powerGenerated += (long)((Math.pow(subtracted+1,1/0.65)-1)*(Math.pow(rps+1,5)-1)*Math.pow(weight,3.5)/30000);
				powerGenerated += (long)((Math.pow(subtracted+1,1/0.65)-1)*(Math.pow(rps+1,2.8)-1)*Math.pow(weight,3.5)/4);
				//powerGenerated += (long)((Math.pow(subtracted+1,5)-1)*50*Math.pow(weight,4));
			}
			rps *= 0.995;
			double turbulenceAdd = Math.pow(Math.max(targetRPS-rps,0)/110,7.2)/Math.max(8,60-turbulence*2);
			turbulence *= 0.99;
			turbulence = Math.min(turbulence+turbulenceAdd,100);
			//LeafiaDebug.debugLog(world,"TURBULENCE: "+turbulence);
			LeafiaDebug.debugLog(world,"RPS: "+rps);
			LeafiaDebug.debugLog(world,"powerGenerated: "+SIPfx.auto(powerGenerated*20)+"HE/s");
			LeafiaDebug.debugLog(world,"TargetRPS-RPS difference: "+(targetRPS-rps));
			LeafiaDebug.debugLog(world,"Turbulence: "+turbulence);
			LeafiaDebug.debugLog(world,"Weight: "+weight+" WU");

			NBTTagCompound syncCompound = new NBTTagCompound();
			NBTTagList syncList = new NBTTagList();
			for (TurbineAssembly assembly : assemblies) {
				NBTTagCompound tag = new NBTTagCompound();
				assembly.input.writeToNBT(tag,"i");
				assembly.output.writeToNBT(tag,"o");
				syncList.appendTag(tag);
			}
			syncCompound.setTag("a",syncList);
			LeafiaPacket._start(this)
					.__write(MTPacketId.CORE_STEAM_SYNC.id,syncCompound)
					.__sendToAffectedClients();
		}
	}
	public static class TurbineAssembly {
		/// NOTE: The values stored here are actually 1 lower than actual offset
		public List<Integer> positionsInOrder = new ArrayList<>();
		public FluidTankNTM input;
		public FluidTankNTM output;
		public FluidType typeIn;
		public boolean decompress = false;
		public int bladeCount = 0;
		public int badBlades = 0;
		public int size = -1;
	}
	public double turbulence;
	public void disassemble() {
		for (ModularTurbineComponentTE component : components) {
			component.assembly = null;
			component.core = null;
		}
		assemblies.clear();
		components.clear();
		length = 0;
		weight = 0;
	}
	public static class AssemblyReturnCode {
		public static class ReturnCodeSuccess extends AssemblyReturnCode { }
		public static class ReturnCodeError extends AssemblyReturnCode {
			public final BlockPos errorPosA;
			public final BlockPos errorPosB;
			public final AssemblyErrorReason reason;
			public ReturnCodeError(BlockPos errorPosA,BlockPos errorPosB,AssemblyErrorReason reason) {
				this.errorPosA = errorPosA;
				this.errorPosB = errorPosB;
				this.reason = reason;
			}
		}
		public enum AssemblyErrorReason {
			OPEN_END,
			NOT_ALIGNED,
			INCOMPATIBLE_SIZE,
			BUG,
			NO_IDENTIFIER,
			IDENTIFIER_MISMATCH,
			CANT_DECOMPRESS,
			INVALID_IDENTIFIER,
			NO_BLADES,
			TOO_LONG,
			SIZE_MISMATCH, // Only the same sized components may be used for each types of steam
		}
	}
	@Override
	public void invalidate() {
		disassemble();
		super.invalidate();
	}
	public AssemblyReturnCode reassemble() {
		AssemblyReturnCode code = reassemble_internal();
		if (code instanceof ReturnCodeError)
			disassemble();
		LeafiaPacket._start(this)
				.__write(MTPacketId.CORE_ASSEMBLY_SYNC.id,writeAssemblyToNBT(new NBTTagCompound()))
				.__sendToAffectedClients();
		return code;
	}
	protected AssemblyReturnCode reassemble_internal() {
		disassemble();
		int[] connectable = new int[]{};
		boolean wasSeparator = false;
		EnumFacing dir = EnumFacing.byIndex(getBlockMetadata()-10).getOpposite();
		BlockPos offs = new BlockPos(pos);
		BlockPos prevPos = offs;
		TurbineAssembly lastAssembly = null;
		BlockPos previousPortPos = null;
		int i = 0;
		int leastCompression = -1;
		int mostCompression = -1;
		Map<FluidType,Integer> typeToSizeMap = new HashMap<>();
		Map<FluidType,Integer> typeToBladesMap = new HashMap<>();
		while (true) {
			if (i > 300) // Who in the sane mind does this?
				return new ReturnCodeError(pos,offs,AssemblyErrorReason.TOO_LONG);
			offs = offs.offset(dir);
			if (world.getBlockState(offs).getBlock() instanceof ModularTurbineBlockBase turbine) {
				boolean aligned = true;
				BlockPos core = turbine.findCore(world,offs);
				weight += turbine.weight();
				if (core == null)
					return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.BUG);
				else {
					if (world.getTileEntity(core) instanceof ModularTurbineComponentTE te) {
						components.add(te);

						// ALIGNMENT CHECK
						if (dir.getAxis() == Axis.X)
							aligned = core.getZ() == offs.getZ();
						else if (dir.getAxis() == Axis.Z)
							aligned = core.getX() == offs.getX();
						aligned = aligned && (core.getY()+turbine.shaftHeight() == offs.getY());
						if (!aligned)
							return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.NOT_ALIGNED);

						// COMPATIBILITY CHECK
						int size = turbine.size();
						boolean compatible = connectable.length == 0 || turbine.canConnectTo().length == 0;
						for (int s : connectable) {
							if (s == size) {
								compatible = true;
								break;
							}
						}
						for (int s : turbine.canConnectTo()) {
							if (s == size) {
								compatible = true;
								break;
							}
						}
						if (!compatible)
							return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.INCOMPATIBLE_SIZE);

						// UPDATING PREVIOUS PARAMETERS & FORMING ASSEMBLY
						connectable = turbine.canConnectTo();
						boolean isSeparator = turbine.componentType().isSeparator;
						if (turbine.componentType().equals(TurbineComponentType.INLINE_PORT)) {
							// IF IT'S INLINE PORT, OVERRIDE isSeparator SO
							// ONE OF THEM WILL FORM AN ASSEMBLY WHEN 2 OF THEM ARE PLACED IN A ROW
							if (wasSeparator)
								isSeparator = false;
						}
						boolean makeAssembly = !isSeparator;

						// IF IT'S INLINE PORT, EXPAND PREVIOUS ASSEMBLY
						// THIS IS THE ONLY WAY WE CAN GET 2 ASSEMBLIES ADJACENT
						// WITHOUT HAVING TO PLACE A SEPARATOR IN-BETWEEN
						if (turbine.componentType().equals(TurbineComponentType.INLINE_PORT)) {
							if (lastAssembly != null)
								makeAssembly = true;
						}

						if (makeAssembly) {
							// ELSE, CREATE A NEW ASSEMBLY OR EXPAND PREVIOUS ONE
							if (lastAssembly == null) {
								lastAssembly = new TurbineAssembly();
								assemblies.add(lastAssembly);
							}
							lastAssembly.positionsInOrder.add(i);
							te.assembly = lastAssembly;
							if (turbine.componentType().equals(TurbineComponentType.BLADES)) {
								lastAssembly.bladeCount++;
								lastAssembly.size = turbine.size();
							}

							// CHECK FOR IDENTIFIERS
							if (te instanceof MTComponentPortTE port) {
								if (port.identifier != null) {
									// CHECK IF THERE WAS ALREADY A COMPONENT WITH IDENTIFIER
									if (lastAssembly.typeIn != null) {
										// IF SO, CHECK IF THE IDENTIFIER MATCHES
										// previousPortPos IS ALWAYS NOT NULL HERE BECAUSE
										// WHEN typeIn IS SET previousPortPos IS ALSO SET AND THUS
										// IMPOSSIBLE TO REACH HERE WITH IT BEING NULL

										// JUST SAYIN' BECAUSE I'M DUMB
										if (!lastAssembly.typeIn.equals(port.identifier) || lastAssembly.decompress != port.decompress)
											return new ReturnCodeError(previousPortPos,offs,AssemblyErrorReason.IDENTIFIER_MISMATCH);
									}
									lastAssembly.typeIn = port.identifier;
									lastAssembly.decompress = port.decompress;
									previousPortPos = offs;

									// SET LEAST/MOST COMPRESSION FOR LATER USE
									FluidType nextSteam = getNextSteam(port.identifier,port.decompress);
									if (nextSteam == null) {
										if (port.decompress)
											return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.CANT_DECOMPRESS);
										else
											return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.INVALID_IDENTIFIER);
									}
									int steamIndex = steamTypes.indexOf(port.identifier);
									int nextSteamIndex = steamTypes.indexOf(nextSteam);
									if (steamIndex == -1)
										return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.INVALID_IDENTIFIER);
									if (mostCompression == -1 || nextSteamIndex < mostCompression)
										mostCompression = nextSteamIndex;
									if (leastCompression == -1 || steamIndex > leastCompression)
										leastCompression = steamIndex;
								}
							}
						}
						if (isSeparator) {
							// SEPARATE ASSEMBLY IF IT'S A SEPARATOR
							if (lastAssembly != null) {
								if (lastAssembly.typeIn == null)
									return new ReturnCodeError(pos.offset(dir,lastAssembly.positionsInOrder.get(0)+1),prevPos,AssemblyErrorReason.NO_IDENTIFIER);

								if (lastAssembly.bladeCount == 0)
									return new ReturnCodeError(pos.offset(dir,lastAssembly.positionsInOrder.get(0)+1),prevPos,AssemblyErrorReason.NO_BLADES);
							}
							lastAssembly = null;
						}
						// DON'T REPLACE THIS WITH isSeparator VARIABLE AS IT GETS
						// OVERRIDDEN ON INLINE_PORT TYPE
						wasSeparator = turbine.componentType().isSeparator;
					} else
						return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.BUG);
				}
			} else {
				if (!wasSeparator)
					return new ReturnCodeError(prevPos,offs,AssemblyErrorReason.OPEN_END);
				if (lastAssembly != null) {
					if (lastAssembly.typeIn == null)
						return new ReturnCodeError(pos.offset(dir,lastAssembly.positionsInOrder.get(0)+1),offs,AssemblyErrorReason.NO_IDENTIFIER);

					if (lastAssembly.bladeCount == 0)
						return new ReturnCodeError(pos.offset(dir,lastAssembly.positionsInOrder.get(0)+1),prevPos,AssemblyErrorReason.NO_BLADES);
				}
				break;
			};
			prevPos = offs;
			i++;
		}
		length = i;
		// CALCULATE BUFFERS
		int compression = leastCompression-mostCompression;
		for (TurbineAssembly assembly : assemblies) {
			// temporary values
			FluidType nextSteam = getNextSteam(assembly.typeIn,assembly.decompress);
			assembly.input = new FluidTankNTM(assembly.typeIn,calculateBufferSize(assembly.typeIn,mostCompression,assembly.bladeCount,assembly.size));
			assembly.output = new FluidTankNTM(nextSteam,calculateBufferSize(nextSteam,mostCompression,assembly.bladeCount*4,assembly.size));
		}
		return new ReturnCodeSuccess();
	}
	public int calculateBufferSize(FluidType fluid,int mostCompression,int blades,int size) {
		// mostCompression IS A SMALLER VALUE BECAUSE THE LOWER THE INDEX IS, THE MORE COMPRESSED THE STEAM IS
		int index = steamTypes.indexOf(fluid);
		int delta = index-mostCompression;
		double buffer = Math.pow(10,delta)/Math.pow(4,delta)*blades*Math.pow(8,(size-1)/2d)*(4500d/steamTypes.get(mostCompression).temperature);
		if (fluid.equals(Fluids.SPENTSTEAM))
			buffer *= 10;
		return (int)buffer;
	}
}
