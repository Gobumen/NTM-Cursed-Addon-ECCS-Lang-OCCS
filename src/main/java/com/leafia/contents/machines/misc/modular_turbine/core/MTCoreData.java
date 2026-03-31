package com.leafia.contents.machines.misc.modular_turbine.core;

import com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE.TurbineAssembly;

public final class MTCoreData {
	private MTCoreData() { }
	/** Relationship between a component and the stage currently being compiled. */
	public enum StageUpgradeRelation {
		MEMBER,
		BEFORE_TARGET,
		AFTER_TARGET,
	}
	/** Stage-local compile context. {@code assembly} is the target stage; neighbors use {@code relation}. */
	public static final class StageUpgradeContext {
		public final TurbineAssembly assembly;
		public final TurbineAssembly previousAssembly;
		public final TurbineAssembly nextAssembly;
		public final int componentPosition;
		public final StageUpgradeRelation relation;
		StageUpgradeContext(TurbineAssembly assembly,TurbineAssembly previousAssembly,TurbineAssembly nextAssembly,int componentPosition,StageUpgradeRelation relation) {
			this.assembly = assembly;
			this.previousAssembly = previousAssembly;
			this.nextAssembly = nextAssembly;
			this.componentPosition = componentPosition;
			this.relation = relation;
		}
		public boolean isMember() {
			return relation == StageUpgradeRelation.MEMBER;
		}
		public boolean isBeforeTarget() {
			return relation == StageUpgradeRelation.BEFORE_TARGET;
		}
		public boolean isAfterTarget() {
			return relation == StageUpgradeRelation.AFTER_TARGET;
		}
	}
	/** Mutable accumulator for one compiled steam stage. */
	public static final class StageUpgradeSummary {
		private double partialExpansionFractionOffset;
		private double inletWhirlCoefficientOffset;
		private double exitWhirlRecoveryFactorOffset;
		private double angularMomentumTorqueCoefficientOffset;
		private double bladeArea;
		private double flowCapacity;
		public void addPartialExpansionFractionOffset(double offset) {
			partialExpansionFractionOffset += offset;
		}
		public void addInletWhirlCoefficientOffset(double offset) {
			inletWhirlCoefficientOffset += offset;
		}
		public void addExitWhirlRecoveryFactorOffset(double offset) {
			exitWhirlRecoveryFactorOffset += offset;
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
		double getInletWhirlCoefficientOffset() {
			return inletWhirlCoefficientOffset;
		}
		double getExitWhirlRecoveryFactorOffset() {
			return exitWhirlRecoveryFactorOffset;
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
	/** Mutable accumulator for shaft-wide machine properties. */
	public static final class MachineUpgradeSummary {
		private double generatorLoadCoefficientOffset;
		private double generatorTorqueLimitOffset;
		private double coulombFrictionTorqueOffset;
		private double frictionRpsEpsilonOffset;
		private double viscousFrictionCoefficientOffset;
		private double windageCoefficientOffset;
		private double powerScaleOffset;
		private double rotorInertiaScaleOffset;
		public void addGeneratorLoadCoefficientOffset(double offset) {
			generatorLoadCoefficientOffset += offset;
		}
		public void addGeneratorTorqueLimitOffset(double offset) {
			generatorTorqueLimitOffset += offset;
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
		double getGeneratorLoadCoefficientOffset() {
			return generatorLoadCoefficientOffset;
		}
		double getGeneratorTorqueLimitOffset() {
			return generatorTorqueLimitOffset;
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
		double generatorLoadCoefficient;
		double generatorTorqueLimit;
		double coulombFrictionTorque;
		double frictionRpsEpsilon;
		double viscousFrictionCoefficient;
		double windageCoefficient;
		double powerScale;
		double rotorInertiaScale;
	}
	static final class CompiledStageStats {
		double partialExpansionFraction;
		double inletWhirlCoefficient;
		double exitWhirlRecoveryFactor;
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
