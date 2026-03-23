package com.leafia.overwrite_contents.interfaces;

import net.minecraft.entity.player.EntityPlayer;

public interface IMixinEntityItem {

    EntityPlayer leafia$getDroppedBy();

    void leafia$setDroppedBy(EntityPlayer player);

    boolean leafia$getWasPickedUp();

    void leafia$setWasPickedUp(boolean pickedUp);
}
