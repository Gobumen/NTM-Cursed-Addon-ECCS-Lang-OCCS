package com.leafia.dev;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

public interface ICacheInvalidator {
	void invalidateCache(World world,BlockPos pos,Map<Long,IBlockState> cache);
}
