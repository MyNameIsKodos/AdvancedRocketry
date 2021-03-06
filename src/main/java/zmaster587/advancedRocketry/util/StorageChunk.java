/* Temporarily stores tile/blocks to move a block of them
 * 
 * 
 */

package zmaster587.advancedRocketry.util;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.stations.IStorageChunk;
import zmaster587.advancedRocketry.tile.TileGuidanceComputer;
import zmaster587.advancedRocketry.tile.Satellite.TileSatelliteHatch;
import zmaster587.advancedRocketry.world.util.WorldDummy;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

public class StorageChunk implements IBlockAccess, IStorageChunk {

	Block blocks[][][];
	short metas[][][];
	int sizeX, sizeY, sizeZ;


	ArrayList<TileEntity> tileEntities;

	//To store inventories (just chests right now)
	ArrayList<TileEntity> usableTiles;

	public WorldDummy world;

	public StorageChunk() {
		sizeX = 0;
		sizeY = 0;
		sizeZ = 0;
		tileEntities = new ArrayList<TileEntity>();
		usableTiles = new ArrayList<TileEntity>();

		world = new WorldDummy(AdvancedRocketry.proxy.getProfiler(), this);
	}

	protected StorageChunk(int xSize, int ySize, int zSize) {
		blocks = new Block[xSize][ySize][zSize];
		metas = new short[xSize][ySize][zSize];

		sizeX = xSize;
		sizeY = ySize;
		sizeZ = zSize;

		tileEntities = new ArrayList<TileEntity>();
		usableTiles = new ArrayList<TileEntity>();

		world = new WorldDummy(AdvancedRocketry.proxy.getProfiler(), this);
	}

	@Override
	public int getSizeX() { return sizeX; }
	
	@Override
	public int getSizeY() { return sizeY; }
	
	@Override
	public int getSizeZ() { return sizeZ; }

	public List<TileEntity> getTileEntityList() {

		return tileEntities;
	}

	public List<TileEntity> getUsableTiles() {
		return usableTiles;
	}

	@Override
	public Block getBlock(int x, int y, int z) {
		if(x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ)
			return Blocks.air;

		if(blocks[x][y][z] != Blocks.air)
			return blocks[x][y][z];
		return blocks[x][y][z];
	}


	//TODO: optimize the F*** out of this
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("xSize", sizeX);
		nbt.setInteger("ySize", sizeY);
		nbt.setInteger("zSize", sizeZ);
		
		
		Iterator<TileEntity> tileEntityIterator = tileEntities.iterator();
		NBTTagList tileList = new NBTTagList();
		while(tileEntityIterator.hasNext()) {
			TileEntity tile = tileEntityIterator.next();
			try {
				NBTTagCompound tileNbt = new NBTTagCompound();
				tile.writeToNBT(tileNbt);
				tileList.appendTag(tileNbt);
			} catch(RuntimeException e) {
				AdvancedRocketry.logger.warning("A tile entity has thrown an error: " + tile.getClass().getCanonicalName());
				blocks[tile.xCoord][tile.yCoord][tile.zCoord] = Blocks.air;
				metas[tile.xCoord][tile.yCoord][tile.zCoord] = 0;
				tileEntityIterator.remove();
			}
		}

		int[] blockId = new int[sizeX*sizeY*sizeZ];
		int[] metasId = new int[sizeX*sizeY*sizeZ];
		for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {
					blockId[z + (sizeZ*y) + (sizeZ*sizeY*x)] = Block.getIdFromBlock(blocks[x][y][z]);
					metasId[z + (sizeZ*y) + (sizeZ*sizeY*x)] = (int)metas[x][y][z];
				}
			}
		}

		NBTTagIntArray idList = new NBTTagIntArray(blockId);
		NBTTagIntArray metaList = new NBTTagIntArray(metasId);
		
		nbt.setTag("idList", idList);
		nbt.setTag("metaList", metaList);
		nbt.setTag("tiles", tileList);


		/*for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {

					idList.appendTag(new NBTTagInt(Block.getIdFromBlock(blocks[x][y][z])));
					metaList.appendTag(new NBTTagInt(metas[x][y][z]));

					//NBTTagCompound tag = new NBTTagCompound();
					tag.setInteger("block", Block.getIdFromBlock(blocks[x][y][z]));
					tag.setShort("meta", metas[x][y][z]);

					NBTTagCompound tileNbtData = null;

					for(TileEntity tile : tileEntities) {
						NBTTagCompound tileNbt = new NBTTagCompound();

						tile.writeToNBT(tileNbt);

						if(tileNbt.getInteger("x") == x && tileNbt.getInteger("y") == y && tileNbt.getInteger("z") == z){
							tileNbtData = tileNbt;
							break;
						}
					}

					if(tileNbtData != null)
						tag.setTag("tile", tileNbtData);

					nbt.setTag(String.format("%d.%d.%d", x,y,z), tag);
				}

			}
		}*/
	}


	private static boolean isUsableBlock(TileEntity tile) {
		return tile instanceof IInventory;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		sizeX = nbt.getInteger("xSize");
		sizeY = nbt.getInteger("ySize");
		sizeZ = nbt.getInteger("zSize");

		blocks = new Block[sizeX][sizeY][sizeZ];
		metas = new short[sizeX][sizeY][sizeZ];

		tileEntities.clear();
		usableTiles.clear();

		int[] blockId = nbt.getIntArray("idList");
		int[] metasId = nbt.getIntArray("metaList");

		for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {
					blocks[x][y][z] = Block.getBlockById(blockId[z + (sizeZ*y) + (sizeZ*sizeY*x)]);
					metas[x][y][z] = (short)metasId[z + (sizeZ*y) + (sizeZ*sizeY*x)];
				}
			}
		}

		NBTTagList tileList = nbt.getTagList("tiles", NBT.TAG_COMPOUND);

		for(int i = 0; i < tileList.tagCount(); i++) {

			try {
				TileEntity tile = TileEntity.createAndLoadEntity(tileList.getCompoundTagAt(i));
				tile.setWorldObj(world);

				if(isUsableBlock(tile)) {
					usableTiles.add(tile);
				}

				tileEntities.add(tile);
			} catch (Exception e) {
				AdvancedRocketry.logger.warning("Rocket missing Tile (was a mod removed?)");
			}

		}

		/*for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {



					NBTTagCompound tag = (NBTTagCompound)nbt.getTag(String.format("%d.%d.%d", x,y,z));

					if(!tag.hasKey("block"))
						continue;
					int blockId = tag.getInteger("block"); 
					blocks[x][y][z] = Block.getBlockById(blockId);
					metas[x][y][z] = tag.getShort("meta");


					if(blockId != 0 && blocks[x][y][z] == Blocks.air) {
						AdvancedRocketry.logger.warning("Removed pre-existing block with id " + blockId + " from a rocket (Was a mod removed?)");
					}
					else if(tag.hasKey("tile")) {

						if(blocks[x][y][z].hasTileEntity(metas[x][y][z])) {
							TileEntity tile = TileEntity.createAndLoadEntity(tag.getCompoundTag("tile"));
							tile.setWorldObj(world);

							tileEntities.add(tile);

							//Machines would throw a wrench in the works
							if(isUsableBlock(tile)) {
								inventories.add((IInventory)tile);
								usableTiles.add(tile);
							}
						}
					}
				}
			}
		}*/

	}

	public static StorageChunk copyWorldBB(World world, AxisAlignedBB bb) {
		int actualMinX = (int)bb.maxX,
				actualMinY = (int)bb.maxY,
				actualMinZ = (int)bb.maxZ,
				actualMaxX = (int)bb.minX,
				actualMaxY = (int)bb.minY,
				actualMaxZ = (int)bb.minZ;


		//Try to fit to smallest bounds
		for(int x = (int)bb.minX; x <= bb.maxX; x++) {
			for(int z = (int)bb.minZ; z <= bb.maxZ; z++) {
				for(int y = (int)bb.minY; y<= bb.maxY; y++) {

					Block block = world.getBlock(x, y, z);

					if(!block.isAir(world, x, y, z)) {
						if(x < actualMinX)
							actualMinX = x;
						if(y < actualMinY)
							actualMinY = y;
						if(z < actualMinZ)
							actualMinZ = z;
						if(x > actualMaxX)
							actualMaxX = x;
						if(y > actualMaxY)
							actualMaxY = y;
						if(z > actualMaxZ)
							actualMaxZ = z;
					}
				}
			}
		}


		bb.setBounds(actualMinX, actualMinY, actualMinZ, actualMaxX, actualMaxY, actualMaxZ);

		StorageChunk ret = new StorageChunk((actualMaxX - actualMinX + 1), (actualMaxY - actualMinY + 1), (actualMaxZ - actualMinZ + 1));


		//Iterate though the bounds given storing blocks/meta/tiles
		for(int x = actualMinX; x <= actualMaxX; x++) {
			for(int z = actualMinZ; z <= actualMaxZ; z++) {
				for(int y = actualMinY; y<= actualMaxY; y++) {


					ret.blocks[x - actualMinX][y - actualMinY][z - actualMinZ] = world.getBlock(x, y, z);
					ret.metas[x - actualMinX][y - actualMinY][z - actualMinZ] = (short)world.getBlockMetadata(x, y, z);

					TileEntity entity = world.getTileEntity(x, y, z);
					if(entity != null) {
						NBTTagCompound nbt = new NBTTagCompound();
						entity.writeToNBT(nbt);

						//Transform tileEntity coords
						nbt.setInteger("x",nbt.getInteger("x") - actualMinX);
						nbt.setInteger("y",nbt.getInteger("y") - actualMinY);
						nbt.setInteger("z",nbt.getInteger("z") - actualMinZ);

						TileEntity newTile = TileEntity.createAndLoadEntity(nbt);

						newTile.setWorldObj(world);

						if(isUsableBlock(newTile)) {
							ret.usableTiles.add(newTile);
						}

						ret.tileEntities.add(newTile);
					}
				}
			}
		}

		return ret;
	}

	//pass the coords of the xmin, ymin, zmin as well as the world to move the rocket
	@Override
	public void pasteInWorld(World world, int xCoord, int yCoord ,int zCoord) {

		//Set all the blocks
		for(int x = 0; x < sizeX; x++) {
			for(int z = 0; z < sizeZ; z++) {
				for(int y = 0; y< sizeY; y++) {

					if(blocks[x][y][z] != null)
						world.setBlock(xCoord + x, yCoord + y, zCoord + z, blocks[x][y][z], metas[x][y][z], 2);
				}
			}
		}

		//Set tiles for each block
		for(TileEntity tile : tileEntities) {
			NBTTagCompound nbt = new NBTTagCompound();
			tile.writeToNBT(nbt);
			int x = nbt.getInteger("x");
			int y = nbt.getInteger("y");
			int z = nbt.getInteger("z");

			int tmpX = x + xCoord;
			int tmpY = y + yCoord;
			int tmpZ = z + zCoord;

			//Set blocks of tiles again to avoid weirdness caused by updates
			//world.setBlock(xCoord + x, yCoord + y, zCoord + z, blocks[x][y][z], metas[x][y][z], 2);


			nbt.setInteger("x",tmpX);
			nbt.setInteger("y",tmpY);
			nbt.setInteger("z",tmpZ);

			TileEntity entity = world.getTileEntity(tmpX, tmpY, tmpZ);

			if(entity != null)
				entity.readFromNBT(nbt);
		}
	}


	@Override
	public TileEntity getTileEntity(int x, int y,
			int z) {
		for(TileEntity tileE : tileEntities) {
			if( tileE.xCoord == x &&  tileE.yCoord == y &&  tileE.zCoord == z)
				return tileE;
		}
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getLightBrightnessForSkyBlocks(int x, int y,
			int z, int meta) {
		Entity ent = Minecraft.getMinecraft().renderViewEntity;
		return Minecraft.getMinecraft().theWorld.getLightBrightnessForSkyBlocks((int)ent.posX, (int)ent.posY, (int)ent.posZ, meta);
	}

	@Override
	public int getBlockMetadata(int x, int y, int z) {
		// Need bounds check... Thank you renderBlockStairs
		if(x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ )
			return 0;
		return metas[x][y][z];
	}

	@Override
	public int isBlockProvidingPowerTo(int p_72879_1_, int p_72879_2_,
			int p_72879_3_, int p_72879_4_) {
		return 0;
	}

	@Override
	public boolean isAirBlock(int x, int y, int z) {
		if(x >= blocks.length || y >= blocks[0].length || z >= blocks[0][0].length)
			return true;
		return blocks[x][y][z] == Blocks.air;
	}

	@Override
	public BiomeGenBase getBiomeGenForCoords(int p_72807_1_, int p_72807_2_) {
		return BiomeGenBase.ocean;
	}

	@Override
	public int getHeight() {
		return sizeY;
	}

	@Override
	public boolean extendedLevelsInChunkCache() {
		return false;
	}

	@Override
	public boolean isSideSolid(int x, int y, int z, ForgeDirection side,
			boolean _default) {

		if(x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ  || x + side.offsetX < 0 || x + side.offsetX >= sizeX || y + side.offsetY < 0 || y + side.offsetY >= sizeY || z + side.offsetZ < 0 || z + side.offsetZ >= sizeZ)
			return false;

		return blocks[x + side.offsetX][y + side.offsetY][z + side.offsetZ].isBlockSolid(this, x, y, z, metas[x][y][z]);
	}

	public static StorageChunk cutWorldBB(World worldObj, AxisAlignedBB bb) {
		StorageChunk chunk = StorageChunk.copyWorldBB(worldObj, bb);
		for(int x = (int)bb.minX; x <= bb.maxX; x++) {
			for(int z = (int)bb.minZ; z <= bb.maxZ; z++) {
				for(int y = (int)bb.minY; y<= bb.maxY; y++) {

					//Workaround for dupe
					TileEntity tile = worldObj.getTileEntity(x, y, z);
					if(tile instanceof IInventory) {
						IInventory inv = (IInventory) tile;
						for(int i = 0; i < inv.getSizeInventory(); i++) {
							inv.setInventorySlotContents(i, null);
						}
					}

					worldObj.setBlock(x, y, z, Blocks.air, 0, 2);
				}
			}
		}

		//Carpenter's block's dupe
		for(Object entity : worldObj.getEntitiesWithinAABB(EntityItem.class, bb.expand(5, 5, 5)) ) {
			((Entity)entity).setDead();
		}

		return chunk;
	}


	
	public List<TileSatelliteHatch> getSatelliteHatches() {
		LinkedList<TileSatelliteHatch> satelliteHatches = new LinkedList<TileSatelliteHatch>();
		Iterator<TileEntity> iterator = getTileEntityList().iterator();
		while(iterator.hasNext()) {
			TileEntity tile = iterator.next();

			if(tile instanceof TileSatelliteHatch) {
				satelliteHatches.add((TileSatelliteHatch) tile);
			}
		}
		
		return satelliteHatches;
	}
	
	@Deprecated
	public List<SatelliteBase> getSatellites() {
		LinkedList<SatelliteBase> satellites = new LinkedList<SatelliteBase>();
		LinkedList<TileSatelliteHatch> satelliteHatches = new LinkedList<TileSatelliteHatch>();
		Iterator<TileEntity> iterator = getTileEntityList().iterator();
		while(iterator.hasNext()) {
			TileEntity tile = iterator.next();

			if(tile instanceof TileSatelliteHatch) {
				satelliteHatches.add((TileSatelliteHatch) tile);
			}
		}


		for(TileSatelliteHatch tile : satelliteHatches) {
			SatelliteBase satellite = tile.getSatellite();
			if(satellite != null)
				satellites.add(satellite);
		}
		return satellites;
	}
	
	public TileGuidanceComputer getGuidanceComputer() {
		Iterator<TileEntity> iterator = getTileEntityList().iterator();
		while(iterator.hasNext()) {
			TileEntity tile = iterator.next();

			if(tile instanceof TileGuidanceComputer) {
				return (TileGuidanceComputer)tile;
			}
		}

		return null;
	}

	public void writeToNetwork(ByteBuf out) {
		PacketBuffer buffer = new PacketBuffer(out);

		buffer.writeByte(this.sizeX);
		buffer.writeByte(this.sizeY);
		buffer.writeByte(this.sizeZ);
		buffer.writeShort(tileEntities.size());

		for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {
					buffer.writeInt(Block.getIdFromBlock(this.blocks[x][y][z]));
					buffer.writeShort(this.metas[x][y][z]);
				}
			}
		}

		Iterator<TileEntity> tileIterator = tileEntities.iterator();
		
		while(tileIterator.hasNext()) {
			TileEntity tile = tileIterator.next();
			
			NBTTagCompound nbt = new NBTTagCompound();
			
			try {
				tile.writeToNBT(nbt);
				
				try {
					buffer.writeNBTTagCompoundToBuffer(nbt);
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			} catch(RuntimeException e) {
				AdvancedRocketry.logger.warning("A tile entity has thrown an error while writing to network: " + tile.getClass().getCanonicalName());
				tileIterator.remove();
			}
		}
	}

	public void readFromNetwork(ByteBuf in) {
		PacketBuffer buffer = new PacketBuffer(in);

		this.sizeX = buffer.readByte();
		this.sizeY = buffer.readByte();
		this.sizeZ = buffer.readByte();
		short numTiles = buffer.readShort();

		this.blocks = new Block[sizeX][sizeY][sizeZ];
		this.metas = new short[sizeX][sizeY][sizeZ];

		for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				for(int z = 0; z < sizeZ; z++) {
					this.blocks[x][y][z] = Block.getBlockById(buffer.readInt());
					this.metas[x][y][z] = buffer.readShort();
				}
			}
		}

		for(short i = 0; i < numTiles; i++) {
			try {
				NBTTagCompound nbt = buffer.readNBTTagCompoundFromBuffer();

				TileEntity tile = TileEntity.createAndLoadEntity(nbt);
				tile.setWorldObj(world);
				tileEntities.add(tile);

				if(isUsableBlock(tile)) {
					usableTiles.add(tile);
				}

			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
