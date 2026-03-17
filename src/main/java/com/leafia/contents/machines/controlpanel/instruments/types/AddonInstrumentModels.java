package com.leafia.contents.machines.controlpanel.instruments.types;

import com.hbm.render.loader.WaveFrontObjectVAO;

import static com.leafia.init.ResourceInit.getIntegrated;
import static com.leafia.init.ResourceInit.getVAO;

public class AddonInstrumentModels {
	public static final WaveFrontObjectVAO nixie = getVAO(getIntegrated("control_panel/instruments/nixie.obj"));
}
