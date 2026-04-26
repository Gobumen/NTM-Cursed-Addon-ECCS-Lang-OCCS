package com.leafia.contents.building.catwalk.railing;

import com.hbm.blocks.network.SimpleUnlistedProperty;
import com.hbm.items.IDynamicModels;
import com.hbm.render.loader.HFRWavefrontObject;
import com.leafia.dev.blocks.blockbase.AddonBlockBase;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.IdentityHashMap;
import java.util.Map;

public abstract class CatwalkRailingBase extends AddonBlockBase implements IDynamicModels {
	public String basePath = "_integrated/decoration/catwalks/railings/";
	public String spritePath = "";
	public String modelPath = "";
	/// should be unique to each types of railings
	public String modelKey = "";

	public static final PropertyBool POS_X = PropertyBool.create("pos_x");
	public static final PropertyBool POS_Z = PropertyBool.create("pos_z");
	public static final PropertyBool NEG_X = PropertyBool.create("neg_x");
	public static final PropertyBool NEG_Z = PropertyBool.create("neg_z");

	public static final int MASK_POS_X = 0b1;
	public static final int MASK_POS_Z = 0b10;
	public static final int MASK_NEG_X = 0b100;
	public static final int MASK_NEG_Z = 0b1000;
	public static final int MASK_S_DIR_SHIFT = 4;
	public static final int MASK_SLOPED = 0b100_0000;
	public static final int MASK_POS_X_NZ = 0b1_00000000;
	public static final int MASK_POS_X_PZ = 0b10_00000000;
	public static final int MASK_POS_Z_NX = 0b100_00000000;
	public static final int MASK_POS_Z_PX = 0b1000_00000000;
	public static final int MASK_NEG_X_NZ = 0b10000_00000000;
	public static final int MASK_NEG_X_PZ = 0b100000_00000000;
	public static final int MASK_NEG_Z_NX = 0b1000000_00000000;
	public static final int MASK_NEG_Z_PX = 0b10000000_00000000;
	private static final int NO_RENDER_MASK = Integer.MIN_VALUE;

	public static final IUnlistedProperty<Integer> RENDER_MASK = new SimpleUnlistedProperty<>("render_mask",Integer.class);
	private static final Map<World,WorldStateCache> renderMaskCaches = new IdentityHashMap<>();

	private static final class WorldStateCache {
		private final Long2ObjectOpenHashMap<Long2IntOpenHashMap> chunkCaches = new Long2ObjectOpenHashMap<>();

		private static long getChunkKey(BlockPos pos) {
			return ChunkPos.asLong(pos.getX() >> 4,pos.getZ() >> 4);
		}

		private static long getPosKey(BlockPos pos) {
			return pos.toLong();
		}

		private Long2IntOpenHashMap getChunkCache(long chunkKey,boolean create) {
			Long2IntOpenHashMap chunkCache = chunkCaches.get(chunkKey);
			if (chunkCache == null && create) {
				chunkCache = new Long2IntOpenHashMap();
				chunkCache.defaultReturnValue(NO_RENDER_MASK);
				chunkCaches.put(chunkKey,chunkCache);
			}
			return chunkCache;
		}

		private int get(BlockPos pos) {
			Long2IntOpenHashMap chunkCache = getChunkCache(getChunkKey(pos),false);
			if (chunkCache == null)
				return NO_RENDER_MASK;
			return chunkCache.get(getPosKey(pos));
		}

		private void put(BlockPos pos,int mask) {
			getChunkCache(getChunkKey(pos),true).put(getPosKey(pos),mask);
		}

		private void remove(BlockPos pos) {
			long chunkKey = getChunkKey(pos);
			Long2IntOpenHashMap chunkCache = getChunkCache(chunkKey,false);
			if (chunkCache == null)
				return;
			chunkCache.remove(getPosKey(pos));
			if (chunkCache.isEmpty())
				chunkCaches.remove(chunkKey);
		}

		private void removeChunk(int chunkX,int chunkZ) {
			chunkCaches.remove(ChunkPos.asLong(chunkX,chunkZ));
		}
	}

	private static WorldStateCache getWorldStateCache(World world,boolean create) {
		WorldStateCache cache = renderMaskCaches.get(world);
		if (cache == null && create) {
			cache = new WorldStateCache();
			renderMaskCaches.put(world,cache);
		}
		return cache;
	}

	public static void initRenderMaskCache(World world) {
		if (world.isRemote)
			getWorldStateCache(world,true);
	}

	public static void clearRenderMaskCache(World world) {
		renderMaskCaches.remove(world);
	}

	public static void invalidateRenderMaskCache(World world,BlockPos pos) {
		WorldStateCache cache = getWorldStateCache(world,false);
		if (cache != null)
			cache.remove(pos);
	}

	public static void invalidateRenderMaskCacheChunk(World world,int chunkX,int chunkZ) {
		WorldStateCache cache = getWorldStateCache(world,false);
		if (cache == null)
			return;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++)
				cache.removeChunk(chunkX + x,chunkZ + z);
		}
	}

	private static int getRenderMaskCache(World world,BlockPos pos) {
		WorldStateCache cache = getWorldStateCache(world,false);
		if (cache == null)
			return NO_RENDER_MASK;
		return cache.get(pos);
	}

	private static void putRenderMaskCache(World world,BlockPos pos,int mask) {
		getWorldStateCache(world,true).put(pos,mask);
	}

	public void initializeState() {
		setDefaultState(this.getBlockState().getBaseState()
				.withProperty(POS_X,false)
				.withProperty(POS_Z,false)
				.withProperty(NEG_X,false)
				.withProperty(NEG_Z,false)
		);
	}

	public CatwalkRailingBase(Material m,String s) {
		super(m,s);
		initializeState();
		IDynamicModels.INSTANCES.add(this);
	}

	public CatwalkRailingBase(Material m,SoundType sound,String s) {
		super(m,sound,s);
		initializeState();
		IDynamicModels.INSTANCES.add(this);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this,new IProperty[]{POS_X,POS_Z,NEG_X,NEG_Z},new IUnlistedProperty[]{RENDER_MASK});
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState()
				.withProperty(POS_X,(meta&MASK_POS_X)!=0)
				.withProperty(POS_Z,(meta&MASK_POS_Z)!=0)
				.withProperty(NEG_X,(meta&MASK_NEG_X)!=0)
				.withProperty(NEG_Z,(meta&MASK_NEG_Z)!=0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		int meta = 0;
		if (state.getValue(POS_X)) meta |= MASK_POS_X;
		if (state.getValue(POS_Z)) meta |= MASK_POS_Z;
		if (state.getValue(NEG_X)) meta |= MASK_NEG_X;
		if (state.getValue(NEG_Z)) meta |= MASK_NEG_Z;
		return meta;
	}

	private static int getDiagonalMask(IBlockAccess world,BlockPos pos) {
		int mask = 0;
		if (hasArm(world,pos.add( 0,0,-1),POS_X) || hasArm(world,pos.add( 1,0,-1),NEG_X)) mask |= MASK_POS_X_NZ;
		if (hasArm(world,pos.add( 0,0, 1),POS_X) || hasArm(world,pos.add( 1,0, 1),NEG_X)) mask |= MASK_POS_X_PZ;
		if (hasArm(world,pos.add(-1,0, 0),POS_Z) || hasArm(world,pos.add(-1,0, 1),NEG_Z)) mask |= MASK_POS_Z_NX;
		if (hasArm(world,pos.add( 1,0, 0),POS_Z) || hasArm(world,pos.add( 1,0, 1),NEG_Z)) mask |= MASK_POS_Z_PX;
		if (hasArm(world,pos.add( 0,0,-1),NEG_X) || hasArm(world,pos.add(-1,0,-1),POS_X)) mask |= MASK_NEG_X_NZ;
		if (hasArm(world,pos.add( 0,0, 1),NEG_X) || hasArm(world,pos.add(-1,0, 1),POS_X)) mask |= MASK_NEG_X_PZ;
		if (hasArm(world,pos.add(-1,0, 0),NEG_Z) || hasArm(world,pos.add(-1,0,-1),POS_Z)) mask |= MASK_NEG_Z_NX;
		if (hasArm(world,pos.add( 1,0, 0),NEG_Z) || hasArm(world,pos.add( 1,0,-1),POS_Z)) mask |= MASK_NEG_Z_PX;
		return mask;
	}

	private static void invalidateRenderMaskAround(World world,BlockPos pos) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++)
				invalidateRenderMaskCache(world,pos.add(x,0,z));
		}
	}

	@Override
	public IBlockState getExtendedState(IBlockState state,IBlockAccess world,BlockPos pos) {
		if (!(state instanceof IExtendedBlockState extState))
			return state;
		int mask = getMetaFromState(state) | getDiagonalMask(world,pos);
		if (world instanceof World actualWorld && actualWorld.isRemote) {
			int cached = getRenderMaskCache(actualWorld,pos);
			if (cached == NO_RENDER_MASK)
				putRenderMaskCache(actualWorld,pos,mask);
			else
				mask = cached;
		}
		return extState.withProperty(RENDER_MASK,mask);
	}

	private static boolean hasArm(IBlockAccess world,BlockPos pos,PropertyBool arm) {
		IBlockState s = world.getBlockState(pos);
		if (!(s.getBlock() instanceof CatwalkRailingBase)) return false;
		return s.getValue(arm);
	}

	@Override
	public void neighborChanged(IBlockState state,World worldIn,BlockPos pos,Block blockIn,BlockPos fromPos) {
		super.neighborChanged(state,worldIn,pos,blockIn,fromPos);
		if (worldIn.isRemote) {
			invalidateRenderMaskAround(worldIn,pos);
			worldIn.markBlockRangeForRenderUpdate(pos,pos);
		}
	}

	@Override
	public void breakBlock(World worldIn,BlockPos pos,IBlockState state) {
		if (worldIn.isRemote)
			invalidateRenderMaskAround(worldIn,pos);
		super.breakBlock(worldIn,pos,state);
	}

	@Override
	public IBlockState getStateForPlacement(World world,BlockPos pos,EnumFacing facing,float hitX,float hitY,float hitZ,int meta,EntityLivingBase placer,EnumHand hand) {
		return getDefaultState()
				.withProperty(POS_X,true)
				.withProperty(POS_Z,true)
				.withProperty(NEG_X,true)
				.withProperty(NEG_Z,true);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public StateMapperBase getStateMapper(ResourceLocation loc) {
		return new StateMapperBase() {
			@Override
			protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
				return new ModelResourceLocation(loc,"normal");
			}
		};
	}

	@SideOnly(Side.CLIENT)
	public TextureAtlasSprite registeredSprite;

	@SideOnly(Side.CLIENT)
	@Override
	public void registerSprite(TextureMap map) {
		registeredSprite = map.registerSprite(new ResourceLocation("leafia",basePath+spritePath));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModel() {
		Item item = Item.getItemFromBlock(this);
		ModelResourceLocation inv = new ModelResourceLocation(this.getRegistryName(),"inventory");
		ModelLoader.setCustomModelResourceLocation(item,0,inv);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void bakeModel(ModelBakeEvent evt) {
		HFRWavefrontObject wavefront = null;
		try {
			wavefront = new HFRWavefrontObject(new ResourceLocation("leafia","textures/"+basePath+modelPath+".obj"));
		} catch (Exception e) {
			System.out.println("Model baking error: "+e.getMessage());
			e.printStackTrace();
		}
		TextureAtlasSprite missing = Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
		IBakedModel blockMdl;
		IBakedModel itemMdl;
		if (wavefront != null) {
			blockMdl = CatwalkRailingBakedModel.forBlock(modelKey,this,wavefront,registeredSprite);
			itemMdl = CatwalkRailingBakedModel.forItem(modelKey,this,wavefront,registeredSprite,0.75f,0,0,0,(float)Math.PI);
		} else {
			blockMdl = CatwalkRailingBakedModel.empty(modelKey,this,missing);
			itemMdl = CatwalkRailingBakedModel.empty(modelKey,this,missing);
		}
		ModelResourceLocation blockMrl = new ModelResourceLocation(getRegistryName(), "normal");
		evt.getModelRegistry().putObject(blockMrl,blockMdl);
		ModelResourceLocation itemMrl = new ModelResourceLocation(getRegistryName(), "inventory");
		evt.getModelRegistry().putObject(itemMrl,itemMdl);
	}

	@Override public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn,IBlockState state,BlockPos pos,EnumFacing face) { return BlockFaceShape.UNDEFINED; }
	@Override public boolean isFullCube(IBlockState state){ return false; }
	@Override public boolean isOpaqueCube(IBlockState state){ return false; }
}
