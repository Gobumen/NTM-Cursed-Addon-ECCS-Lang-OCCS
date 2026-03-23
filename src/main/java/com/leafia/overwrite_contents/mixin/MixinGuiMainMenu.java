package com.leafia.overwrite_contents.mixin;

import com.leafia.settings.AddonConfig;
import com.leafia.transformer.LeafiaGeneralLocal;
import net.minecraft.client.gui.GuiMainMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(GuiMainMenu.class)
public class MixinGuiMainMenu {

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0)
    )
    private boolean leafia$injectWackySplashes(List<String> splashes) {
        if (AddonConfig.enableWackySplashes) {
            LeafiaGeneralLocal.injectWackySplashes(splashes);
        }
        return splashes.isEmpty();
    }
}
