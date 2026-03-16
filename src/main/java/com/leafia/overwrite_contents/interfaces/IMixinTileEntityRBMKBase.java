package com.leafia.overwrite_contents.interfaces;

public interface IMixinTileEntityRBMKBase {
	int dmgIncrement = 2;
	int maxDamage = 30*20*dmgIncrement;
	int leafia$getDamage();
	double leafia$jumpHeight();
}
