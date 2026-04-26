package com.leafia.contents.building.catwalk.railing;

import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.model.AbstractWavefrontBakedModel;
import com.hbm.render.model.BakedModelTransforms;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.property.IExtendedBlockState;
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
		super(true,model,forBlock ? DefaultVertexFormats.BLOCK : DefaultVertexFormats.ITEM,baseScale,tx,ty,tz,BakedModelTransforms.pipeItem());
		this.registeredSprite = sprite;
		this.forBlock = forBlock;
		this.itemYaw = itemYaw;
		this.entry = entry;
		this.railing = railing;
	}

	private List<BakedQuad> bake(Set<String> parts,float roll,float pitch,float yaw,boolean centerToBlock) {
		return bakeSimpleQuads(parts,roll,pitch,yaw,true,centerToBlock,registeredSprite,-1,0,-0.5f,0);
	}

	private static boolean hasMask(int mask,int bit) {
		return (mask & bit) != 0;
	}

	private static EnumFacing getSlopeDirection(int mask) {
		return EnumFacing.byHorizontalIndex((mask >> CatwalkRailingBase.MASK_S_DIR_SHIFT) & 0b11);
	}

	private int getMask(@Nullable IBlockState state) {
		if (state == null)
			return 0;
		if (state instanceof IExtendedBlockState extState) {
			Integer mask = extState.getValue(CatwalkRailingBase.RENDER_MASK);
			if (mask != null)
				return mask;
		}
		try {
			return railing.getMetaFromState(state);
		} catch (Exception ignored) {
			return 0;
		}
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
			int mask = getMask(state);
			List<BakedQuad> quads = mop.get(mask);
			if (quads == null) {
				HashSet<String> parts = new HashSet<>();
				if (hasMask(mask,CatwalkRailingBase.MASK_SLOPED)) {
					EnumFacing face = getSlopeDirection(mask);
					if (face.equals(EnumFacing.NORTH)) {
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_X)) parts.add("stair_pXnZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X)) parts.add("stair_nXnZ");
					} else if (face.equals(EnumFacing.SOUTH)) {
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_X)) parts.add("stair_pXpZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X)) parts.add("stair_nXpZ");
					} else if (face.equals(EnumFacing.WEST)) {
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z)) parts.add("stair_nXpZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z)) parts.add("stair_nXnZ");
					} else if (face.equals(EnumFacing.EAST)) {
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z)) parts.add("stair_pXpZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z)) parts.add("stair_pXnZ");
					}
				} else {
					if (hasMask(mask,CatwalkRailingBase.MASK_POS_X)) {
						parts.add("post_pX");
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_X_PZ))
							parts.add("straight_pXpZ");
						else if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z))
							parts.add("curve_pXpZ");
						else
							parts.add("end_pXpZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_X_NZ))
							parts.add("straight_pXnZ");
						else if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z))
							parts.add("curve_pXnZ");
						else
							parts.add("end_pXnZ");
					}
					if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X)) {
						parts.add("post_nX");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X_PZ))
							parts.add("straight_nXpZ");
						else if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z))
							parts.add("curve_nXpZ");
						else
							parts.add("end_nXpZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X_NZ))
							parts.add("straight_nXnZ");
						else if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z))
							parts.add("curve_nXnZ");
						else
							parts.add("end_nXnZ");
					}
					if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z)) {
						parts.add("post_pZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z_PX))
							parts.add("straight_pZpX");
						else if (hasMask(mask,CatwalkRailingBase.MASK_POS_X))
							parts.add("curve_pXpZ");
						else
							parts.add("end_pZpX");
						if (hasMask(mask,CatwalkRailingBase.MASK_POS_Z_NX))
							parts.add("straight_pZnX");
						else if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X))
							parts.add("curve_nXpZ");
						else
							parts.add("end_pZnX");
					}
					if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z)) {
						parts.add("post_nZ");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z_PX))
							parts.add("straight_nZpX");
						else if (hasMask(mask,CatwalkRailingBase.MASK_POS_X))
							parts.add("curve_pXnZ");
						else
							parts.add("end_nZpX");
						if (hasMask(mask,CatwalkRailingBase.MASK_NEG_Z_NX))
							parts.add("straight_nZnX");
						else if (hasMask(mask,CatwalkRailingBase.MASK_NEG_X))
							parts.add("curve_nXnZ");
						else
							parts.add("end_nZnX");
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
