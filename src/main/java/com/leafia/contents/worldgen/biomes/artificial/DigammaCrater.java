package com.leafia.contents.worldgen.biomes.artificial;

import com.leafia.contents.worldgen.AddonBiome;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class DigammaCrater extends AddonBiome {
	public DigammaCrater(String resource) {
		super(resource,new BiomeProperties("DigammaCrater")
				.setRainfall(0).setRainDisabled()
				.setWaterColor(0xFF0000)
		);
		this.postInit = ()->{
			//BiomeManager.addBiome(BiomeType.DESERT,new BiomeEntry(this,15));
			BiomeDictionary.addTypes(this,Type.SWAMP,Type.WET);
		};
	}
	@Override
	public int getSkyColorByTemp(float currentTemperature) {
		return 0;
	}
	@Override
	public int getFogColor() {
		return getSkyColorByTemp(0);
	}
	@Override
	public float getFogStart(float original) { return original*0f; }
	@Override
	public float getFogEnd(float original) { return original*0.4f; }
	@Override
	public int getGrassColorAtPos(BlockPos pos) {
		return 0x666666;
	}
	@Override
	public int getFoliageColorAtPos(BlockPos pos) {
		return 0x666666;
	}
}
