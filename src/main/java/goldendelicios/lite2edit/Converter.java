// Not written by me! this is from https://github.com/GoldenDelicios/Lite2Edit

package goldendelicios.lite2edit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.ShortTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

public class Converter {

	public static List<File> litematicToWorldEdit(File inputFile, File outputDir) throws IOException {
		DataInputStream inStream = new DataInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
		Tag litematica = CompoundTag.read(inStream).get("");
		inStream.close();
		int dataVersion = litematica.get("MinecraftDataVersion").intValue();
		
		List<File> files = new ArrayList<>();
		CompoundTag regions = litematica.get("Regions").asCompound();
		for (NamedTag regionTag : regions) {
			CompoundTag region = regionTag.asCompound();
			ListTag palette = region.get("BlockStatePalette").asList();
			int bitsPerBlock = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
			
			// Litematica dimensions can be negative.
			Tag size = region.get("Size");
			int x = size.get("x").intValue();
			int y = size.get("y").intValue();
			int z = size.get("z").intValue();
			
			// get offset
			Tag position = region.get("Position");
			int offsetx = position.get("x").intValue() + (x < 0 ? x+1 : 0);
			int offsety = position.get("y").intValue() + (y < 0 ? y+1 : 0);
			int offsetz = position.get("z").intValue() + (z < 0 ? z+1 : 0);
			
			int numBlocks = Math.abs(x * y * z);
			short[] liteBlocks = new short[numBlocks];
			long bitmask, bits = 0;
			int i = 0, bitCount = 0;
			for (long num : region.get("BlockStates").longArray()) {
				int remainingBits = bitCount + 64;
				if (bitCount != 0) {
					bitmask = (1 << (bitsPerBlock - bitCount)) - 1;
					long newBits = (num & bitmask) << bitCount;
					bits = bits | newBits;
					num = num >>> (bitsPerBlock - bitCount);
					remainingBits -= bitsPerBlock;
					liteBlocks[i++] = (short) bits;
				}
				
				bitmask = (1 << bitsPerBlock) - 1;
				while (remainingBits >= bitsPerBlock) {
					bits = num & bitmask;
					num = num >>> bitsPerBlock;
					remainingBits -= bitsPerBlock;
					if (i >= liteBlocks.length)
						break;
					liteBlocks[i++] = (short) bits;
				}
				bits = num;
				bitCount = remainingBits;
			}
			
			i = 0;
			String[] blockPalette = new String[palette.size()];
			for (SpecificTag blockState : palette) {
				String name = blockState.get("Name").stringValue();
				CompoundTag properties = blockState.get("Properties").asCompound();
				if (!properties.isEmpty()) {
					List<String> propertyNames = new ArrayList<>();
					for (NamedTag property : properties) {
						propertyNames.add(property.name() + "=" + property.unpack().stringValue());
					}
					name += "[" + String.join(",", propertyNames) + "]";
				}
				blockPalette[i++] = name;
			}
			
			/*
			 * Convert to WorldEdit format now
			 */
			// Convert block data
			byte[] weBlocks = new byte[liteBlocks.length * 2];
			i = 0;
			for (short block : liteBlocks) {
				if (block >= palette.size())
					throw new IllegalArgumentException("Something's wrong with the palette");
				
				if (block > 127) {
					weBlocks[i++] = (byte) (block | 128);
					weBlocks[i++] = (byte) (block / 128);
				}
				else {
					weBlocks[i++] = (byte) block;
				}
			}
			weBlocks = Arrays.copyOf(weBlocks, i);
			
			// Convert palette
			CompoundTag wePalette = new CompoundTag();
			for (i = 0; i < blockPalette.length; ++i) {
				wePalette.add(blockPalette[i], new IntTag(i));
			}
			
			// Copy tile entity data
			List<CompoundTag> weTileEntities = new ArrayList<>();
			for (SpecificTag tileEntity : region.get("TileEntities").asList()) {
				CompoundTag liteTileEntity = tileEntity.asCompound();
				CompoundTag weTileEntity = new CompoundTag();
				
				// Litematica uses integer "x", "y", and "z" tags
				// WorldEdit uses one integer array "Pos" tag
				int tx = liteTileEntity.get("x").intValue();
				int ty = liteTileEntity.get("y").intValue();
				int tz = liteTileEntity.get("z").intValue();
				weTileEntity.add("Pos", new IntArrayTag(new int[] {tx, ty, tz}));
				
				// Litematica uses a lowercase "id"
				// WorldEdit uses a capitalized "Id"
				String tid = liteTileEntity.get("id").stringValue();
				weTileEntity.add("Id", new StringTag(tid));
				
				List<String> skip = Arrays.asList("x", "y", "z", "id");
				for (NamedTag tileEntityTag : liteTileEntity) {
					String name = tileEntityTag.name();
					if (!skip.contains(name))
						weTileEntity.add(tileEntityTag);
				}
				weTileEntities.add(weTileEntity);
			}
			
			// metadata
			CompoundTag metadata = new CompoundTag();
			metadata.add("WEOffsetX", new IntTag(offsetx));
			metadata.add("WEOffsetY", new IntTag(offsety));
			metadata.add("WEOffsetZ", new IntTag(offsetz));
			
			CompoundTag worldEdit = new CompoundTag();
			worldEdit.add(new NamedTag("Metadata", metadata));
			worldEdit.add(new NamedTag("Palette", wePalette));
			worldEdit.add(new NamedTag("BlockEntities", new ListTag(Tag.TAG_COMPOUND, weTileEntities)));
			worldEdit.add(new NamedTag("DataVersion", new IntTag(dataVersion)));
			worldEdit.add(new NamedTag("Height", new ShortTag((short) Math.abs(y))));
			worldEdit.add(new NamedTag("Length", new ShortTag((short) Math.abs(z))));
			worldEdit.add(new NamedTag("PaletteMax", new IntTag(wePalette.size())));
			worldEdit.add(new NamedTag("Version", new IntTag(2)));
			worldEdit.add(new NamedTag("Width", new ShortTag((short) Math.abs(x))));
			worldEdit.add(new NamedTag("BlockData", new ByteArrayTag(weBlocks)));
			worldEdit.add(new NamedTag("Offset", new IntArrayTag(new int[3])));
			
			CompoundTag worldEditRoot = new CompoundTag();
			worldEditRoot.add("Schematic", worldEdit);
			
			// determine outputFileName
			String outputFileName = inputFile.getName();
			if (outputFileName.contains(".")) {
				outputFileName = outputFileName.substring(0, outputFileName.lastIndexOf('.'));
			}
			if (regions.size() > 1) {
				outputFileName += "-" + regionTag.name();
			}
			outputFileName = outputFileName.replace(' ', '_') + ".schem";
			
			// make sure directory exists, and write to the provided path
			Files.createDirectories(outputDir.toPath());
			File outputFile = new File(outputDir + "/" + outputFileName);
			DataOutputStream outStream = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(outputFile)));
			worldEditRoot.write(outStream);
			outStream.close();
			files.add(outputFile);
		}
		
		return files;
	}

}
