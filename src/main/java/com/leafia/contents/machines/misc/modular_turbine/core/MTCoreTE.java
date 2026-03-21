package com.leafia.contents.machines.misc.modular_turbine.core;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
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

import java.util.*;
import java.util.Map.Entry;

public class MTCoreTE extends TileEntity implements LeafiaPacketReceiver, ITickable {
	// Work fraction extracted by a non-decompressing stage
	private static final double PARTIAL_EXPANSION_FRACTION = 0.2D;
	// Converts stage-specific work into an effective nozzle speed
	private static final double NOZZLE_SPEED_COEFFICIENT = 140D;
	private static final double NOZZLE_VELOCITY_COEFFICIENT = 0.92D;
	private static final double INLET_WHIRL_FRACTION = 0.97D;
	private static final double RELATIVE_EXIT_VELOCITY_FRACTION = 0.82D;
	private static final double EXIT_WHIRL_FRACTION = 1D;
	private static final double OPTIMAL_SPEED_RATIO = 0.5D;
	// Converts the angular-momentum proxy into the rotor torque scale
	private static final double ANGULAR_MOMENTUM_TORQUE_COEFFICIENT = 0.02D;
	private static final double GENERATOR_EMF_COEFFICIENT = 0.12D;
	private static final double GENERATOR_TOTAL_RESISTANCE = 1.8D;
	private static final double GENERATOR_CURRENT_LIMIT = 16D;
	private static final double GENERATOR_TORQUE_COEFFICIENT = 0.12D;
	private static final double COULOMB_FRICTION_TORQUE = 0.35D;
	private static final double FRICTION_RPS_EPSILON = 0.25D;
	private static final double VISCOUS_FRICTION_COEFFICIENT = 0.02D;
	private static final double WINDAGE_COEFFICIENT = 0.0015D;
	private static final double POWER_SCALE = 500D;
	private static final double ADMISSION_NOMINAL_TAU_TICKS = 4D;
	private static final double ADMISSION_NOMINAL_RESPONSE = 1D-Math.exp(-1D/ADMISSION_NOMINAL_TAU_TICKS);
	private static final double ADMISSION_STABLE_RELATIVE_ERROR = 0.03D;
	private static final double ADMISSION_MAX_BUFFER_TICKS = 2D;
	private static final double ADMISSION_FLOW_EPSILON = 1.0E-9D;
	private static final double ADMISSION_BUFFER_EPSILON = 1.0E-6D;
	private static final double TWO_PI = Math.PI*2D;
	private static final double TICK_SECONDS = 1D/20D;

	public double rps;
	public double lastTargetRPS = 0;
	public double lastDriveTorque = 0;
	public double lastGeneratorTorque = 0;
	public double lastFrictionTorque = 0;
	public double lastWindageTorque = 0;
	public final List<ModularTurbineComponentTE> components = new ArrayList<>();
	public final List<TurbineAssembly> assemblies = new ArrayList<>();
	public int length = 0;
	// Rotor inertia scalar
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
	public static FT_Coolable getSteamExpansion(FluidType type) {
		if (type.hasTrait(FT_Coolable.class))
			return type.getTrait(FT_Coolable.class);
		return null;
	}
	public static double getSteamMassEquivalent(FluidType type) {
		if (type.equals(Fluids.SPENTSTEAM))
			return 1;
		FT_Coolable expansion = getSteamExpansion(type);
		if (expansion == null)
			return 0;
		return getSteamMassEquivalent(expansion.coolsTo)*expansion.amountProduced/(double)expansion.amountReq;
	}
	public static double getTurbineStepWork(FluidType type) {
		FT_Coolable expansion = getSteamExpansion(type);
		if (expansion == null)
			return 0;
		return expansion.heatEnergy*expansion.getEfficiency(FT_Coolable.CoolingType.TURBINE)/(double)expansion.amountReq;
	}
	public static double getSteamRemainingWork(FluidType type) {
		FT_Coolable expansion = getSteamExpansion(type);
		if (expansion == null)
			return 0;
		return getTurbineStepWork(type)+getSteamRemainingWork(expansion.coolsTo)*expansion.amountProduced/(double)expansion.amountReq;
	}
	public static double getSteamSpecificRemainingWork(FluidType type) {
		return getSteamRemainingWork(type)/getSteamMassEquivalent(type);
	}
	public static double getStageSpecificWork(FluidType type,boolean decompress) {
		double specificWork = getSteamSpecificRemainingWork(type);
		if (!decompress)
			return specificWork*PARTIAL_EXPANSION_FRACTION;
		FluidType nextSteam = getNextSteam(type,true);
		if (nextSteam == null)
			return specificWork;
		return Math.max(specificWork-getSteamSpecificRemainingWork(nextSteam),0);
	}
	public static double getStageRadius(int size) {
		return Math.max(size,2)*0.35D;
	}
	private static double getEffectiveMassFlow(TurbineAssembly assembly,double rawMassFlow) {
		if (assembly.nominalMassFlow <= ADMISSION_FLOW_EPSILON && assembly.admissionBufferMass <= ADMISSION_BUFFER_EPSILON) {
			assembly.nominalMassFlow = rawMassFlow;
			assembly.admissionBufferMass = 0;
			return rawMassFlow;
		}

		assembly.nominalMassFlow += (rawMassFlow-assembly.nominalMassFlow)*ADMISSION_NOMINAL_RESPONSE;
		double stableBand = Math.max(assembly.nominalMassFlow*ADMISSION_STABLE_RELATIVE_ERROR,ADMISSION_FLOW_EPSILON);
		double maxBufferMass = Math.max(assembly.nominalMassFlow,0)*ADMISSION_MAX_BUFFER_TICKS;
		if (Math.abs(rawMassFlow-assembly.nominalMassFlow) <= stableBand && assembly.admissionBufferMass <= ADMISSION_BUFFER_EPSILON) {
			assembly.admissionBufferMass = 0;
			return rawMassFlow;
		}

		double availableMass = rawMassFlow+assembly.admissionBufferMass;
		double effectiveMassFlow = Math.min(assembly.nominalMassFlow,availableMass);
		assembly.admissionBufferMass = Math.max(availableMass-effectiveMassFlow,0);
		if (assembly.admissionBufferMass > maxBufferMass) {
			double overflowMass = assembly.admissionBufferMass-maxBufferMass;
			effectiveMassFlow += overflowMass;
			assembly.admissionBufferMass = maxBufferMass;
		}
		if (assembly.admissionBufferMass <= ADMISSION_BUFFER_EPSILON && Math.abs(effectiveMassFlow-rawMassFlow) <= stableBand) {
			assembly.admissionBufferMass = 0;
			return rawMassFlow;
		}
		return effectiveMassFlow;
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
				posList.appendTag(new NBTTagShort(p.shortValue()));
			tag.setTag("positions",posList);
			NBTTagList bladeMap = new NBTTagList();
			for (Entry<Integer,Boolean> entry : assembly.bladeDirections.entrySet()) {
				NBTTagCompound e = new NBTTagCompound();
				e.setShort("pos",entry.getKey().shortValue());
				e.setBoolean("opposite",entry.getValue());
			}
			tag.setTag("bladeMap",bladeMap);
			assembly.input.writeToNBT(tag,"input");
			assembly.output.writeToNBT(tag,"output");
			tag.setString("typeIn",assembly.typeIn.getName());
			tag.setBoolean("decompress",assembly.decompress);
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
			double targetRPSWeight = 0;
			double driveTorque = 0;
			double generatorTorque = 0;
			double frictionTorque = 0;
			double windageTorque = 0;
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
					assembly.lastOps = ops;
					assembly.lastDivision = division;

					// STEAM FLOW DIRECTION CHECK (BLADE DIRECTIONS AND PORT COUNT)
					int wrongBlades = 0;
					if (!assembly.receivingPositions.isEmpty()) {
						int startIndex = assembly.positionsInOrder.get(0);
						int endIndex = assembly.positionsInOrder.get(assembly.positionsInOrder.size()-1);
						// CHECK IF THERE ARE 2+ INPUT PORTS
						// IF SO, DETECT ALL BLADES PLACED BETWEEN THE FIRST AND LAST PORTS AS WRONG
						int flowCounterStart = assembly.receivingPositions.size();
						int flowCounter = flowCounterStart;
						boolean shouldBeOpposite = true;
						for (int i = startIndex; i <= endIndex; i++) {
							// IF IT'S AN INPUT PORT
							if (assembly.receivingPositions.contains(i)) {
								flowCounter--;
								shouldBeOpposite = false;
							}
							// IF IT'S BLADES
							if (assembly.bladeDirections.containsKey(i)) {
								boolean isOpposite = assembly.bladeDirections.get(i);
								if (shouldBeOpposite != isOpposite || (flowCounter > 0 && flowCounter < flowCounterStart)) {
									wrongBlades++;
									EnumFacing dir = EnumFacing.byIndex(getBlockMetadata()-10).getOpposite();
									LeafiaDebug.debugPos(
											world,
											new BlockPos(pos).offset(dir,i+1),
											0.05f,0xFF0000,
											"BAD BLADE",
											"FLOWCOUNTER: "+flowCounter+"/"+flowCounterStart
									);
								}
							}
						}
						assembly.receivingPositions.clear();
					}
					assembly.lastWrongBlades = wrongBlades;

					// ADD TURBULENCE (FOR BADLY PLACED BLADES)
					turbulence = Math.min(turbulence+wrongBlades*10d/assembly.bladeDirections.size(),100);

					// TORQUE CALCULATION
					double bladeEfficiency = Math.max(assembly.bladeDirections.size()-wrongBlades,0)/(double)assembly.bladeDirections.size();
					assembly.lastBladeEfficiency = bladeEfficiency;
					double stageSpecificWork = getStageSpecificWork(assembly.typeIn,assembly.decompress);
					double rawMassFlow = ops*getSteamMassEquivalent(assembly.typeIn);
					double massFlow = getEffectiveMassFlow(assembly,rawMassFlow);
					assembly.lastRawMassFlow = rawMassFlow;
					assembly.lastEffectiveMassFlow = massFlow;
					assembly.lastAdmissionBufferMass = assembly.admissionBufferMass;
					assembly.lastNominalMassFlow = assembly.nominalMassFlow;
					double stageRadius = getStageRadius(assembly.size);
					double availableWork = massFlow*stageSpecificWork*bladeEfficiency;
					if (massFlow > 0 && stageSpecificWork > 0) {
						double nozzleSpeed = NOZZLE_VELOCITY_COEFFICIENT*Math.sqrt(2*Math.max(stageSpecificWork,0))*NOZZLE_SPEED_COEFFICIENT;
						double inletWhirl = nozzleSpeed*INLET_WHIRL_FRACTION;
						double stageTargetRPS = inletWhirl*OPTIMAL_SPEED_RATIO/(Math.PI*2*stageRadius);
						targetRPS += stageTargetRPS*availableWork;
						targetRPSWeight += availableWork;

						double omega = rps*Math.PI*2;
						double bladeSpeed = omega*stageRadius;
						double relativeInletVelocity = inletWhirl-bladeSpeed;
						double relativeExitVelocity = -RELATIVE_EXIT_VELOCITY_FRACTION*relativeInletVelocity;
						double outletWhirl = bladeSpeed+relativeExitVelocity*EXIT_WHIRL_FRACTION;
						double stageTorque = massFlow*stageRadius*(inletWhirl-outletWhirl)*bladeEfficiency*ANGULAR_MOMENTUM_TORQUE_COEFFICIENT;
						driveTorque += stageTorque;
					}
				}
				if (targetRPSWeight > 0)
					targetRPS /= targetRPSWeight;
				double omega = rps*TWO_PI;
				double generatorEmf = GENERATOR_EMF_COEFFICIENT*omega;
				double generatorCurrent = generatorEmf/GENERATOR_TOTAL_RESISTANCE;
				generatorCurrent = Math.min(Math.max(generatorCurrent,-GENERATOR_CURRENT_LIMIT),GENERATOR_CURRENT_LIMIT);
				generatorTorque = GENERATOR_TORQUE_COEFFICIENT*generatorCurrent;
				frictionTorque = COULOMB_FRICTION_TORQUE*Math.tanh(rps/FRICTION_RPS_EPSILON)+VISCOUS_FRICTION_COEFFICIENT*rps;
				windageTorque = WINDAGE_COEFFICIENT*rps*Math.abs(rps);

				double netTorque = driveTorque-generatorTorque-frictionTorque-windageTorque;
				omega += netTorque/weight*TICK_SECONDS;
				rps = omega/TWO_PI;

				double outputOmega = rps*TWO_PI;
				powerGenerated += (long)Math.max(generatorTorque*outputOmega*POWER_SCALE,0);
			}
			// ADD TURBULENCE (FOR SUDDEN INCREASE OF RPS)
			double turbulenceAdd = Math.pow(Math.max(targetRPS-rps,0)/110,7.2)/Math.max(8,60-turbulence*2);
			turbulence *= 0.99;
			turbulence = Math.min(turbulence+turbulenceAdd,100);
			turbulence = 0; // removed temporarily because it's annoying to test
			lastTargetRPS = targetRPS;
			lastDriveTorque = driveTorque;
			lastGeneratorTorque = generatorTorque;
			lastFrictionTorque = frictionTorque;
			lastWindageTorque = windageTorque;
			//LeafiaDebug.debugLog(world,"TURBULENCE: "+turbulence);
			LeafiaDebug.debugLog(world,"RPS: "+rps);
			LeafiaDebug.debugLog(world,"powerGenerated: "+SIPfx.auto(powerGenerated*20)+"HE/s");
			LeafiaDebug.debugLog(world,"Drive torque: "+driveTorque);
			LeafiaDebug.debugLog(world,"Generator torque: "+generatorTorque);
			LeafiaDebug.debugLog(world,"Friction torque: "+frictionTorque);
			LeafiaDebug.debugLog(world,"Windage torque: "+windageTorque);
			LeafiaDebug.debugLog(world,"TargetRPS-RPS difference: "+(targetRPS-rps));
			LeafiaDebug.debugLog(world,"Turbulence: "+turbulence);
			LeafiaDebug.debugLog(world,"Weight: "+weight+" WU");

			NBTTagCompound syncCompound = new NBTTagCompound();
			NBTTagList syncList = new NBTTagList();
			for (TurbineAssembly assembly : assemblies) {
				NBTTagCompound tag = new NBTTagCompound();
				assembly.input.writeToNBT(tag,"i");
				assembly.output.writeToNBT(tag,"o");
				NBTTagList flowList = new NBTTagList();
				for (Integer receiving : assembly.receivingPositions)
					flowList.appendTag(new NBTTagShort(receiving.shortValue()));
				tag.setTag("b",flowList);
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
		public int size = -1;
		public Set<Integer> receivingPositions = new HashSet<>();
		public Map<Integer,Boolean> bladeDirections = new HashMap<>(); // false: normal, true: opposite
		public int lastOps = 0;
		public double lastDivision = 1;
		public int lastWrongBlades = 0;
		public double lastBladeEfficiency = 0;
		public double admissionBufferMass = 0;
		public double nominalMassFlow = 0;
		public double lastRawMassFlow = 0;
		public double lastEffectiveMassFlow = 0;
		public double lastAdmissionBufferMass = 0;
		public double lastNominalMassFlow = 0;
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
					EnumFacing partDir = EnumFacing.byIndex(world.getBlockState(core).getValue(BlockDummyable.META)-10).getOpposite();
					if (world.getTileEntity(core) instanceof ModularTurbineComponentTE te) {
						components.add(te);
						te.core = this;

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
								lastAssembly.bladeDirections.put(i,!dir.equals(partDir));
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

								if (lastAssembly.bladeDirections.isEmpty())
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

					if (lastAssembly.bladeDirections.isEmpty())
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
			assembly.input = new FluidTankNTM(assembly.typeIn,calculateBufferSize(assembly.typeIn,mostCompression,assembly.bladeDirections.keySet().size(),assembly.size));
			assembly.output = new FluidTankNTM(nextSteam,calculateBufferSize(nextSteam,mostCompression,assembly.bladeDirections.keySet().size()*4,assembly.size));
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
