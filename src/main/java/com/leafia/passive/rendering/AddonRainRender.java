package com.leafia.passive.rendering;

import com.hbm.render.NTMRenderHelper;
import com.hbm.util.RenderUtil;
import com.leafia.contents.worldgen.biomes.artificial.DigammaCrater;
import com.leafia.init.LeafiaSoundEvents;
import com.leafia.savedata.FalloutSavedData;
import com.leafia.savedata.FalloutSavedData.FalloutData;
import com.leafia.unsorted.ParticleRainBlood;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class AddonRainRender {
	public static AddonRainRender INSTANCE = new AddonRainRender();
	private static final ResourceLocation texLight = new ResourceLocation("hbm", "textures/entity/fallout.png");
	private static final ResourceLocation texDense = new ResourceLocation("leafia", "textures/fallout_dense.png");

	private static final ResourceLocation texBlood = new ResourceLocation("leafia","textures/bloodrain.png");

	private Minecraft mc;
	private Random random = new Random();
	float[] rainXCoords;
	float[] rainYCoords;
	public int rendererUpdateCount;

	public AddonRainRender() {
		this.mc = Minecraft.getMinecraft();
		this.rainXCoords = new float[1024];
		this.rainYCoords = new float[1024];
		for (int i = 0; i < 32; ++i)
		{
			for (int j = 0; j < 32; ++j)
			{
				float f = (float)(j - 16);
				float f1 = (float)(i - 16);
				float f2 = MathHelper.sqrt(f * f + f1 * f1);
				this.rainXCoords[i << 5 | j] = -f1 / f2;
				this.rainYCoords[i << 5 | j] = f / f2;
			}
		}
	}
	public void render(float partialTicks) {
		GlStateManager.pushMatrix();
		GlStateManager.disableCull();
		boolean light = RenderUtil.isLightingEnabled();
		//Drillgon200: It doesn't work when I use GLStateManager...
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		NTMRenderHelper.resetColor();

		GlStateManager.disableLighting();

		renderFalloutRain(partialTicks);
		renderBloodRain(partialTicks);

		if (light)
			GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
	public void update() {
		rendererUpdateCount++;
		addRainParticles();
	}
	private int rainSoundCounter;
	private void addRainParticles()
	{
		float f = 1;

		if (!this.mc.gameSettings.fancyGraphics)
		{
			f /= 2.0F;
		}

		if (f != 0.0F)
		{
			this.random.setSeed((long)this.rendererUpdateCount * 312987231L);
			Entity entity = this.mc.getRenderViewEntity();
			World world = this.mc.world;
			BlockPos blockpos = new BlockPos(entity);
			int i = 10;
			double d0 = 0.0D;
			double d1 = 0.0D;
			double d2 = 0.0D;
			int j = 0;
			int k = (int)(100.0F * f * f);

			if (this.mc.gameSettings.particleSetting == 1)
			{
				k >>= 1;
			}
			else if (this.mc.gameSettings.particleSetting == 2)
			{
				k = 0;
			}

			for (int l = 0; l < k; ++l)
			{
				BlockPos blockpos1 = world.getPrecipitationHeight(blockpos.add(this.random.nextInt(10) - this.random.nextInt(10), 0, this.random.nextInt(10) - this.random.nextInt(10)));
				Biome biome = world.getBiome(blockpos1);
				BlockPos blockpos2 = blockpos1.down();
				IBlockState iblockstate = world.getBlockState(blockpos2);

				if (blockpos1.getY() <= blockpos.getY() + 10 && blockpos1.getY() >= blockpos.getY() - 10 && biome instanceof DigammaCrater)
				{
					double d3 = this.random.nextDouble();
					double d4 = this.random.nextDouble();
					AxisAlignedBB axisalignedbb = iblockstate.getBoundingBox(world, blockpos2);

					if (iblockstate.getMaterial() != Material.LAVA && iblockstate.getBlock() != Blocks.MAGMA)
					{
						if (iblockstate.getMaterial() != Material.AIR)
						{
							++j;

							if (this.random.nextInt(j) == 0)
							{
								d0 = (double)blockpos2.getX() + d3;
								d1 = (double)((float)blockpos2.getY() + 0.1F) + axisalignedbb.maxY - 1.0D;
								d2 = (double)blockpos2.getZ() + d4;
							}
							ParticleRainBlood blood = new ParticleRainBlood(this.mc.world,(double)blockpos2.getX() + d3, (double)((float)blockpos2.getY() + 0.1F) + axisalignedbb.maxY, (double)blockpos2.getZ() + d4);
							this.mc.effectRenderer.addEffect(blood);
						}
					}
					else
					{
						this.mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (double)blockpos1.getX() + d3, (double)((float)blockpos1.getY() + 0.1F) - axisalignedbb.minY, (double)blockpos1.getZ() + d4, 0.0D, 0.0D, 0.0D, new int[0]);
					}
				}
			}

			if (j > 0 && this.random.nextInt(3) < this.rainSoundCounter++)
			{
				this.rainSoundCounter = 0;

				if (d1 > (double)(blockpos.getY() + 1) && world.getPrecipitationHeight(blockpos).getY() > MathHelper.floor((float)blockpos.getY()))
				{
					this.mc.world.playSound(d0, d1, d2, LeafiaSoundEvents.sbesrottenrain_above, SoundCategory.WEATHER, 0.3F, 1F, false);
				}
				else
				{
					this.mc.world.playSound(d0, d1, d2, LeafiaSoundEvents.sbesrottenrain, SoundCategory.WEATHER, 0.3F, 1.0F, false);
				}
			}
		}
	}
	protected void renderBloodRain(float partialTick) {
		MutableBlockPos pos = new BlockPos.MutableBlockPos();
		World world = mc.world;
		Entity entitylivingbase = this.mc.getRenderViewEntity();


		if (true) {

			if (this.rainXCoords == null) {
				this.rainXCoords = new float[1024];
				this.rainYCoords = new float[1024];

				for (int i = 0; i < 32; ++i) {
					for (int j = 0; j < 32; ++j) {
						float f2 = j - 16;
						float f3 = i - 16;
						float f4 = MathHelper.sqrt(f2 * f2 + f3 * f3);
						this.rainXCoords[i << 5 | j] = -f3 / f4;
						this.rainYCoords[i << 5 | j] = f2 / f4;
					}
				}
			}

			WorldClient worldclient = this.mc.world;
			int k2 = MathHelper.floor(entitylivingbase.posX);
			int l2 = MathHelper.floor(entitylivingbase.posY);
			int i3 = MathHelper.floor(entitylivingbase.posZ);
			Tessellator tessellator = Tessellator.getInstance();
			GlStateManager.disableCull();
			GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			double d0 = entitylivingbase.lastTickPosX
					+ (entitylivingbase.posX - entitylivingbase.lastTickPosX) * partialTick;
			double d1 = entitylivingbase.lastTickPosY
					+ (entitylivingbase.posY - entitylivingbase.lastTickPosY) * partialTick;
			double d2 = entitylivingbase.lastTickPosZ
					+ (entitylivingbase.posZ - entitylivingbase.lastTickPosZ) * partialTick;
			int k = MathHelper.floor(d1);
			byte b0 = 5;

			if (this.mc.gameSettings.fancyGraphics) {
				b0 = 10;
			}

			byte b1 = -1;
			float f5 = this.rendererUpdateCount + partialTick;

			if (this.mc.gameSettings.fancyGraphics) {
				b0 = 10;
			}

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			NTMRenderHelper.resetColor();
			for (int l = i3 - b0; l <= i3 + b0; ++l) {
				for (int i1 = k2 - b0; i1 <= k2 + b0; ++i1) {
					int j1 = (l - i3 + 16) * 32 + i1 - k2 + 16;
					float f6 = this.rainXCoords[j1] * 0.5F;
					float f7 = this.rainYCoords[j1] * 0.5F;
					pos.setPos(i1, 50, l);
					Biome biomegenbase = worldclient.getBiomeForCoordsBody(pos);

					if (biomegenbase instanceof DigammaCrater) {
						int k1 = worldclient.getPrecipitationHeight(pos).getY();
						int l1 = l2 - b0;
						int i2 = l2 + b0;

						if (l1 < k1) {
							l1 = k1;
						}

						if (i2 < k1) {
							i2 = k1;
						}

						float f8 = 1.0F;
						int j2 = k1;

						if (k1 < k) {
							j2 = k;
						}

						if (l1 != i2) {
							pos.setY(l1);
							this.random.setSeed(i1 * i1 * 3121 + i1 * 45238971 ^ l * l * 418711 + l * 13761);
							biomegenbase.getTemperature(pos);
							double d4;

							{
								if (b1 != 0) {
									if (b1 >= 1) {
										tessellator.draw();
									}
									b1 = 0;
									this.mc.getTextureManager().bindTexture(texBlood);
									tessellator.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);;
								}

								//f10 = ((this.rendererUpdateCount & 511) + partialTick) / 512.0F;
								//float f16 = this.random.nextFloat() + f5 * 0.01F * (float) this.random.nextGaussian();
								double f11 = -((double)(this.rendererUpdateCount + l1 * l1 * 3121 + l1 * 45238971 + k1 * k1 * 418711 + k1 * 13761 & 31) + (double)partialTick) / 32.0D * (3.0D + this.random.nextDouble());

								//float f11 = this.random.nextFloat() + f5 * (float) this.random.nextGaussian() * 0.001F;
								d4 = i1 + 0.5F - entitylivingbase.posX;
								double d5 = l + 0.5F - entitylivingbase.posZ;
								float f14 = MathHelper.sqrt(d4 * d4 + d5 * d5) / b0;
								float f15 = 1;
								BufferBuilder buf = tessellator.getBuffer();
								worldclient.getLightBrightness(pos.setPos(i1, j2, l));
								//  buf.putBrightness4(bright, bright, bright, bright);
								buf.setTranslation(-d0 * 1.0D, -d1 * 1.0D, -d2 * 1.0D);

								buf.pos(i1 - f6 + 0.5D, l1, l - f7 + 0.5D).tex(0.0F, l1 / 4.0F - f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * f15).endVertex();
								buf.pos(i1 + f6 + 0.5D, l1, l + f7 + 0.5D).tex(1.0F, l1 / 4.0F - f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * f15).endVertex();
								buf.pos(i1 + f6 + 0.5D, i2, l + f7 + 0.5D).tex(1.0F, i2 / 4.0F - f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * f15).endVertex();
								buf.pos(i1 - f6 + 0.5D, i2, l - f7 + 0.5D).tex(0.0F, i2 / 4.0F - f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * f15).endVertex();
								buf.setTranslation(0.0D, 0.0D, 0.0D);
							}
						}
					}
				}
			}

			if (b1 >= 0) {
				tessellator.draw();
			}

			GlStateManager.enableCull();
			GlStateManager.disableBlend();
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		}
	}
	protected void renderFalloutRain(float partialTick) {
		MutableBlockPos pos = new BlockPos.MutableBlockPos();
		World world = mc.world;
		FalloutSavedData saved = FalloutSavedData.forWorld(world);
		Entity entitylivingbase = this.mc.getRenderViewEntity();
		float rawStrength = 0;
		FalloutData data = saved.getDensest(new Vec3d(entitylivingbase.posX,0,entitylivingbase.posZ));
		if (data != null)
			rawStrength = (float)data.getDensity();

		float f1 = (float)Math.min(Math.pow(rawStrength*2,0.5),1); // strength

		if (f1 > 0.0F) {

			if (this.rainXCoords == null) {
				this.rainXCoords = new float[1024];
				this.rainYCoords = new float[1024];

				for (int i = 0; i < 32; ++i) {
					for (int j = 0; j < 32; ++j) {
						float f2 = j - 16;
						float f3 = i - 16;
						float f4 = MathHelper.sqrt(f2 * f2 + f3 * f3);
						this.rainXCoords[i << 5 | j] = -f3 / f4;
						this.rainYCoords[i << 5 | j] = f2 / f4;
					}
				}
			}

			WorldClient worldclient = this.mc.world;
			int k2 = MathHelper.floor(entitylivingbase.posX);
			int l2 = MathHelper.floor(entitylivingbase.posY);
			int i3 = MathHelper.floor(entitylivingbase.posZ);
			Tessellator tessellator = Tessellator.getInstance();
			GlStateManager.disableCull();
			GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			double d0 = entitylivingbase.lastTickPosX
					+ (entitylivingbase.posX - entitylivingbase.lastTickPosX) * partialTick;
			double d1 = entitylivingbase.lastTickPosY
					+ (entitylivingbase.posY - entitylivingbase.lastTickPosY) * partialTick;
			double d2 = entitylivingbase.lastTickPosZ
					+ (entitylivingbase.posZ - entitylivingbase.lastTickPosZ) * partialTick;
			int k = MathHelper.floor(d1);
			byte b0 = 5;

			if (this.mc.gameSettings.fancyGraphics) {
				b0 = 10;
			}

			byte b1 = -1;
			float f5 = this.rendererUpdateCount + partialTick;

			if (this.mc.gameSettings.fancyGraphics) {
				b0 = 10;
			}

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			NTMRenderHelper.resetColor();
			for (int l = i3 - b0; l <= i3 + b0; ++l) {
				for (int i1 = k2 - b0; i1 <= k2 + b0; ++i1) {
					int j1 = (l - i3 + 16) * 32 + i1 - k2 + 16;
					float f6 = this.rainXCoords[j1] * 0.5F;
					float f7 = this.rainYCoords[j1] * 0.5F;
					pos.setPos(i1, 50, l);
					Biome biomegenbase = worldclient.getBiomeForCoordsBody(pos);
					double mul = data.getDistanceMultiplier(new Vec3d(i1+0.5,0,l+0.5));
					float rawAlpha = (float)(rawStrength*mul);
					float alpha = (float)(f1*mul);

					if (true) {
						int k1 = worldclient.getPrecipitationHeight(pos).getY();
						int l1 = l2 - b0;
						int i2 = l2 + b0;

						if (l1 < k1) {
							l1 = k1;
						}

						if (i2 < k1) {
							i2 = k1;
						}

						float f8 = 1.0F;
						int j2 = k1;

						if (k1 < k) {
							j2 = k;
						}

						if (l1 != i2) {
							pos.setY(l1);
							this.random.setSeed(i1 * i1 * 3121 + i1 * 45238971 ^ l * l * 418711 + l * 13761);
							biomegenbase.getTemperature(pos);
							float f10;
							double d4;

							{
								if (b1 != 1) {
									if (b1 >= 0) {
										tessellator.draw();
									}
									b1 = 1;
									this.mc.getTextureManager().bindTexture(/*rawAlpha >= 0.5 ?*/ texDense /*: texLight*/);
									tessellator.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);;
								}

								f10 = ((this.rendererUpdateCount & 511) + partialTick) / 512.0F;
								float f16 = this.random.nextFloat() + f5 * 0.01F * (float) this.random.nextGaussian();
								float f11 = this.random.nextFloat() + f5 * (float) this.random.nextGaussian() * 0.001F;
								d4 = i1 + 0.5F - entitylivingbase.posX;
								double d5 = l + 0.5F - entitylivingbase.posZ;
								float f14 = MathHelper.sqrt(d4 * d4 + d5 * d5) / b0;
								float f15 = 1;
								BufferBuilder buf = tessellator.getBuffer();
								worldclient.getLightBrightness(pos.setPos(i1, j2, l));
								//  buf.putBrightness4(bright, bright, bright, bright);
								buf.setTranslation(-d0 * 1.0D, -d1 * 1.0D, -d2 * 1.0D);

								buf.pos(i1 - f6 + 0.5D, l1, l - f7 + 0.5D).tex(0.0F * f8 + f16, l1 * f8 / 4.0F + f10 * f8 + f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * alpha).endVertex();
								buf.pos(i1 + f6 + 0.5D, l1, l + f7 + 0.5D).tex(1.0F * f8 + f16, l1 * f8 / 4.0F + f10 * f8 + f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * alpha).endVertex();
								buf.pos(i1 + f6 + 0.5D, i2, l + f7 + 0.5D).tex(1.0F * f8 + f16, i2 * f8 / 4.0F + f10 * f8 + f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * alpha).endVertex();
								buf.pos(i1 - f6 + 0.5D, i2, l - f7 + 0.5D).tex(0.0F * f8 + f16, i2 * f8 / 4.0F + f10 * f8 + f11).color(f15, f15, f15, ((1.0F - f14 * f14) * 0.3F + 0.5F) * alpha).endVertex();
								buf.setTranslation(0.0D, 0.0D, 0.0D);
							}
						}
					}
				}
			}

			if (b1 >= 0) {
				tessellator.draw();
			}

			GlStateManager.enableCull();
			GlStateManager.disableBlend();
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		}
	}
}
