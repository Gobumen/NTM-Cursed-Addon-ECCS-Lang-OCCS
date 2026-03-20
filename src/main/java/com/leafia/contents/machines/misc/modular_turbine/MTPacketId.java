package com.leafia.contents.machines.misc.modular_turbine;

public enum MTPacketId {
	PORT_IDENTIFIER(0),
	PORT_DECOMPRESSION(1),

	CORE_ASSEMBLY_SYNC(0),
	CORE_STEAM_SYNC(1),
	;
	public final int id;
	MTPacketId(int id) {
		this.id = id;
	}
}
