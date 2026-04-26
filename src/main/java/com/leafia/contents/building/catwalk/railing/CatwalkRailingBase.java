package com.leafia.contents.building.catwalk.railing;

import com.hbm.items.IDynamicModels;
import com.hbm.render.loader.HFRWavefrontObject;
import com.leafia.dev.blocks.blockbase.AddonBlockBase;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class CatwalkRailingBase extends AddonBlockBase implements IDynamicModels {
	public String basePath = "_integrated/decoration/catwalks/railings/";
	public String spritePath = "";
	public String modelPath = "";
	/// should be unique to each types of railings
	public String modelKey = "";

	// data stored as meta
	public static final PropertyBool POS_X = PropertyBool.create("pos_x");
	public static final PropertyBool POS_Z = PropertyBool.create("pos_z");
	public static final PropertyBool NEG_X = PropertyBool.create("neg_x");
	public static final PropertyBool NEG_Z = PropertyBool.create("neg_z");

	// internal data
	public static final PropertyBool SLOPED = PropertyBool.create("sloped");
	public static final PropertyDirection S_DIR = PropertyDirection.create("s_dir"); // stair direction
	public static final PropertyBool POS_X_NZ = PropertyBool.create("pos_x_nz");
	public static final PropertyBool POS_X_PZ = PropertyBool.create("pos_x_pz");
	public static final PropertyBool POS_Z_NX = PropertyBool.create("pos_z_nx");
	public static final PropertyBool POS_Z_PX = PropertyBool.create("pos_z_px");
	public static final PropertyBool NEG_X_NZ = PropertyBool.create("neg_x_nz");
	public static final PropertyBool NEG_X_PZ = PropertyBool.create("neg_x_pz");
	public static final PropertyBool NEG_Z_NX = PropertyBool.create("neg_z_nx");
	public static final PropertyBool NEG_Z_PX = PropertyBool.create("neg_z_px");
	public void initializeState() {
		setDefaultState(this.getBlockState().getBaseState()
				.withProperty(POS_X,false)
				.withProperty(POS_Z,false)
				.withProperty(NEG_X,false)
				.withProperty(NEG_Z,false)
				.withProperty(SLOPED,false)
				.withProperty(S_DIR,EnumFacing.NORTH)
				.withProperty(POS_X_NZ,false)
				.withProperty(POS_X_PZ,false)
				.withProperty(POS_Z_NX,false)
				.withProperty(POS_Z_PX,false)
				.withProperty(NEG_X_NZ,false)
				.withProperty(NEG_X_PZ,false)
				.withProperty(NEG_Z_NX,false)
				.withProperty(NEG_Z_PX,false)
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
		return new BlockStateContainer(this,SLOPED,S_DIR,POS_X,POS_Z,NEG_X,NEG_Z,POS_X_NZ,POS_X_PZ,POS_Z_NX,POS_Z_PX,NEG_X_NZ,NEG_X_PZ,NEG_Z_NX,NEG_Z_PX);
	}
	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState()
				.withProperty(POS_X,(meta&0b1)==0b1)
				.withProperty(POS_Z,(meta&0b10)==0b10)
				.withProperty(NEG_X,(meta&0b100)==0b100)
				.withProperty(NEG_Z,(meta&0b1000)==0b1000)
				// for baked models
				.withProperty(S_DIR,EnumFacing.byHorizontalIndex((meta>>4&0b11)))
				.withProperty(SLOPED,(meta&0b1000000)==0b1000000)

				.withProperty(POS_X_NZ,(meta&0b1_00000000)==0b1_00000000)
				.withProperty(POS_X_PZ,(meta&0b10_00000000)==0b10_00000000)
				.withProperty(POS_Z_NX,(meta&0b100_00000000)==0b100_00000000)
				.withProperty(POS_Z_PX,(meta&0b1000_00000000)==0b1000_00000000)
				.withProperty(NEG_X_NZ,(meta&0b10000_00000000)==0b10000_00000000)
				.withProperty(NEG_X_PZ,(meta&0b100000_00000000)==0b100000_00000000)
				.withProperty(NEG_Z_NX,(meta&0b1000000_00000000)==0b1000000_00000000)
				.withProperty(NEG_Z_PX,(meta&0b10000000_00000000)==0b10000000_00000000);
	}
	@Override
	public int getMetaFromState(IBlockState state) {
		int meta = 0;
/*		if (state.getValue(SLOPED)) {
			meta += 0b1000000;
			meta += state.getValue(S_DIR).getHorizontalIndex()<<4;
		}*/ // i am stupid bro
		if (state.getValue(POS_X)) meta += 0b1;
		if (state.getValue(POS_Z)) meta += 0b10;
		if (state.getValue(NEG_X)) meta += 0b100;
		if (state.getValue(NEG_Z)) meta += 0b1000;
		return meta;
	}

	@Override
	public IBlockState getActualState(IBlockState state,IBlockAccess world,BlockPos pos) {
		return state
				.withProperty(POS_X_NZ,hasArm(world,pos.add( 0,0,-1),POS_X))
				.withProperty(POS_X_PZ,hasArm(world,pos.add( 0,0, 1),POS_X))
				.withProperty(POS_Z_NX,hasArm(world,pos.add(-1,0, 0),POS_Z))
				.withProperty(POS_Z_PX,hasArm(world,pos.add( 1,0, 0),POS_Z))
				.withProperty(NEG_X_NZ,hasArm(world,pos.add( 0,0,-1),NEG_X))
				.withProperty(NEG_X_PZ,hasArm(world,pos.add( 0,0, 1),NEG_X))
				.withProperty(NEG_Z_NX,hasArm(world,pos.add(-1,0, 0),NEG_Z))
				.withProperty(NEG_Z_PX,hasArm(world,pos.add( 1,0, 0),NEG_Z));
	}

	private static boolean hasArm(IBlockAccess world,BlockPos pos,PropertyBool arm) {
		IBlockState s = world.getBlockState(pos);
		if (!(s.getBlock() instanceof CatwalkRailingBase)) return false;
		return s.getValue(arm);
	}

	@Override
	public void neighborChanged(IBlockState state,World worldIn,BlockPos pos,Block blockIn,BlockPos fromPos) {
		super.neighborChanged(state,worldIn,pos,blockIn,fromPos);
		if (worldIn.isRemote)
			worldIn.markBlockRangeForRenderUpdate(pos,pos);
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
			itemMdl = CatwalkRailingBakedModel.forItem(modelKey,this,wavefront,registeredSprite,1,0,0,0,(float)Math.PI);
		} else {
			blockMdl = CatwalkRailingBakedModel.empty(modelKey,this,missing);
			itemMdl = CatwalkRailingBakedModel.empty(modelKey,this,missing);
		}
		ModelResourceLocation blockMrl = new ModelResourceLocation(getRegistryName(), "normal");
		evt.getModelRegistry().putObject(blockMrl,blockMdl);
		ModelResourceLocation itemMrl = new ModelResourceLocation(getRegistryName(), "inventory");
		evt.getModelRegistry().putObject(itemMrl,itemMdl);
	}
}
