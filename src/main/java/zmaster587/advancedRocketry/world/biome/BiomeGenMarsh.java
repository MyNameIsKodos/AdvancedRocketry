package zmaster587.advancedRocketry.world.biome;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenShrub;

public class BiomeGenMarsh extends Biome {

	public BiomeGenMarsh(int id, boolean b) {
		super(new BiomeProperties("Marsh").setBaseHeight(-0.4f).setHeightVariation(0f));
		
		registerBiome(id, "Marsh", this);
		
		this.theBiomeDecorator.clayPerChunk = 10;
		this.theBiomeDecorator.flowersPerChunk = 0;
		this.theBiomeDecorator.mushroomsPerChunk = 0;
		this.theBiomeDecorator.treesPerChunk = 0;
		this.theBiomeDecorator.grassPerChunk = 0;
		this.theBiomeDecorator.waterlilyPerChunk = 10;
		this.theBiomeDecorator.sandPerChunk = 0;
		this.theBiomeDecorator.sandPerChunk2 = 0;
		
		this.spawnableCreatureList.clear();
	}

	@Override
	public void genTerrainBlocks(World worldIn, Random rand,
			ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal) {
		super.genTerrainBlocks(worldIn, rand, chunkPrimerIn, x, z, noiseVal);

		double d1 = GRASS_COLOR_NOISE.getValue((double)x * 0.25D, (double)z * 0.25D);
		x = Math.abs(x % 16);
		z = Math.abs(z % 16);
		if (d1 > 0.2D)
		{
			chunkPrimerIn.setBlockState(x, 62, z, Blocks.GRASS.getDefaultState());
			for(int y = (int)(61); y > 1; y--) {
				
				if(!chunkPrimerIn.getBlockState(x, y, z).isOpaqueCube())
					chunkPrimerIn.setBlockState(x, y, z, Blocks.GRASS.getDefaultState());
				else
					break;
			}
		}
	}
	
	@Override
	public WorldGenAbstractTree genBigTreeChance(Random rand) {
		return new WorldGenShrub(Blocks.LOG.getDefaultState(), Blocks.LEAVES.getDefaultState());
	}
}
