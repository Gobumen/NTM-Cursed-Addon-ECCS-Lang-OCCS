package com.leafia.contents.machines.controlpanel.instruments.types;

import com.hbm.render.loader.WaveFrontObjectVAO;

import static com.leafia.AddonBase.getIntegrated;
import static com.leafia.init.ResourceInit.getVAO;

public class AddonInstrumentModels {
	static final String basePath = "control_panel/instruments/";
	public static final WaveFrontObjectVAO nixie = getVAO(getIntegrated(basePath+"nixie.obj"));
	public static final WaveFrontObjectVAO large_switch = getVAO(getIntegrated(basePath+"starboundswitchlol.obj"));
	public static final WaveFrontObjectVAO large_button = getVAO(getIntegrated(basePath+"starboundbuttonlol.obj"));
	public static final WaveFrontObjectVAO text_indicator = getVAO(getIntegrated(basePath+"textindicator.obj"));
}
