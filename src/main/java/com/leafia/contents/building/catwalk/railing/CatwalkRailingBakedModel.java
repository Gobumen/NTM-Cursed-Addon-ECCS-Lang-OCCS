package com.leafia.contents.building.catwalk.railing;

import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.model.AbstractWavefrontBakedModel;
import com.hbm.render.model.BakedModelTransforms;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CatwalkRailingBakedModel extends AbstractWavefrontBakedModel {
	public final TextureAtlasSprite registeredSprite;
	public final boolean forBlock;
	public final float itemYaw;
	public final String entry;
	public final CatwalkRailingBase railing;
	private final Map<String,Map<Integer,List<BakedQuad>>> blockQuads = new HashMap<>();
	public static Map<String,List<BakedQuad>> itemQuads = new HashMap<>();
	<T> T getElement(Map<String,T> map) {
		return map.getOrDefault(entry,null);
	}
	protected CatwalkRailingBakedModel(String entry,CatwalkRailingBase railing,HFRWavefrontObject model,TextureAtlasSprite sprite,boolean forBlock,float baseScale,float tx,float ty,float tz,float itemYaw) {
		super(model,forBlock ? DefaultVertexFormats.BLOCK : DefaultVertexFormats.ITEM,baseScale,tx,ty,tz,BakedModelTransforms.pipeItem());
		this.registeredSprite = sprite;
		this.forBlock = forBlock;
		this.itemYaw = itemYaw;
		this.entry = entry;
		this.railing = railing;
	}
	private List<BakedQuad> bake(Set<String> parts,float roll,float pitch,float yaw,boolean centerToBlock) {
		/*List<FaceGeometry> geometry = buildGeometry(parts, roll, pitch, yaw, false, centerToBlock);
		List<BakedQuad> quads = new ArrayList<>(geometry.size());
		for (FaceGeometry geo : geometry)
			quads.add(geo.buildQuad(registeredSprite, -1));
		return quads;*/
		return bakeSimpleQuads(parts,roll,pitch,yaw,false,centerToBlock,registeredSprite);
	}
	boolean getBooleanSafely(IBlockState state,PropertyBool property) {
		if (state.getPropertyKeys().contains(property))
			return state.getValue(property);
		return false;
	}
	EnumFacing getDirectionSafely(IBlockState state,PropertyDirection property) {
		if (state.getPropertyKeys().contains(property))
			return state.getValue(property);
		return EnumFacing.NORTH;
	}
	@Override
	public List<BakedQuad> getQuads(@Nullable IBlockState state,@Nullable EnumFacing side,long rand) {
		if (side != null) return Collections.emptyList();
		if (!forBlock) {
			List<BakedQuad> quads = getElement(itemQuads);
			if (quads == null) {
				quads = bake(
						Set.of(
								"curve_nXnZ","curve_nXpZ","curve_pXnZ","curve_pXpZ",
								"post_nX","post_nZ","post_pX","post_pZ"
						),
						0,0,itemYaw,false
				);
				itemQuads.put(entry,quads);
			}
			return quads;
		} else {
			Map<Integer,List<BakedQuad>> mop = getElement(blockQuads);
			if (mop == null) {
				mop = new HashMap<>();
				blockQuads.put(entry,mop);
			}
			int mask = 0;
			if (state != null) {
				try {
					mask += railing.getMetaFromState(state);
					if (state.getValue(CatwalkRailingBase.SLOPED)) {
						mask += 0b100_0000;
						mask += state.getValue(CatwalkRailingBase.S_DIR).getHorizontalIndex()<<4;
					}
					if (getBooleanSafely(state,CatwalkRailingBase.POS_X_NZ)) mask += 0b1_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.POS_X_PZ)) mask += 0b10_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.POS_Z_NX)) mask += 0b100_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.POS_Z_PX)) mask += 0b1000_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.NEG_X_NZ)) mask += 0b10000_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.NEG_X_PZ)) mask += 0b100000_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.NEG_Z_NX)) mask += 0b1000000_00000000;
					if (getBooleanSafely(state,CatwalkRailingBase.NEG_Z_PX)) mask += 0b10000000_00000000;
				} catch (Exception ignored) {}
			}
			List<BakedQuad> quads = mop.get(mask);
			if (quads == null) {
				HashSet<String> parts = new HashSet<>();
				IBlockState s = railing.getStateFromMeta(mask);
				if (getBooleanSafely(s,CatwalkRailingBase.SLOPED)) {
					EnumFacing face = getDirectionSafely(s,CatwalkRailingBase.S_DIR);
					if (face.equals(EnumFacing.NORTH)) {
						if (getBooleanSafely(s,CatwalkRailingBase.POS_X)) parts.add("stair_pXnZ");
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_X)) parts.add("stair_nXnZ");
					} else if (face.equals(EnumFacing.SOUTH)) {
						if (getBooleanSafely(s,CatwalkRailingBase.POS_X)) parts.add("stair_pXpZ");
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_X)) parts.add("stair_nXpZ");
					} else if (face.equals(EnumFacing.WEST)) {
						if (getBooleanSafely(s,CatwalkRailingBase.POS_Z)) parts.add("stair_nXpZ");
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z)) parts.add("stair_nXnZ");
					} else if (face.equals(EnumFacing.EAST)) {
						if (getBooleanSafely(s,CatwalkRailingBase.POS_Z)) parts.add("stair_pXpZ");
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z)) parts.add("stair_pXnZ");
					}
				} else {
					// // // // // // // //
					if (getBooleanSafely(s,CatwalkRailingBase.POS_X)) {
						parts.add("post_pX");
						if (getBooleanSafely(s,CatwalkRailingBase.POS_X_PZ))
							parts.add("straight_pXpZ");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.POS_Z))
								parts.add("curve_pXpZ");
							else
								parts.add("end_pXpZ");
						}
						if (getBooleanSafely(s,CatwalkRailingBase.POS_X_NZ))
							parts.add("straight_pXnZ");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z))
								parts.add("curve_pXnZ");
							else
								parts.add("end_pXnZ");
						}
					}
					// // // // // // // //
					if (getBooleanSafely(s,CatwalkRailingBase.NEG_X)) {
						parts.add("post_nX");
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_X_PZ))
							parts.add("straight_nXpZ");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.POS_Z))
								parts.add("curve_nXpZ");
							else
								parts.add("end_nXpZ");
						}
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_X_NZ))
							parts.add("straight_nXnZ");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z))
								parts.add("curve_nXnZ");
							else
								parts.add("end_nXnZ");
						}
					}
					// // // // // // // //
					if (getBooleanSafely(s,CatwalkRailingBase.POS_Z)) {
						parts.add("post_pZ");
						if (getBooleanSafely(s,CatwalkRailingBase.POS_Z_PX))
							parts.add("straight_pZpX");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.POS_X))
								parts.add("curve_pXpZ");
							else
								parts.add("end_pZpX");
						}
						if (getBooleanSafely(s,CatwalkRailingBase.POS_Z_NX))
							parts.add("straight_pZnX");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.NEG_X))
								parts.add("curve_nXpZ");
							else
								parts.add("end_pZnX");
						}
					}
					// // // // // // // //
					if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z)) {
						parts.add("post_nZ");
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z_PX))
							parts.add("straight_nZpX");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.POS_X))
								parts.add("curve_pXnZ");
							else
								parts.add("end_nZpX");
						}
						if (getBooleanSafely(s,CatwalkRailingBase.NEG_Z_NX))
							parts.add("straight_nZnX");
						else {
							if (getBooleanSafely(s,CatwalkRailingBase.NEG_X))
								parts.add("curve_nXnZ");
							else
								parts.add("end_nZnX");
						}
					}
				}
				quads = bake(parts,0,0,0,true);
				mop.put(mask,quads);
			}
			return quads;
		}
	}
	@Override
	public TextureAtlasSprite getParticleTexture() {
		return registeredSprite;
	}
	public static CatwalkRailingBakedModel forBlock(String entry,CatwalkRailingBase base,HFRWavefrontObject model,TextureAtlasSprite baseSprite) {
		return new CatwalkRailingBakedModel(entry,base,model,baseSprite,true,1.0F,0.0F,0.0F,0.0F,0.0F);
	}
	public static CatwalkRailingBakedModel forItem(String entry,CatwalkRailingBase base,HFRWavefrontObject model,TextureAtlasSprite baseSprite,float baseScale,float tx,float ty,float tz,float yaw) {
		return new CatwalkRailingBakedModel(entry,base,model,baseSprite,false,baseScale,tx,ty,tz,yaw);
	}
	public static CatwalkRailingBakedModel empty(String entry,CatwalkRailingBase base,TextureAtlasSprite sprite) {
		return new CatwalkRailingBakedModel(entry,base,new HFRWavefrontObject(new ResourceLocation("minecraft:empty")),sprite,true,1.0F,0,0,0,0);
	}
}
