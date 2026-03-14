package com.custom_hbm.sound;

import net.minecraft.client.audio.ISound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.BiFunction;

@SideOnly(Side.CLIENT)
public class LCEAudioWrapperClient extends LCEAudioWrapper {

    private final SoundEvent source;
    private final SoundCategory category;
    private float x;
    private float y;
    private float z;
    private float volume = 1;
    private float pitch = 1;
    private boolean looped = true;
    private ISound.AttenuationType attenuationType = ISound.AttenuationType.NONE;
    private BiFunction<Float, Double, Double> attentuationFunction = null;
	LCEAudioDynamic sound;
	
	public LCEAudioWrapperClient(SoundEvent source,SoundCategory cat) {
        this.source = source;
        this.category = cat;
        rebuild();
    }

    protected void rebuild() {
        if (source == null) {
            sound = null;
            return;
        }
        sound = new LCEAudioDynamic(source, category);
        sound.setPosition(x, y, z);
        sound.setVolume(volume);
        sound.setPitch(pitch);
        sound.setLooped(looped);
        sound.setAttenuation(attenuationType);
        if (attentuationFunction != null)
            sound.setCustomAttenuation(attentuationFunction);
    }

    public void setAttenuation(ISound.AttenuationType attenuationType) {
        this.attenuationType = attenuationType;
        if (sound != null)
            sound.setAttenuation(attenuationType);
	}
	
	public LCEAudioWrapperClient updatePosition(float x,float y,float z) {
        this.x = x;
        this.y = y;
        this.z = z;
		if(sound != null)
			sound.setPosition(x, y, z);
		return this;
	}
	
	public LCEAudioWrapperClient updateVolume(float volume) {
        this.volume = volume;
		if(sound != null)
			sound.setVolume(volume);
		return this;
	}
	
	public LCEAudioWrapperClient updatePitch(float pitch) {
        this.pitch = pitch;
		if(sound != null)
			sound.setPitch(pitch);
		return this;
	}
	
	public float getVolume() {
		if(sound != null)
			return sound.getVolume();
		else
			return 1;
	}
	
	public float getPitch() {
		if(sound != null)
			return sound.getPitch();
		else
			return 1;
	}
	
	public LCEAudioWrapperClient startSound() {
        if (sound != null && !sound.isPlaying())
			sound.start();
		return this;
	}
	
	public LCEAudioWrapperClient stopSound() {
        if (sound != null) {
			sound.stop();
            rebuild();
        }
		return this;
	}

	public LCEAudioWrapperClient setLooped(boolean looped) {
        this.looped = looped;
		sound.setLooped(looped);
		return this;
	}

	@Override
	public LCEAudioWrapperClient setCustomAttentuation(BiFunction<Float,Double,Double> attentuationFunction) {
        this.attentuationFunction = attentuationFunction;
		sound.setCustomAttenuation(attentuationFunction);
		return this;
	}
}
