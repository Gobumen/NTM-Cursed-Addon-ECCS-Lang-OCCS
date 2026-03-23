package com.leafia.overwrite_contents.mixin;

import com.leafia.overwrite_contents.interfaces.IMixinEntityItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityItem.class)
public abstract class MixinEntityItem implements IMixinEntityItem {

    @Unique
    private EntityPlayer leafia$addonDroppedBy;

    @Unique
    private boolean leafia$addonWasPickedUp;

    @Override
    public EntityPlayer leafia$getDroppedBy() {
        return leafia$addonDroppedBy;
    }

    @Override
    public void leafia$setDroppedBy(EntityPlayer player) {
        leafia$addonDroppedBy = player;
    }

    @Override
    public boolean leafia$getWasPickedUp() {
        return leafia$addonWasPickedUp;
    }

    @Override
    public void leafia$setWasPickedUp(boolean pickedUp) {
        leafia$addonWasPickedUp = pickedUp;
    }
}
