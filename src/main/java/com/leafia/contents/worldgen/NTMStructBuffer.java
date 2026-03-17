package com.leafia.contents.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.leafia.contents.building.generic.lined_asphalt.LinedAsphaltBlock;
import com.leafia.contents.building.generic.lined_asphalt.LinedAsphaltBlock.AsphaltLine;
import com.leafia.contents.gear.wands.ItemWandSaving.SavingProperty;
import com.leafia.dev.optimization.bitbyte.LeafiaBuf;
import com.llib.LeafiaLib;
import com.llib.exceptions.LeafiaDevFlaw;
import com.llib.group.LeafiaMap;
import com.llib.technical.FifthString;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NTMStructBuffer {
    private static final MethodHandle fillSpaceHandle;

    static {
        try {
            Method reflected = BlockDummyable.class.getDeclaredMethod("fillSpace",World.class,int.class,int.class,int.class,ForgeDirection.class,int.class);
            reflected.setAccessible(true);
            fillSpaceHandle= MethodHandles.lookup().unreflect(reflected);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new LeafiaDevFlaw(e);
        }
    }

	public enum NTMStructVersion {
		V0_FIRST_TEST,
		V1_REPLACEMENT_MAP_ADDITION,
		V2_BETTER_TE_COMPRESSION,
		V3_METADATA_FIX,
		;
		public boolean isUpOrNewerThan(NTMStructVersion other) { return this.ordinal() >= other.ordinal(); }
		public boolean isNewerThan(NTMStructVersion other) { return this.ordinal() > other.ordinal(); }
		public boolean isOlderThan(NTMStructVersion other) { return this.ordinal() < other.ordinal(); }
		public boolean isUpOrOlderThan(NTMStructVersion other) { return this.ordinal() <= other.ordinal(); }
		public static final NTMStructVersion latest = V3_METADATA_FIX;
	}
	public enum NTMStructAngle {
		ORIGINAL(1,1,0,0),RIGHT(0,0,-1,1),BACK(-1,-1,0,0),LEFT(0,0,1,-1);
		final int xx; final int zz; final int xz; final int zx;
		NTMStructAngle(int xx,int zz,int xz,int zx) {
			this.xx = xx;
			this.zz = zz;
			this.xz = xz;
			this.zx = zx;
		}
		public NTMStructAngle getRight() { return NTMStructAngle.values()[Math.floorMod(this.ordinal()+1,4)]; }
		public NTMStructAngle getLeft() { return NTMStructAngle.values()[Math.floorMod(this.ordinal()-1,4)]; }
		public int getX(int x,int z) { return x*xx+z*xz; }
		public int getZ(int x,int z) { return z*zz+x*zx; }
	}
	public static class StructData {
		LeafiaBuf buf;
		String name;
		boolean terrarinAware = false;
		public StructData() {}
		public String getName() { return name; }
	}
	// this whole shit is taken from Community Edition QMAWLoader
	// https://github.com/Warfactory-Offical/Hbm-s-Nuclear-Tech-CE/blob/master/src/main/java/com/hbm/qmaw/QMAWLoader.java
	public static class StructLoader implements ISelectiveResourceReloadListener  {
		public static final HashSet<FileResourcePack> modResourcePacks = new HashSet<>();
		public static final HashSet<FolderResourcePack> folderResourcePacks = new HashSet<>();
		public static final Map<String,StructData> structs = new HashMap<>();
		static void addStructMeta(String name,JsonObject object) {
			StructData data = structs.getOrDefault(name,new StructData());
			if (object.has("name"))
				data.name = object.get("name").getAsString();
			if (object.has("terrarinAware"))
				data.terrarinAware = object.get("terrarinAware").getAsBoolean();
			structs.putIfAbsent(name,data);
			MainRegistry.logger.info("[NTMSTRUCT META] Loaded structure meta "+name);
		}
		// thanks community edition
		@Override
		public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
			long timestamp = System.currentTimeMillis();
			MainRegistry.logger.info("[NTMSTRUCT] Reloading structures...");
			init();
			clientShit();
			MainRegistry.logger.info("[NTMSTRUCT] Loaded " + structs.size() + " structure entries! (" + (System.currentTimeMillis() - timestamp) + "ms)");
		}
		// thanks community edition
		public static void init() {
			modResourcePacks.clear();
			folderResourcePacks.clear();
			for (ModContainer mod : Loader.instance().getActiveModList()) {
				File src = mod.getSource();
				if (src.isFile())
					modResourcePacks.add(new FileResourcePack(src));
				if (src.isDirectory())
					folderResourcePacks.add(new FolderResourcePack(src));
			}
			structs.clear();
			agonyEngine();
		}
		// thanks community edition
		public static void agonyEngine() {
			for (FileResourcePack modResourcePack : modResourcePacks) {
				try {
					File file = ((AbstractResourcePack) modResourcePack).resourcePackFile;
					if (file != null) {
						dissectZip(file);
					} else {
						MainRegistry.logger.warn("[NTMSTRUCT Loader] resourcePackFile is null for " + modResourcePack.getPackName());
					}
				} catch (Exception e) {
					MainRegistry.logger.error("[NTMSTRUCT Loader] Failed to access private field for " + modResourcePack.getPackName() + ". THIS IS A BUG!", e);
				}
			}
			for (FolderResourcePack modResourcePack : folderResourcePacks) {
				try {
					File file = ((AbstractResourcePack) modResourcePack).resourcePackFile;
					if (file != null) {
						dissectFolder(file);
					} else {
						MainRegistry.logger.warn("[NTMSTRUCT Loader] resourcePackFile is null for " + modResourcePack.getPackName());
					}
				} catch (Exception e) {
					MainRegistry.logger.error("[NTMSTRUCT Loader] Failed to access private field for " + modResourcePack.getPackName() + ". THIS IS A BUG!", e);
				}
			}
		}
		@SideOnly(Side.CLIENT)
		public static void clientShit() {
			try {
				File devEnvManualFolder = new File(Minecraft.getMinecraft().gameDir.getAbsolutePath().replace("/eclipse/.", "") + "/src/main/resources/assets/hbm/manual");
				if (devEnvManualFolder.exists() && devEnvManualFolder.isDirectory()) {
					MainRegistry.logger.info("[NTMSTRUCT] Exploring " + devEnvManualFolder.getAbsolutePath());
					dissectStructFolder(devEnvManualFolder);
				}
			} catch (Exception e) {
				MainRegistry.logger.error("[NTMSTRUCT] Failed to explore dev environment manual folder!", e);
			}

			try {
				ResourcePackRepository repo = Minecraft.getMinecraft().getResourcePackRepository();

				for (ResourcePackRepository.Entry entry : repo.getRepositoryEntries()) {
					IResourcePack pack = entry.getResourcePack();

					if (pack instanceof FileResourcePack) {
						try {
							File file = ((AbstractResourcePack) pack).resourcePackFile;
							if (file != null) {
								dissectZip(file);
							} else {
								MainRegistry.logger.warn("[NTMSTRUCT Loader] resourcePackFile is null for " + pack.getPackName());
							}
						} catch (Exception e) {
							MainRegistry.logger.error("[NTMSTRUCT Loader] Failed to dissect FileResourcePack for " + pack.getPackName(), e);
						}
					}

					if (pack instanceof FolderResourcePack) {
						try {
							File file = ((AbstractResourcePack) pack).resourcePackFile;
							if (file != null) {
								dissectFolder(file);
							} else {
								MainRegistry.logger.warn("[NTMSTRUCT Loader] resourcePackFile is null for " + pack.getPackName());
							}
						} catch (Exception e) {
							MainRegistry.logger.error("[NTMSTRUCT Loader] Failed to dissect FolderResourcePack for " + pack.getPackName(), e);
						}
					}
				}
			} catch (Exception e) {
				MainRegistry.logger.error("[NTMSTRUCT Loader] Failed to process resource pack repository", e);
			}
		}
		public static final JsonParser parser = new JsonParser();
		// thanks community edition
		public static void dissectZip(File zipFile) {

			if(zipFile == null) {
				MainRegistry.logger.info("[NTMSTRUCT] Pack file does not exist!");
				return;
			}

			ZipFile zip = null;

			try {
				zip = new ZipFile(zipFile);
				Enumeration<? extends ZipEntry> enumerator = zip.entries();

				while(enumerator.hasMoreElements()) {
					ZipEntry entry = enumerator.nextElement();
					String name = entry.getName();
					if(name.startsWith("assets/leafia/structs/")) {
						if (name.endsWith(".ntmstruct")) {
							InputStream fileStream = zip.getInputStream(entry);
							byte[] bytes = new byte[fileStream.available()];
							fileStream.read(bytes);
							LeafiaBuf buf = new LeafiaBuf(null);
							buf.bytes = bytes;
							buf.writerIndex = bytes.length*8;
							fileStream.close();
							String structName = name.replace("assets/leafia/structs/", "");
							StructData data = structs.getOrDefault(structName.substring(0,structName.length()-10),new StructData());
							data.buf = buf;
							if (data.name == null)
								data.name = structName;
							structs.putIfAbsent(structName.substring(0,structName.length()-10),data);
							MainRegistry.logger.info("[NTMSTRUCT] Loaded structure "+structName);
						} else if (name.endsWith(".json")) {
							try {
								InputStream fileStream = zip.getInputStream(entry);
								InputStreamReader reader = new InputStreamReader(fileStream,StandardCharsets.UTF_8);
								JsonObject obj = (JsonObject) parser.parse(reader);
								String structName = name.replace("assets/leafia/structs/", "");
								addStructMeta(structName.substring(0,structName.length()-5),obj);
							} catch (Exception ex) {
								MainRegistry.logger.info("[NTMSTRUCT META] Error dissecting folder "+name+": "+ex);
							}
						}
					}
				}

			} catch(Exception ex) {
				MainRegistry.logger.info("[NTMSTRUCT] Error dissecting zip " + zipFile.getName() + ": " + ex);
			} finally {
				try {
					if(zip != null) zip.close();
				} catch(Exception ex) { }
			}
		}

		// thanks community edition
		public static void dissectFolder(File folder) {
			File manualFolder = new File(folder, "/assets/structs");
			if(manualFolder.exists() && manualFolder.isDirectory()) dissectStructFolder(manualFolder);
		}

		// thanks community edition
		public static void dissectStructFolder(File folder) {

			File[] files = folder.listFiles();
			for(File file : files) {
				String name = file.getName();
				if(file.isFile()) {
					if (name.endsWith(".ntmstruct")) {
						try {
							FileInputStream fileStream = new FileInputStream(file);
							byte[] bytes = new byte[fileStream.available()];
							fileStream.read(bytes);
							LeafiaBuf buf = new LeafiaBuf(null);
							buf.bytes = bytes;
							buf.writerIndex = bytes.length*8;
							fileStream.close();
							StructData data = structs.getOrDefault(name,new StructData());
							data.buf = buf;
							if (data.name == null)
								data.name = name;
							structs.putIfAbsent(name.substring(0,name.length()-10),data);
							MainRegistry.logger.info("[NTMSTRUCT] Loaded structure "+name);
						} catch (Exception ex) {
							MainRegistry.logger.info("[NTMSTRUCT] Error dissecting folder "+name+": "+ex);
						}
					} else if (name.endsWith(".json")) {
						try {
							FileInputStream fileStream = new FileInputStream(file);
							InputStreamReader reader = new InputStreamReader(fileStream,StandardCharsets.UTF_8);
							JsonObject obj = (JsonObject) parser.parse(reader);
							addStructMeta(name.substring(0,name.length()-5),obj);
						} catch (Exception ex) {
							MainRegistry.logger.info("[NTMSTRUCT META] Error dissecting folder "+name+": "+ex);
						}
					}
				} else if(file.isDirectory()) {
					dissectStructFolder(file); // scrape subfolders too lmao
				}
			}
		}
	}
	public final LeafiaBuf buf;
	public final int bitNeedle;
	public final Block[] paletteBlock;
	public final NBTTagCompound[] paletteTEs;
	public final Vec3i size;
	public final Vec3i offset;
	public final EnumFacing originalFace;
	public final NTMStructVersion version;
	public NTMStructAngle rotation = NTMStructAngle.ORIGINAL;
	public static NTMStructBuffer fromFiles(String path) {
		LeafiaBuf buffer = new LeafiaBuf(null);
		try {
			buffer.bytes = Files.readAllBytes(Paths.get(path));
			buffer.writerIndex = buffer.bytes.length*8;
		} catch (IOException exception) {
			LeafiaDevFlaw flaw = new LeafiaDevFlaw("Exception while tryina read "+path+" as .ntmstruct");
			flaw.setStackTrace(exception.getStackTrace());
			throw flaw;
		}
		return new NTMStructBuffer(buffer);
	}
	public static NTMStructBuffer fromMetadata(StructData data) {
		LeafiaBuf buffer = new LeafiaBuf(null);
		buffer.bytes = data.buf.bytes;
		buffer.writerIndex = data.buf.writerIndex;
		NTMStructBuffer struct = new NTMStructBuffer(buffer);
		struct.terrarinAware = data.terrarinAware;
		return struct;
	}
	public NTMStructBuffer(LeafiaBuf buf) {
		super();
		//System.out.println("Data size: "+buf.readableBits());
		version = NTMStructVersion.values()[buf.readUnsignedByte()];
		offset = buf.readVec3i();
		size = buf.readVec3i();
		originalFace = EnumFacing.byHorizontalIndex(buf.readByte());
		paletteBlock = new Block[buf.readUnsignedShort()];
		for (int i = 0; i < paletteBlock.length; i++) {
			boolean hasNext = true;
			while (hasNext) {
				FifthString fifth = buf.readFifthString();
				if (paletteBlock[i] == null) {
					String rid = LeafiaLib.stringSwap(fifth.toString(),' ',':');
					paletteBlock[i] = Block.getBlockFromName(rid);
					if (paletteBlock[i] == null && rid.startsWith("hbm:")) {
						rid = "leafia:"+rid.substring("hbm:".length());
						paletteBlock[i] = Block.getBlockFromName(rid);
					}
				}
				hasNext = buf.extract(1) > 0;
			}
		}
		if (version.isOlderThan(NTMStructVersion.V2_BETTER_TE_COMPRESSION)) {
			paletteTEs = new NBTTagCompound[buf.readUnsignedShort()];
			for (int i = 0; i < paletteTEs.length; i++) {
				byte[] bytes = new byte[buf.readInt()];
				buf.readBytes(bytes);
				ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
				try {
					paletteTEs[i] = CompressedStreamTools.readCompressed(stream);
				} catch (IOException ignored) {}
			}
		} else {
			byte[] bytes = new byte[buf.readInt()];
			if (bytes.length > 0) {
				buf.readBytes(bytes);
				ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
				try {
					NBTTagCompound attr = CompressedStreamTools.readCompressed(stream);
					NBTTagList entities = attr.getTagList("te",10);
					paletteTEs = new NBTTagCompound[entities.tagCount()];
					for (int i = 0; i < paletteTEs.length; i++)
						paletteTEs[i] = entities.getCompoundTagAt(i);
				} catch (IOException fatal) {
					LeafiaDevFlaw flaw = new LeafiaDevFlaw("Error trying to decode NBTStruct V2+ attributes");
					flaw.setStackTrace(fatal.getStackTrace());
					throw flaw;
				}
			} else {
				paletteTEs = new NBTTagCompound[0];
			}
		}
		this.buf = buf;
		bitNeedle = buf.readerIndex;
	}
	public NTMStructBuffer rotateToFace(EnumFacing face) {
		rotation = NTMStructAngle.values()[Math.floorMod(face.getHorizontalIndex()-originalFace.getHorizontalIndex(),4)];
		return this;
	}
	public boolean terrarinAware = false; // name idea: movblock
	public void build(World world,BlockPos origin) {
		buf.readerIndex = bitNeedle;
		BlockPos start = origin.add(rotation.getX(offset.getX(),offset.getZ()),offset.getY(),rotation.getZ(offset.getX(),offset.getZ()));
		int sx = size.getX()+1;
		int sy = size.getY()+1;
		int sz = size.getZ()+1;
		Map<BlockPos,NBTTagCompound> tebuffer = new LeafiaMap<>();
		SavingProperty property = null;
		int repeats = 0;
		for (int i = 0; i < sx*sy*sz; i++) {
			int x = Math.floorMod(i,sx);
			int y = Math.floorMod(i/sx,sy);
			int z = Math.floorMod(i/sx/sy,sz);
			BlockPos pos = start.add(rotation.getX(x,z),y,rotation.getZ(x,z));
			if (terrarinAware)
				pos = world.getHeight(pos).up(offset.getY()+y);
			if (repeats <= 0) {
				property = new SavingProperty();
				int value = buf.readUnsignedShort();
				int modifier = value>>>13&0b11;
				repeats = value&(1<<13)-1;
				if (modifier == 0b01)
					property.ignore = true;
				else {
					if (modifier == 0b10) property.replaceAirOnly = true;
					Block block = paletteBlock[buf.readUnsignedShort()];
					int meta = -1;
					if ((value>>>15&1) > 0)
						meta = version.isUpOrNewerThan(NTMStructVersion.V3_METADATA_FIX) ? buf.extract(4) : buf.readInt();
					if (block == null) {
						block = Blocks.AIR;
						property.state = block.getDefaultState();
					} else {
						if (meta != -1)
							property.state = block.getStateFromMeta(meta);
						else
							property.state = block.getDefaultState();
					}
					if (modifier == 0b11)
						property.entity = buf.readUnsignedShort();
					for (IProperty<?> key : property.state.getPropertyKeys()) {
						if (key instanceof PropertyDirection) {
							PropertyDirection cast = (PropertyDirection)key;
							EnumFacing facing = property.state.getValue(cast);
							if (facing.getYOffset() == 0 && rotation != NTMStructAngle.ORIGINAL) {
								switch(rotation) {
									case RIGHT: property.state = property.state.withProperty(cast,facing.rotateY()); break;
									case BACK: property.state = property.state.withProperty(cast,facing.getOpposite()); break;
									case LEFT: property.state = property.state.withProperty(cast,facing.getOpposite().rotateY()); break;
								}
							}
						}
					}
				}
			}
			if (!property.ignore) {
				Block block = property.state.getBlock();
				if (block instanceof LinedAsphaltBlock) {
					AsphaltLine line = new AsphaltLine(block);
					line = switch(rotation) {
						case RIGHT -> line.rotate();
						case BACK -> line.rotate().rotate();
						case LEFT -> line.rotate().rotate().rotate();
						default -> line;
					};
					Block newBlock = AsphaltLine.getBlock(line.toString());
					if (newBlock != null)
						world.setBlockState(pos,newBlock.getDefaultState(),0b00010);
				} else if (block instanceof BlockDummyable dummyable) {
					int meta = property.state.getValue(BlockDummyable.META);
					if (meta >= 12) {
						EnumFacing dir = ForgeDirection.getOrientation(meta-BlockDummyable.offset).toEnumFacing();
						dir = switch(rotation) {
							case RIGHT -> dir.rotateY();
							case BACK -> dir.getOpposite();
							case LEFT -> dir.getOpposite().rotateY();
							default -> dir;
						};
						world.setBlockState(pos,dummyable.getStateFromMeta(ForgeDirection.getOrientation(dir).ordinal()+BlockDummyable.offset),0b00010);
						try {
							// thanks movblock
							BlockPos pos1 = pos.offset(dir,-dummyable.getOffset());
							fillSpaceHandle.invokeExact((BlockDummyable) dummyable,(World) world,pos1.getX(),pos1.getY(),pos1.getZ(),ForgeDirection.getOrientation(dir),dummyable.getOffset());
						} catch (Throwable e) {
							throw new LeafiaDevFlaw(e);
						}
					}
				} else
					world.setBlockState(pos,property.state,0b00010);
				if (property.entity != null)
					tebuffer.put(pos,paletteTEs[property.entity]);
			}
			repeats--;
		}
		for (Entry<BlockPos,NBTTagCompound> entry : tebuffer.entrySet()) {
			TileEntity te = world.getTileEntity(entry.getKey());
			if (te != null) {
				BlockPos pos = te.getPos();
				te.deserializeNBT(entry.getValue());
				te.setPos(pos);
			} //else
				//throw new LeafiaDevFlaw("Tile entity could not be created");
		}
	}
}