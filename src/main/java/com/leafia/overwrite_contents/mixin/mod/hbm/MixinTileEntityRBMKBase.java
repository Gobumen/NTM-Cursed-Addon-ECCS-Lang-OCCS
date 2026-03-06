package com.leafia.overwrite_contents.mixin.mod.hbm;

import com.hbm.config.MachineConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.RBMKDials.RBMKKeys;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.leafia.dev.optimization.LeafiaParticlePacket.RBMKJetParticle;
import com.leafia.overwrite_contents.interfaces.IMixinTileEntityRBMKBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityRBMKBase.class)
public abstract class MixinTileEntityRBMKBase extends TileEntityLoadedBase implements IMixinTileEntityRBMKBase {
	@Shadow(remap = false) public double jumpheight;
	@Shadow(remap = false) public double heat;
	@Shadow(remap = false) public boolean falling;
	@Shadow(remap = false) public abstract double maxHeat();
	@Shadow(remap = false) public float downwardSpeed;
	@Shadow(remap = false) @Final public static byte gravity;
	@Shadow(remap = false) public abstract void meltdown();
	@Unique public int leafia$damage = 0;
	@Unique public double leafia$lastHeat = 20;

	@Override
	public int leafia$getDamage() {
		return leafia$damage;
	}

	@Inject(method = "serialize",at = @At(value = "TAIL"),require = 1,remap = false)
	void leafia$onSerialize(ByteBuf buf,CallbackInfo ci) {
		buf.writeDouble(jumpheight);
	}
	@Inject(method = "deserialize",at = @At(value = "TAIL"),require = 1,remap = false)
	void leafia$onDeserialize(ByteBuf buf,CallbackInfo ci) {
		jumpheight = buf.readDouble();
	}

	@Inject(method = "writeToNBT",at = @At(value = "HEAD"),require = 1)
	void leafia$onWriteToNBT(NBTTagCompound nbt,CallbackInfoReturnable<NBTTagCompound> cir) {
		nbt.setInteger("leafia_damage",leafia$damage); // damage to my heart ;(
	}

	@Inject(method = "readFromNBT",at = @At(value = "HEAD"),require = 1)
	void leafia$onReadFromNBT(NBTTagCompound nbt,CallbackInfo ci) {
		leafia$damage = nbt.getInteger("leafia_damage");
	}

	@Inject(method = "update",at = @At(value = "HEAD"),require = 1)
	void leafia$onUpdate(CallbackInfo ci) {
		if (!world.isRemote) {
			if (heat > maxHeat() && !world.getGameRules().getBoolean(RBMKKeys.KEY_DISABLE_MELTDOWNS.keyString)) {
				if (heat >= leafia$lastHeat)
					leafia$damage += IMixinTileEntityRBMKBase.dmgIncrement;
			} else
				leafia$damage = Math.max(leafia$damage-1,0);
			if (leafia$damage > IMixinTileEntityRBMKBase.maxDamage)
				meltdown();
			leafia$lastHeat = heat;
		}
	}

	/**
	 * @author ntmleafia
	 * @reason rbmk damage mechanic
	 */
	@Overwrite(remap = false)
	private void jump(){
		if(leafia$damage <= 0 && !falling && jumpheight <= 0)
			return;

		if(!falling){ // linear rise
			if(leafia$damage > 0){
				int rand = world.rand.nextInt((IMixinTileEntityRBMKBase.maxDamage-leafia$damage)/3+5);
				if(this.jumpheight > 0 || /*world.rand.nextInt((int)(25D*maxHeat()/(this.heat-MachineConfig.rbmkJumpTemp+200D))+1)*/rand == 0){
					if (this.jumpheight == 0) {
						RBMKJetParticle particle = new RBMKJetParticle(20+world.rand.nextInt(10));
						particle.emit(
								new Vec3d(
										pos.getX()+0.5,
										pos.getY()+RBMKDials.getColumnHeight(world)-1,
										pos.getZ()+0.5
								),
								new Vec3d(0,1,0),
								world.provider.getDimension(),
								1000
						);
					}
					int dmg = (int)(Math.pow(leafia$damage/(double)IMixinTileEntityRBMKBase.maxDamage,0.5)*100);
					double change = dmg*0.0005D;
					double heightLimit = dmg*0.005D;

					this.jumpheight = this.jumpheight + change;

					if(this.jumpheight > heightLimit){
						this.jumpheight = heightLimit;
						this.falling = true;
					}
				}
			} else {
				this.falling = true;
			}
		} else{ // gravity fall
			if(this.jumpheight > 0){
				this.downwardSpeed = this.downwardSpeed + gravity * 0.05F;
				this.jumpheight = Math.max(this.jumpheight-this.downwardSpeed,0);
			} else {
				this.jumpheight = 0;
				this.downwardSpeed = 0;
				this.falling = false;
				world.playSound(null, pos.getX(),  pos.getY() + 4,  pos.getZ(), HBMSoundHandler.rbmkLid, SoundCategory.BLOCKS, 2.0F, 1.0F);
			}
		}
	}
}
