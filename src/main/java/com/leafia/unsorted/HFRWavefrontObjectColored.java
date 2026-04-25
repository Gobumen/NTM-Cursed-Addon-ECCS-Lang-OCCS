package com.leafia.unsorted;

import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.ModelFormatException;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;

public class HFRWavefrontObjectColored extends HFRWavefrontObject {
	public HFRWavefrontObjectColored(String filename,InputStream inputStream) throws ModelFormatException {
		super(filename,inputStream);
	}
	public HFRWavefrontObjectColored(ResourceLocation resource) throws ModelFormatException {
		super(resource);
	}
	public HFRWavefrontObjectColored(ResourceLocation resource,boolean mixedMode) throws ModelFormatException {
		super(resource,mixedMode);
	}
}
