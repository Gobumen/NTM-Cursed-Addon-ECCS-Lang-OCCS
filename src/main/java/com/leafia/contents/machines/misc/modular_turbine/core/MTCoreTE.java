package com.leafia.contents.machines.misc.modular_turbine.core;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.leafia.contents.machines.misc.modular_turbine.*;
import com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase.TurbineComponentType;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.AssemblyErrorReason;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.ReturnCodeError;
import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.AssemblyReturnCode.ReturnCodeSuccess;
import com.leafia.contents.machines.misc.modular_turbine.ports.MTComponentPortTE;
import com.leafia.dev.LeafiaDebug;
import com.leafia.dev.LeafiaDebug.Tracker;
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

import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreData.*;

public class MTCoreTE extends TileEntity implements LeafiaPacketReceiver, ITickable {
	public static double PARTIAL_EXPANSION_FRACTION = 0.2D;
	public static double NOZZLE_SPEED_COEFFICIENT = 100D;
	public static double NOZZLE_VELOCITY_COEFFICIENT = 0.8D;
	public static double INLET_WHIRL_FRACTION = 0.4D;
	public static double RELATIVE_EXIT_VELOCITY_FRACTION = 0.82D;
	public static double EXIT_WHIRL_FRACTION = 1D;
	public static double ANGULAR_MOMENTUM_TORQUE_COEFFICIENT = 0.02D;
	public static double GENERATOR_EMF_COEFFICIENT = 5000D;
	public static double GENERATOR_TOTAL_RESISTANCE = 1.8D;
	public static double GENERATOR_CURRENT_LIMIT = Double.MAX_VALUE;
	public static double GENERATOR_TORQUE_COEFFICIENT = 0.012D;
	public static double COULOMB_FRICTION_TORQUE = 0.7D;
	public static double FRICTION_RPS_EPSILON = 0.25D;
	public static double VISCOUS_FRICTION_COEFFICIENT = 0.08D;
	public static double WINDAGE_COEFFICIENT = 0.003D;
	public static double POWER_SCALE = 0.00232478632;
	public static double ADMISSION_NOMINAL_TAU_TICKS = 4D;
	public static double ADMISSION_NOMINAL_RESPONSE = 1D-Math.exp(-1D/ADMISSION_NOMINAL_TAU_TICKS);
	public static double ADMISSION_STABLE_RELATIVE_ERROR = 0.03D;
	// DEATHSTEAM -> ULTRAHOTSTEAM -> SUPERHOTSTEAM -> HOTSTEAM -> STEAM -> SPENTSTEAM stage-work scalars
	public static double DEATHSTEAM_STAGE_WORK_MULTIPLIER = 1D;
	public static double ULTRAHOTSTEAM_STAGE_WORK_MULTIPLIER = 1D;
	public static double SUPERHOTSTEAM_STAGE_WORK_MULTIPLIER = 1D;
	public static double HOTSTEAM_STAGE_WORK_MULTIPLIER = 1D;
	public static double STEAM_STAGE_WORK_MULTIPLIER = 1D;
	public static double BUFFER_CAPACITY_MULTIPLIER = 16D;
	public static double ADMISSION_MAX_BUFFER_TICKS = 2D;
	public static double ADMISSION_FLOW_EPSILON = 1.0E-9D;
	public static double ADMISSION_BUFFER_EPSILON = 1.0E-6D;
	public static double EQUILIBRIUM_RPS_EPSILON = 1.0E-6D;
	public static double EQUILIBRIUM_RPS_LIMIT = 1.0E6D;
	public static int EQUILIBRIUM_SOLVER_ITERATIONS = 48;
	public static double DEFAULT_GLOBAL_GEAR_SCALE = 0.05D;
	public static double GEAR_CONTROLLER_MIN_SCALE = 1D/128D;
	public static double GEAR_CONTROLLER_MAX_SCALE = 2D;
	public static int GEAR_CONTROLLER_SAMPLE_COUNT = 25;
	public static double GEAR_CONTROLLER_RESPONSE = 0.25D;
	public static double ROTOR_INERTIA_SCALE = 0.5D;
	public static double TWO_PI = Math.PI*2D;
	public static double TICK_SECONDS = 1D/20D;
	private static final TunableDomain PARTIAL_EXPANSION_FRACTION_DOMAIN = TunableDomain.additive(0D,1D,-0.15D,0.15D);
	private static final TunableDomain NOZZLE_SPEED_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain NOZZLE_VELOCITY_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,1D,-0.25D,0.25D);
	private static final TunableDomain INLET_WHIRL_FRACTION_DOMAIN = TunableDomain.multiplicative(0D,1D,-0.25D,0.25D);
	private static final TunableDomain RELATIVE_EXIT_VELOCITY_FRACTION_DOMAIN = TunableDomain.multiplicative(0D,1D,-0.25D,0.25D);
	private static final TunableDomain EXIT_WHIRL_FRACTION_DOMAIN = TunableDomain.multiplicative(0D,1D,-0.25D,0.25D);
	private static final TunableDomain ANGULAR_MOMENTUM_TORQUE_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.35D,0.35D);
	private static final TunableDomain GENERATOR_EMF_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain GENERATOR_TOTAL_RESISTANCE_DOMAIN = TunableDomain.multiplicative(ADMISSION_FLOW_EPSILON,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain GENERATOR_CURRENT_LIMIT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.75D,1D);
	private static final TunableDomain GENERATOR_TORQUE_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain COULOMB_FRICTION_TORQUE_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain FRICTION_RPS_EPSILON_DOMAIN = TunableDomain.multiplicative(EQUILIBRIUM_RPS_EPSILON,Double.MAX_VALUE,0D,0D);
	private static final TunableDomain VISCOUS_FRICTION_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain WINDAGE_COEFFICIENT_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,-0.5D,0.5D);
	private static final TunableDomain POWER_SCALE_DOMAIN = TunableDomain.multiplicative(0D,Double.MAX_VALUE,0D,0D);
	private static final TunableDomain ROTOR_INERTIA_SCALE_DOMAIN = TunableDomain.multiplicative(ADMISSION_FLOW_EPSILON,Double.MAX_VALUE,0D,0D);

	public double rps;
	public double lastTargetRPS = 0;
	public double lastDriveTorque = 0;
	public double lastGeneratorTorque = 0;
	public double lastFrictionTorque = 0;
	public double lastWindageTorque = 0;
	public double globalGearScale = DEFAULT_GLOBAL_GEAR_SCALE;
	public double lastGlobalGearScale = DEFAULT_GLOBAL_GEAR_SCALE;
	public final List<ModularTurbineComponentTE> components = new ArrayList<>();
	public final List<TurbineAssembly> assemblies = new ArrayList<>();
	public int length = 0;
	// Rotor inertia scalar
	public double weight = 0;
	private CompiledMachineStats compiledMachineStats;
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
		return getStageSpecificWork(type,decompress,PARTIAL_EXPANSION_FRACTION);
	}
	private static double getStageSpecificWork(FluidType type,boolean decompress,double partialExpansionFraction) {
		double specificWork = getSteamSpecificRemainingWork(type);
		if (!decompress)
			return specificWork*partialExpansionFraction;
		FluidType nextSteam = getNextSteam(type,true);
		if (nextSteam == null)
			return specificWork;
		return Math.max(specificWork-getSteamSpecificRemainingWork(nextSteam),0);
	}
	private static double getStageSpecificWork(TurbineAssembly assembly,CompiledStageStats compiledStats) {
		return getStageSpecificWork(assembly.typeIn,assembly.decompress,compiledStats.partialExpansionFraction)*getStageWorkMultiplier(assembly.typeIn);
	}
	private static double getStageWorkMultiplier(FluidType type) {
		return switch (steamTypes.indexOf(type)) {
			case 0 -> DEATHSTEAM_STAGE_WORK_MULTIPLIER;
			case 1 -> ULTRAHOTSTEAM_STAGE_WORK_MULTIPLIER;
			case 2 -> SUPERHOTSTEAM_STAGE_WORK_MULTIPLIER;
			case 3 -> HOTSTEAM_STAGE_WORK_MULTIPLIER;
			case 4 -> STEAM_STAGE_WORK_MULTIPLIER;
			default -> 1D;
		};
	}
	private static int getStageInputAmount(FluidType type,boolean decompress) {
		if (!decompress)
			return 1;
		FT_Coolable expansion = getSteamExpansion(type);
		if (expansion == null)
			return 0;
		return expansion.amountReq;
	}
	private static int getStageOutputAmount(FluidType type,boolean decompress) {
		if (!decompress)
			return 1;
		FT_Coolable expansion = getSteamExpansion(type);
		if (expansion == null)
			return 0;
		return expansion.amountProduced;
	}
	private static double getStageOutputRatio(FluidType type,boolean decompress) {
		int inputAmount = getStageInputAmount(type,decompress);
		if (inputAmount <= 0)
			return 0;
		return getStageOutputAmount(type,decompress)/(double)inputAmount;
	}
	public static double getStageRadius(int size) {
		return Math.max(size,2)*0.35D;
	}
	private static double getStageInletWhirl(FluidType type,boolean decompress) {
		return getStageInletWhirl(getStageSpecificWork(type,decompress),NOZZLE_SPEED_COEFFICIENT,NOZZLE_VELOCITY_COEFFICIENT,INLET_WHIRL_FRACTION);
	}
	private static double getStageInletWhirl(CompiledStageStats compiledStats) {
		return getStageInletWhirl(compiledStats.stageSpecificWork,compiledStats.nozzleSpeedCoefficient,compiledStats.nozzleVelocityCoefficient,compiledStats.inletWhirlFraction);
	}
	private static double getStageInletWhirl(double stageSpecificWork,double nozzleSpeedCoefficient,double nozzleVelocityCoefficient,double inletWhirlFraction) {
		if (stageSpecificWork <= 0)
			return 0;
		double nozzleSpeed = nozzleVelocityCoefficient*Math.sqrt(2*Math.max(stageSpecificWork,0))*nozzleSpeedCoefficient;
		return nozzleSpeed*inletWhirlFraction;
	}
	private static void assignBaseGearRatios(List<TurbineAssembly> assemblies) {
		double weightedLogSum = 0;
		double totalWeight = 0;
		for (TurbineAssembly assembly : assemblies) {
			CompiledStageStats compiledStats = assembly.compiledStats;
			double inletWhirl = compiledStats.inletWhirl;
			double stageRadius = compiledStats.stageRadius;
			if (inletWhirl <= 0 || stageRadius <= 0) {
				assembly.baseGearRatio = 1;
				compiledStats.baseGearRatio = 1;
				continue;
			}
			double speedIndex = inletWhirl/stageRadius;
			double authorityWeight = compiledStats.authorityWeight;
			weightedLogSum += authorityWeight*Math.log(speedIndex);
			totalWeight += authorityWeight;
		}
		if (totalWeight <= 0) {
			for (TurbineAssembly assembly : assemblies) {
				assembly.baseGearRatio = 1;
				assembly.compiledStats.baseGearRatio = 1;
			}
			return;
		}
		double referenceSpeedIndex = Math.exp(weightedLogSum/totalWeight);
		for (TurbineAssembly assembly : assemblies) {
			CompiledStageStats compiledStats = assembly.compiledStats;
			double inletWhirl = compiledStats.inletWhirl;
			double stageRadius = compiledStats.stageRadius;
			if (inletWhirl <= 0 || stageRadius <= 0) {
				assembly.baseGearRatio = 1;
				compiledStats.baseGearRatio = 1;
				continue;
			}
			double speedIndex = inletWhirl/stageRadius;
			assembly.baseGearRatio = speedIndex/referenceSpeedIndex;
			compiledStats.baseGearRatio = assembly.baseGearRatio;
		}
	}
	private static double getEffectiveMassFlow(TurbineAssembly assembly,CompiledStageStats compiledStats,double rawMassFlow) {
		double flowCapacity = compiledStats.flowCapacity;
		double cappedRawMassFlow = Math.min(rawMassFlow,flowCapacity);
		if (assembly.nominalMassFlow <= ADMISSION_FLOW_EPSILON && assembly.admissionBufferMass <= ADMISSION_BUFFER_EPSILON) {
			assembly.nominalMassFlow = cappedRawMassFlow;
			assembly.admissionBufferMass = Math.max(rawMassFlow-cappedRawMassFlow,0);
			if (assembly.admissionBufferMass <= ADMISSION_BUFFER_EPSILON)
				assembly.admissionBufferMass = 0;
			return cappedRawMassFlow;
		}

		assembly.nominalMassFlow += (rawMassFlow-assembly.nominalMassFlow)*ADMISSION_NOMINAL_RESPONSE;
		assembly.nominalMassFlow = Math.min(assembly.nominalMassFlow,flowCapacity);
		double stableBand = Math.max(assembly.nominalMassFlow*ADMISSION_STABLE_RELATIVE_ERROR,ADMISSION_FLOW_EPSILON);
		double maxBufferMass = Math.max(assembly.nominalMassFlow,0)*ADMISSION_MAX_BUFFER_TICKS;
		if (Math.abs(rawMassFlow-assembly.nominalMassFlow) <= stableBand && assembly.admissionBufferMass <= ADMISSION_BUFFER_EPSILON) {
			assembly.admissionBufferMass = 0;
			return cappedRawMassFlow;
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
			return cappedRawMassFlow;
		}
		return effectiveMassFlow;
	}
	private static double getGeneratorTorqueAtOmega(CompiledMachineStats machineStats,double omega) {
		double generatorEmf = machineStats.generatorEmfCoefficient*omega;
		double generatorCurrent = generatorEmf/machineStats.generatorTotalResistance;
		generatorCurrent = Math.min(Math.max(generatorCurrent,-machineStats.generatorCurrentLimit),machineStats.generatorCurrentLimit);
		return machineStats.generatorTorqueCoefficient*generatorCurrent;
	}
	private static double getFrictionTorqueAtRPS(CompiledMachineStats machineStats,double rps) {
		return machineStats.coulombFrictionTorque*Math.tanh(rps/machineStats.frictionRpsEpsilon)+machineStats.viscousFrictionCoefficient*rps;
	}
	private static double getWindageTorqueAtRPS(CompiledMachineStats machineStats,double rps) {
		return machineStats.windageCoefficient*rps*Math.abs(rps);
	}
	private static double getNetTorqueAtRPS(CompiledMachineStats machineStats,double rps,double driveTorqueIntercept,double driveTorqueOmegaSlope) {
		double omega = rps*TWO_PI;
		double driveTorque = driveTorqueIntercept-driveTorqueOmegaSlope*omega;
		return driveTorque-getGeneratorTorqueAtOmega(machineStats,omega)-getFrictionTorqueAtRPS(machineStats,rps)-getWindageTorqueAtRPS(machineStats,rps);
	}
	private static double solveEquilibriumRPS(CompiledMachineStats machineStats,double driveTorqueIntercept,double driveTorqueOmegaSlope) {
		double zeroNetTorque = getNetTorqueAtRPS(machineStats,0,driveTorqueIntercept,driveTorqueOmegaSlope);
		if (zeroNetTorque <= 0)
			return 0;

		double linearNoLoadRPS = 0;
		if (driveTorqueOmegaSlope > EQUILIBRIUM_RPS_EPSILON)
			linearNoLoadRPS = driveTorqueIntercept/(driveTorqueOmegaSlope*TWO_PI);
		double low = 0;
		double high = Math.max(linearNoLoadRPS,1);
		double highNetTorque = getNetTorqueAtRPS(machineStats,high,driveTorqueIntercept,driveTorqueOmegaSlope);
		while (highNetTorque > 0 && high < EQUILIBRIUM_RPS_LIMIT) {
			low = high;
			high *= 2;
			highNetTorque = getNetTorqueAtRPS(machineStats,high,driveTorqueIntercept,driveTorqueOmegaSlope);
		}
		if (highNetTorque > 0)
			return high;
		for (int i = 0; i < EQUILIBRIUM_SOLVER_ITERATIONS; i++) {
			double mid = (low+high)/2;
			double midNetTorque = getNetTorqueAtRPS(machineStats,mid,driveTorqueIntercept,driveTorqueOmegaSlope);
			if (midNetTorque > 0)
				low = mid;
			else
				high = mid;
		}
		return (low+high)/2;
	}
	private static double predictedPowerForGearScale(CompiledMachineStats machineStats,double globalGearScale,double baseDriveIntercept,double baseDriveOmegaSlope) {
		double driveTorqueIntercept = baseDriveIntercept*globalGearScale;
		double driveTorqueOmegaSlope = baseDriveOmegaSlope*globalGearScale*globalGearScale;
		double equilibriumRPS = solveEquilibriumRPS(machineStats,driveTorqueIntercept,driveTorqueOmegaSlope);
		double equilibriumOmega = equilibriumRPS*TWO_PI;
		double generatorTorque = getGeneratorTorqueAtOmega(machineStats,equilibriumOmega);
		return Math.max(generatorTorque*equilibriumOmega*machineStats.powerScale*20D,0);
	}
	private static double optimizeInstantaneousGearScale(CompiledMachineStats machineStats,double baseDriveIntercept,double baseDriveOmegaSlope) {
		double bestScale = GEAR_CONTROLLER_MIN_SCALE;
		double bestPower = Double.NEGATIVE_INFINITY;
		double minLog2Scale = Math.log(GEAR_CONTROLLER_MIN_SCALE)/Math.log(2);
		double maxLog2Scale = Math.log(GEAR_CONTROLLER_MAX_SCALE)/Math.log(2);
		for (int i = 0; i < GEAR_CONTROLLER_SAMPLE_COUNT; i++) {
			double t = GEAR_CONTROLLER_SAMPLE_COUNT <= 1 ? 0 : i/(double)(GEAR_CONTROLLER_SAMPLE_COUNT-1);
			double log2Scale = minLog2Scale+(maxLog2Scale-minLog2Scale)*t;
			double scale = Math.pow(2,log2Scale);
			double power = predictedPowerForGearScale(machineStats,scale,baseDriveIntercept,baseDriveOmegaSlope);
			if (power > bestPower) {
				bestPower = power;
				bestScale = scale;
			}
		}
		return bestScale;
	}
	private static int requireTankCapacity(long bufferSize,FluidType fluid) {
		if (bufferSize > Integer.MAX_VALUE)
			throw new IllegalStateException("Buffer size exceeds FluidTankNTM capacity for "+fluid.getName()+": "+bufferSize);
		return (int)bufferSize;
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
		globalGearScale = compound.hasKey("globalGearScale") ? compound.getDouble("globalGearScale") : DEFAULT_GLOBAL_GEAR_SCALE;
		EnumFacing dir = getAssemblyFacing();
		Map<Integer,TurbineAssembly> assemblyMap = new HashMap<>();
		boolean missingGearRatio = false;

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
				for (NBTBase nbtBase1 : tag.getTagList("bladeMap",10)) {
					if (nbtBase1 instanceof NBTTagCompound bladeTag)
						assembly.bladeDirections.put((int)bladeTag.getShort("pos"),bladeTag.getBoolean("opposite"));
					else
						return false;
				}
				assembly.input = new FluidTankNTM(Fluids.NONE,0);
				assembly.output = new FluidTankNTM(Fluids.NONE,0);
				assembly.input.readFromNBT(tag,"input");
				assembly.output.readFromNBT(tag,"output");
				assembly.typeIn = Fluids.fromName(tag.getString("typeIn"));
				assembly.decompress = tag.getBoolean("decompress");
				assembly.size = tag.getByte("size");
				if (tag.hasKey("gearRatio"))
					assembly.baseGearRatio = tag.getDouble("gearRatio");
				else
					missingGearRatio = true;
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
		compileAssemblyModel(missingGearRatio);
		return true;
	}
	public NBTTagCompound writeAssemblyToNBT(NBTTagCompound compound) {
		compound.setInteger("length",length);
		compound.setDouble("weight",weight);
		compound.setDouble("globalGearScale",globalGearScale);
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
				bladeMap.appendTag(e);
			}
			tag.setTag("bladeMap",bladeMap);
			assembly.input.writeToNBT(tag,"input");
			assembly.output.writeToNBT(tag,"output");
			tag.setString("typeIn",assembly.typeIn.getName());
			tag.setBoolean("decompress",assembly.decompress);
			tag.setByte("size",(byte)assembly.size);
			tag.setDouble("gearRatio",assembly.baseGearRatio);
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
	private EnumFacing getAssemblyFacing() {
		return EnumFacing.byIndex(getBlockMetadata()-10).getOpposite();
	}
	private CompiledMachineStats getCompiledMachineStats() {
		if (compiledMachineStats == null)
			compiledMachineStats = compileMachineStats(summarizeMachineUpgrades());
		return compiledMachineStats;
	}
	private void compileAssemblyModel(boolean recomputeGearRatios) {
		compiledMachineStats = compileMachineStats(summarizeMachineUpgrades());
		for (TurbineAssembly assembly : assemblies)
			assembly.compiledStats = compileStageStats(assembly,summarizeStageUpgrades(assembly));
		if (recomputeGearRatios)
			assignBaseGearRatios(assemblies);
		else
			for (TurbineAssembly assembly : assemblies)
				assembly.compiledStats.baseGearRatio = assembly.baseGearRatio;
	}
	private MachineUpgradeSummary summarizeMachineUpgrades() {
		MachineUpgradeSummary summary = new MachineUpgradeSummary();
		for (ModularTurbineComponentTE component : components)
			collectMachineUpgradeOffsets(summary,component);
		return summary;
	}
	private StageUpgradeSummary summarizeStageUpgrades(TurbineAssembly assembly) {
		StageUpgradeSummary summary = new StageUpgradeSummary();
		for (ModularTurbineComponentTE component : components) {
			if (component.assembly != assembly)
				continue;
			collectStageUpgradeOffsets(summary,assembly,component);
		}
		return summary;
	}
	private void collectMachineUpgradeOffsets(MachineUpgradeSummary summary,ModularTurbineComponentTE component) {
		component.contributeMachineUpgradeOffsets(summary,this,component);
		((IMTMachineUpgradeContributor)world.getBlockState(component.getPos()).getBlock()).contributeMachineUpgradeOffsets(summary,this,component);
	}
	private void collectStageUpgradeOffsets(StageUpgradeSummary summary,TurbineAssembly assembly,ModularTurbineComponentTE component) {
		component.contributeStageUpgradeOffsets(summary,this,assembly,component);
		((IMTStageUpgradeContributor) world.getBlockState(component.getPos()).getBlock()).contributeStageUpgradeOffsets(summary,this,assembly,component);
	}
	private CompiledMachineStats compileMachineStats(MachineUpgradeSummary upgrades) {
		CompiledMachineStats machineStats = new CompiledMachineStats();
		machineStats.generatorEmfCoefficient = GENERATOR_EMF_COEFFICIENT_DOMAIN.resolve(GENERATOR_EMF_COEFFICIENT,upgrades.getGeneratorEmfCoefficientOffset());
		machineStats.generatorTotalResistance = GENERATOR_TOTAL_RESISTANCE_DOMAIN.resolve(GENERATOR_TOTAL_RESISTANCE,upgrades.getGeneratorTotalResistanceOffset());
		machineStats.generatorCurrentLimit = GENERATOR_CURRENT_LIMIT_DOMAIN.resolve(GENERATOR_CURRENT_LIMIT,upgrades.getGeneratorCurrentLimitOffset());
		machineStats.generatorTorqueCoefficient = GENERATOR_TORQUE_COEFFICIENT_DOMAIN.resolve(GENERATOR_TORQUE_COEFFICIENT,upgrades.getGeneratorTorqueCoefficientOffset());
		machineStats.coulombFrictionTorque = COULOMB_FRICTION_TORQUE_DOMAIN.resolve(COULOMB_FRICTION_TORQUE,upgrades.getCoulombFrictionTorqueOffset());
		machineStats.frictionRpsEpsilon = FRICTION_RPS_EPSILON_DOMAIN.resolve(FRICTION_RPS_EPSILON,upgrades.getFrictionRpsEpsilonOffset());
		machineStats.viscousFrictionCoefficient = VISCOUS_FRICTION_COEFFICIENT_DOMAIN.resolve(VISCOUS_FRICTION_COEFFICIENT,upgrades.getViscousFrictionCoefficientOffset());
		machineStats.windageCoefficient = WINDAGE_COEFFICIENT_DOMAIN.resolve(WINDAGE_COEFFICIENT,upgrades.getWindageCoefficientOffset());
		machineStats.powerScale = POWER_SCALE_DOMAIN.resolve(POWER_SCALE,upgrades.getPowerScaleOffset());
		machineStats.rotorInertiaScale = ROTOR_INERTIA_SCALE_DOMAIN.resolve(ROTOR_INERTIA_SCALE,upgrades.getRotorInertiaScaleOffset());
		return machineStats;
	}
	private CompiledStageStats compileStageStats(TurbineAssembly assembly,StageUpgradeSummary upgrades) {
		CompiledStageStats compiledStats = new CompiledStageStats();
		compiledStats.partialExpansionFraction = PARTIAL_EXPANSION_FRACTION_DOMAIN.resolve(PARTIAL_EXPANSION_FRACTION,upgrades.getPartialExpansionFractionOffset());
		compiledStats.nozzleSpeedCoefficient = NOZZLE_SPEED_COEFFICIENT_DOMAIN.resolve(NOZZLE_SPEED_COEFFICIENT,upgrades.getNozzleSpeedCoefficientOffset());
		compiledStats.nozzleVelocityCoefficient = NOZZLE_VELOCITY_COEFFICIENT_DOMAIN.resolve(NOZZLE_VELOCITY_COEFFICIENT,upgrades.getNozzleVelocityCoefficientOffset());
		compiledStats.inletWhirlFraction = INLET_WHIRL_FRACTION_DOMAIN.resolve(INLET_WHIRL_FRACTION,upgrades.getInletWhirlFractionOffset());
		compiledStats.relativeExitVelocityFraction = RELATIVE_EXIT_VELOCITY_FRACTION_DOMAIN.resolve(RELATIVE_EXIT_VELOCITY_FRACTION,upgrades.getRelativeExitVelocityFractionOffset());
		compiledStats.exitWhirlFraction = EXIT_WHIRL_FRACTION_DOMAIN.resolve(EXIT_WHIRL_FRACTION,upgrades.getExitWhirlFractionOffset());
		compiledStats.angularMomentumTorqueCoefficient = ANGULAR_MOMENTUM_TORQUE_COEFFICIENT_DOMAIN.resolve(ANGULAR_MOMENTUM_TORQUE_COEFFICIENT,upgrades.getAngularMomentumTorqueCoefficientOffset());
		compiledStats.inputAmount = getStageInputAmount(assembly.typeIn,assembly.decompress);
		compiledStats.outputAmount = getStageOutputAmount(assembly.typeIn,assembly.decompress);
		compiledStats.division = getStageOutputRatio(assembly.typeIn,assembly.decompress);
		compiledStats.steamMassEquivalent = getSteamMassEquivalent(assembly.typeIn);
		compiledStats.stageSpecificWork = getStageSpecificWork(assembly,compiledStats);
		compiledStats.stageRadius = getStageRadius(assembly.size);
		compiledStats.inletWhirl = getStageInletWhirl(compiledStats);
		compiledStats.torqueResponseFactor = compiledStats.angularMomentumTorqueCoefficient*(1D+compiledStats.relativeExitVelocityFraction*compiledStats.exitWhirlFraction);
		compiledStats.bladeCount = assembly.bladeDirections.size();
		compiledStats.bladeArea = Math.max(compiledStats.bladeCount+upgrades.getBladeArea(),ADMISSION_FLOW_EPSILON);
		compiledStats.flowCapacity = upgrades.getFlowCapacity() > ADMISSION_FLOW_EPSILON ? upgrades.getFlowCapacity() : Double.POSITIVE_INFINITY;
		compiledStats.authorityWeight = Math.max(compiledStats.bladeArea*compiledStats.steamMassEquivalent,ADMISSION_FLOW_EPSILON);
		compiledStats.baseGearRatio = assembly.baseGearRatio;
		return compiledStats;
	}
	private TickSummary simulateTick() {
		TickSummary tickSummary = new TickSummary();
		if (weight <= 0)
			return tickSummary;
		CompiledMachineStats machineStats = getCompiledMachineStats();
		List<StageRuntimeData> stageData = evaluateStages();
		DriveCurve driveCurve = buildDriveCurve(stageData);
		updateGlobalGearScale(machineStats,driveCurve);
		applyRotationalStep(machineStats,stageData,driveCurve,tickSummary);
		return tickSummary;
	}
	private List<StageRuntimeData> evaluateStages() {
		List<StageRuntimeData> stageData = new ArrayList<>();
		for (TurbineAssembly assembly : assemblies)
			stageData.add(evaluateStage(assembly));
		return stageData;
	}
	private StageRuntimeData evaluateStage(TurbineAssembly assembly) {
		CompiledStageStats compiledStats = assembly.compiledStats;
		int inputConsumed = transferStageFluids(assembly,compiledStats);
		int wrongBlades = countWrongBlades(assembly);
		assembly.lastWrongBlades = wrongBlades;
		applyWrongBladeTurbulence(assembly,wrongBlades);

		double bladeEfficiency = Math.max(compiledStats.bladeCount-wrongBlades,0)/(double)compiledStats.bladeCount;
		assembly.lastBladeEfficiency = bladeEfficiency;

		double rawMassFlow = inputConsumed*compiledStats.steamMassEquivalent;
		double massFlow = getEffectiveMassFlow(assembly,compiledStats,rawMassFlow);
		assembly.lastRawMassFlow = rawMassFlow;
		assembly.lastEffectiveMassFlow = massFlow;
		assembly.lastAdmissionBufferMass = assembly.admissionBufferMass;
		assembly.lastNominalMassFlow = assembly.nominalMassFlow;

		double stageTorqueScale = 0;
		if (massFlow > 0 && compiledStats.stageSpecificWork > 0)
			stageTorqueScale = massFlow*compiledStats.stageRadius*bladeEfficiency*compiledStats.getBladeAreaFactor()*compiledStats.torqueResponseFactor;
		return new StageRuntimeData(assembly,compiledStats,massFlow,stageTorqueScale);
	}
	private int transferStageFluids(TurbineAssembly assembly,CompiledStageStats compiledStats) {
		int inputOps = compiledStats.inputAmount > 0 ? assembly.input.getFill()/compiledStats.inputAmount : 0;
		int outputOps = compiledStats.outputAmount > 0 ? (assembly.output.getMaxFill()-assembly.output.getFill())/compiledStats.outputAmount : 0;
		int ops = Math.min(inputOps,outputOps);
		int inputConsumed = ops*compiledStats.inputAmount;
		int outputProduced = ops*compiledStats.outputAmount;
		assembly.input.setFill(assembly.input.getFill()-inputConsumed);
		assembly.output.setFill(assembly.output.getFill()+outputProduced);
		assembly.lastOps = inputConsumed;
		assembly.lastDivision = compiledStats.division;
		return inputConsumed;
	}
	private int countWrongBlades(TurbineAssembly assembly) {
		int wrongBlades = 0;
		if (!assembly.receivingPositions.isEmpty()) {
			int startIndex = assembly.positionsInOrder.get(0);
			int endIndex = assembly.positionsInOrder.get(assembly.positionsInOrder.size()-1);
			int flowCounterStart = assembly.receivingPositions.size();
			int flowCounter = flowCounterStart;
			boolean shouldBeOpposite = true;
			EnumFacing dir = getAssemblyFacing();
			for (int i = startIndex; i <= endIndex; i++) {
				if (assembly.receivingPositions.contains(i)) {
					flowCounter--;
					shouldBeOpposite = false;
				}
				if (assembly.bladeDirections.containsKey(i)) {
					boolean isOpposite = assembly.bladeDirections.get(i);
					if (shouldBeOpposite != isOpposite || (flowCounter > 0 && flowCounter < flowCounterStart)) {
						wrongBlades++;
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
		return wrongBlades;
	}
	private void applyWrongBladeTurbulence(TurbineAssembly assembly,int wrongBlades) {
		turbulence = Math.min(turbulence+wrongBlades*10d/assembly.compiledStats.bladeArea,100);
	}
	private DriveCurve buildDriveCurve(List<StageRuntimeData> stageData) {
		DriveCurve driveCurve = new DriveCurve();
		for (StageRuntimeData data : stageData) {
			driveCurve.baseDriveTorqueIntercept += data.getBaseDriveTorqueInterceptContribution();
			driveCurve.baseDriveTorqueOmegaSlope += data.getBaseDriveTorqueOmegaSlopeContribution();
		}
		return driveCurve;
	}
	private void updateGlobalGearScale(CompiledMachineStats machineStats,DriveCurve driveCurve) {
		if (driveCurve.hasPositiveDrive()) {
			double targetGearScale = optimizeInstantaneousGearScale(machineStats,driveCurve.baseDriveTorqueIntercept,driveCurve.baseDriveTorqueOmegaSlope);
			globalGearScale += (targetGearScale-globalGearScale)*GEAR_CONTROLLER_RESPONSE;
			globalGearScale = Math.min(Math.max(globalGearScale,GEAR_CONTROLLER_MIN_SCALE),GEAR_CONTROLLER_MAX_SCALE);
		}
	}
	private void applyRotationalStep(CompiledMachineStats machineStats,List<StageRuntimeData> stageData,DriveCurve driveCurve,TickSummary tickSummary) {
		double driveTorqueIntercept = driveCurve.getDriveTorqueIntercept(globalGearScale);
		double driveTorqueOmegaSlope = driveCurve.getDriveTorqueOmegaSlope(globalGearScale);
		tickSummary.targetRPS = solveEquilibriumRPS(machineStats,driveTorqueIntercept,driveTorqueOmegaSlope);

		double omega = rps*TWO_PI;
		for (StageRuntimeData data : stageData)
			tickSummary.driveTorque += data.getDriveTorque(omega,globalGearScale);
		tickSummary.generatorTorque = getGeneratorTorqueAtOmega(machineStats,omega);
		tickSummary.frictionTorque = getFrictionTorqueAtRPS(machineStats,rps);
		tickSummary.windageTorque = getWindageTorqueAtRPS(machineStats,rps);

		double netTorque = tickSummary.driveTorque-tickSummary.generatorTorque-tickSummary.frictionTorque-tickSummary.windageTorque;
		double effectiveWeight = weight*machineStats.rotorInertiaScale;
		omega += netTorque/effectiveWeight*TICK_SECONDS;
		rps = omega/TWO_PI;

		double outputOmega = rps*TWO_PI;
		tickSummary.powerGenerated += (long)Math.max(tickSummary.generatorTorque*outputOmega*machineStats.powerScale,0);
	}
	private void updateTurbulence(TickSummary tickSummary) {
		double turbulenceAdd = Math.pow(Math.max(tickSummary.targetRPS-rps,0)/110,7.2)/Math.max(8,60-turbulence*2);
		turbulence *= 0.99;
		turbulence = Math.min(turbulence+turbulenceAdd,100);
		turbulence = 0; // removed temporarily because it's annoying to test
	}
	private void commitTickSummary(TickSummary tickSummary) {
		lastTargetRPS = tickSummary.targetRPS;
		lastDriveTorque = tickSummary.driveTorque;
		lastGeneratorTorque = tickSummary.generatorTorque;
		lastFrictionTorque = tickSummary.frictionTorque;
		lastWindageTorque = tickSummary.windageTorque;
		lastGlobalGearScale = globalGearScale;
	}
	private void debugTick(TickSummary tickSummary) {
		Tracker._startProfile(this,"update");
		Tracker._tracePosition(this,pos.up(),
				"RPS: "+rps,
				"powerGenerated: "+SIPfx.auto(tickSummary.powerGenerated*20)+"HE/s",
				"Drive torque: "+tickSummary.driveTorque,
				"Generator torque: "+tickSummary.generatorTorque,
				"Friction torque: "+tickSummary.frictionTorque,
				"Windage torque: "+tickSummary.windageTorque,
				"TargetRPS-RPS difference: "+(tickSummary.targetRPS-rps),
				"Turbulence: "+turbulence,
				"Global gear scale: "+globalGearScale,
				"Weight: "+weight+" WU"
		);
		Tracker._endProfile(this);
	}
	private void syncSteamState() {
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
	@Override
	public void update() {
		if (world.isRemote)
			return;
		TickSummary tickSummary = simulateTick();
		updateTurbulence(tickSummary);
		commitTickSummary(tickSummary);
		debugTick(tickSummary);
		syncSteamState();
	}
	public static class TurbineAssembly {
		/// NOTE: The values stored here are actually 1 lower than actual offset
		public List<Integer> positionsInOrder = new ArrayList<>();
		public FluidTankNTM input;
		public FluidTankNTM output;
		public FluidType typeIn;
		public boolean decompress = false;
		public int size = -1;
		public double baseGearRatio = 1;
		private CompiledStageStats compiledStats;
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
		compiledMachineStats = null;
		globalGearScale = DEFAULT_GLOBAL_GEAR_SCALE;
		lastGlobalGearScale = DEFAULT_GLOBAL_GEAR_SCALE;
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
		EnumFacing dir = getAssemblyFacing();
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
			long inputCapacity = calculateBufferSize(assembly.typeIn,mostCompression,assembly.bladeDirections.keySet().size(),assembly.size);
			long outputCapacity = calculateBufferSize(nextSteam,mostCompression,assembly.bladeDirections.keySet().size()*4,assembly.size);
			assembly.input = new FluidTankNTM(assembly.typeIn,requireTankCapacity(inputCapacity,assembly.typeIn));
			assembly.output = new FluidTankNTM(nextSteam,requireTankCapacity(outputCapacity,nextSteam));
		}
		compileAssemblyModel(true);
		globalGearScale = DEFAULT_GLOBAL_GEAR_SCALE;
		return new ReturnCodeSuccess();
	}
	public long calculateBufferSize(FluidType fluid,int mostCompression,int blades,int size) {
		// mostCompression IS A SMALLER VALUE BECAUSE THE LOWER THE INDEX IS, THE MORE COMPRESSED THE STEAM IS
		int index = steamTypes.indexOf(fluid);
		int delta = index-mostCompression;
		double buffer = Math.pow(10,delta)/Math.pow(4,delta)*blades*Math.pow(8,(size-1)/2d)*(4500d/steamTypes.get(mostCompression).temperature)*BUFFER_CAPACITY_MULTIPLIER;
		if (fluid.equals(Fluids.SPENTSTEAM))
			buffer *= 10;
		return (long)buffer;
	}
}
