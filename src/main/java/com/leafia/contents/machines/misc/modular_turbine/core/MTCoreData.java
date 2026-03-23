package com.leafia.contents.machines.misc.modular_turbine.core;

import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.TurbineAssembly;

public final class MTCoreData {
	private MTCoreData() { }

	static final class TunableDomain {
		private final double minResolved;
		private final double maxResolved;
		private final double minOffset;
		private final double maxOffset;
		private final boolean multiplicative;
		private TunableDomain(double minResolved,double maxResolved,double minOffset,double maxOffset,boolean multiplicative) {
			this.minResolved = minResolved;
			this.maxResolved = maxResolved;
			this.minOffset = minOffset;
			this.maxOffset = maxOffset;
			this.multiplicative = multiplicative;
		}
		static TunableDomain additive(double minResolved,double maxResolved,double minOffset,double maxOffset) {
			return new TunableDomain(minResolved,maxResolved,minOffset,maxOffset,false);
		}
		static TunableDomain multiplicative(double minResolved,double maxResolved,double minOffset,double maxOffset) {
			return new TunableDomain(minResolved,maxResolved,minOffset,maxOffset,true);
		}
		double resolve(double baseValue,double offset) {
			double boundedBase = sanitize(baseValue,minResolved,maxResolved);
			double boundedOffset = sanitize(offset,minOffset,maxOffset);
			double resolved = multiplicative ? boundedBase*(1D+boundedOffset) : boundedBase+boundedOffset;
			return sanitize(resolved,minResolved,maxResolved);
		}
		private static double sanitize(double value,double min,double max) {
			if (Double.isNaN(value))
				return min;
			if (value == Double.POSITIVE_INFINITY)
				return max;
			if (value == Double.NEGATIVE_INFINITY)
				return min;
			return Math.min(Math.max(value,min),max);
		}
	}
	public static final class StageUpgradeSummary {
		private double partialExpansionFractionOffset;
		private double nozzleSpeedCoefficientOffset;
		private double nozzleVelocityCoefficientOffset;
		private double inletWhirlFractionOffset;
		private double relativeExitVelocityFractionOffset;
		private double exitWhirlFractionOffset;
		private double angularMomentumTorqueCoefficientOffset;
		private double bladeArea;
		private double flowCapacity;
		public void addPartialExpansionFractionOffset(double offset) {
			partialExpansionFractionOffset += offset;
		}
		public void addNozzleSpeedCoefficientOffset(double offset) {
			nozzleSpeedCoefficientOffset += offset;
		}
		public void addNozzleVelocityCoefficientOffset(double offset) {
			nozzleVelocityCoefficientOffset += offset;
		}
		public void addInletWhirlFractionOffset(double offset) {
			inletWhirlFractionOffset += offset;
		}
		public void addRelativeExitVelocityFractionOffset(double offset) {
			relativeExitVelocityFractionOffset += offset;
		}
		public void addExitWhirlFractionOffset(double offset) {
			exitWhirlFractionOffset += offset;
		}
		public void addAngularMomentumTorqueCoefficientOffset(double offset) {
			angularMomentumTorqueCoefficientOffset += offset;
		}
		public void addBladeArea(double area) {
			bladeArea += area;
		}
		public void addFlowCapacity(double capacity) {
			flowCapacity += capacity;
		}
		double getPartialExpansionFractionOffset() {
			return partialExpansionFractionOffset;
		}
		double getNozzleSpeedCoefficientOffset() {
			return nozzleSpeedCoefficientOffset;
		}
		double getNozzleVelocityCoefficientOffset() {
			return nozzleVelocityCoefficientOffset;
		}
		double getInletWhirlFractionOffset() {
			return inletWhirlFractionOffset;
		}
		double getRelativeExitVelocityFractionOffset() {
			return relativeExitVelocityFractionOffset;
		}
		double getExitWhirlFractionOffset() {
			return exitWhirlFractionOffset;
		}
		double getAngularMomentumTorqueCoefficientOffset() {
			return angularMomentumTorqueCoefficientOffset;
		}
		double getBladeArea() {
			return bladeArea;
		}
		double getFlowCapacity() {
			return flowCapacity;
		}
	}
	public static final class MachineUpgradeSummary {
		private double generatorEmfCoefficientOffset;
		private double generatorTotalResistanceOffset;
		private double generatorCurrentLimitOffset;
		private double generatorTorqueCoefficientOffset;
		private double coulombFrictionTorqueOffset;
		private double frictionRpsEpsilonOffset;
		private double viscousFrictionCoefficientOffset;
		private double windageCoefficientOffset;
		private double powerScaleOffset;
		private double rotorInertiaScaleOffset;
		public void addGeneratorEmfCoefficientOffset(double offset) {
			generatorEmfCoefficientOffset += offset;
		}
		public void addGeneratorTotalResistanceOffset(double offset) {
			generatorTotalResistanceOffset += offset;
		}
		public void addGeneratorCurrentLimitOffset(double offset) {
			generatorCurrentLimitOffset += offset;
		}
		public void addGeneratorTorqueCoefficientOffset(double offset) {
			generatorTorqueCoefficientOffset += offset;
		}
		public void addCoulombFrictionTorqueOffset(double offset) {
			coulombFrictionTorqueOffset += offset;
		}
		public void addFrictionRpsEpsilonOffset(double offset) {
			frictionRpsEpsilonOffset += offset;
		}
		public void addViscousFrictionCoefficientOffset(double offset) {
			viscousFrictionCoefficientOffset += offset;
		}
		public void addWindageCoefficientOffset(double offset) {
			windageCoefficientOffset += offset;
		}
		public void addPowerScaleOffset(double offset) {
			powerScaleOffset += offset;
		}
		public void addRotorInertiaScaleOffset(double offset) {
			rotorInertiaScaleOffset += offset;
		}
		double getGeneratorEmfCoefficientOffset() {
			return generatorEmfCoefficientOffset;
		}
		double getGeneratorTotalResistanceOffset() {
			return generatorTotalResistanceOffset;
		}
		double getGeneratorCurrentLimitOffset() {
			return generatorCurrentLimitOffset;
		}
		double getGeneratorTorqueCoefficientOffset() {
			return generatorTorqueCoefficientOffset;
		}
		double getCoulombFrictionTorqueOffset() {
			return coulombFrictionTorqueOffset;
		}
		double getFrictionRpsEpsilonOffset() {
			return frictionRpsEpsilonOffset;
		}
		double getViscousFrictionCoefficientOffset() {
			return viscousFrictionCoefficientOffset;
		}
		double getWindageCoefficientOffset() {
			return windageCoefficientOffset;
		}
		double getPowerScaleOffset() {
			return powerScaleOffset;
		}
		double getRotorInertiaScaleOffset() {
			return rotorInertiaScaleOffset;
		}
	}
	static final class CompiledMachineStats {
		double generatorEmfCoefficient;
		double generatorTotalResistance;
		double generatorCurrentLimit;
		double generatorTorqueCoefficient;
		double coulombFrictionTorque;
		double frictionRpsEpsilon;
		double viscousFrictionCoefficient;
		double windageCoefficient;
		double powerScale;
		double rotorInertiaScale;
	}
	static final class CompiledStageStats {
		double partialExpansionFraction;
		double nozzleSpeedCoefficient;
		double nozzleVelocityCoefficient;
		double inletWhirlFraction;
		double relativeExitVelocityFraction;
		double exitWhirlFraction;
		double angularMomentumTorqueCoefficient;
		int inputAmount;
		int outputAmount;
		double division;
		double steamMassEquivalent;
		double stageSpecificWork;
		double stageRadius;
		double inletWhirl;
		double torqueResponseFactor;
		double authorityWeight;
		int bladeCount;
		double bladeArea;
		double flowCapacity;
		double baseGearRatio = 1;
		double getActualGearRatio(double globalGearScale) {
			return baseGearRatio*globalGearScale;
		}
		double getBladeAreaFactor() {
			return bladeArea/Math.max(bladeCount,1);
		}
	}
	static final class DriveCurve {
		double baseDriveTorqueIntercept;
		double baseDriveTorqueOmegaSlope;
		boolean hasPositiveDrive() {
			return baseDriveTorqueIntercept > 0 && baseDriveTorqueOmegaSlope > 0;
		}
		double getDriveTorqueIntercept(double globalGearScale) {
			return baseDriveTorqueIntercept*globalGearScale;
		}
		double getDriveTorqueOmegaSlope(double globalGearScale) {
			return baseDriveTorqueOmegaSlope*globalGearScale*globalGearScale;
		}
	}
	static final class TickSummary {
		double targetRPS;
		double driveTorque;
		double generatorTorque;
		double frictionTorque;
		double windageTorque;
		long powerGenerated;
	}
	static final class StageRuntimeData {
		final TurbineAssembly assembly;
		final CompiledStageStats compiledStats;
		final double massFlow;
		final double stageTorqueScale;
		StageRuntimeData(TurbineAssembly assembly,CompiledStageStats compiledStats,double massFlow,double stageTorqueScale) {
			this.assembly = assembly;
			this.compiledStats = compiledStats;
			this.massFlow = massFlow;
			this.stageTorqueScale = stageTorqueScale;
		}
		double getBaseDriveTorqueInterceptContribution() {
			return stageTorqueScale*compiledStats.inletWhirl*compiledStats.baseGearRatio;
		}
		double getBaseDriveTorqueOmegaSlopeContribution() {
			return stageTorqueScale*compiledStats.stageRadius*compiledStats.baseGearRatio*compiledStats.baseGearRatio;
		}
		double getDriveTorque(double omega,double globalGearScale) {
			if (massFlow <= 0 || compiledStats.stageSpecificWork <= 0)
				return 0;
			double actualGearRatio = compiledStats.getActualGearRatio(globalGearScale);
			double stageOmega = omega*actualGearRatio;
			double bladeSpeed = stageOmega*compiledStats.stageRadius;
			double localStageTorque = stageTorqueScale*(compiledStats.inletWhirl-bladeSpeed);
			return localStageTorque*actualGearRatio;
		}
	}
}
