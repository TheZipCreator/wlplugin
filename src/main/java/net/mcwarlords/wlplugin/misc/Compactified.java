package net.mcwarlords.wlplugin.misc;

import org.bukkit.block.data.*;
import org.bukkit.persistence.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;
import java.util.*;

import net.mcwarlords.wlplugin.*;

/** Represents a compactified structure */
public class Compactified {
	public int width, height, length;
	BlockData[] data;
	public boolean solid = true; // true = barrier, false = structure void
	public boolean old = false; // whether this uses the old format

	public static NamespacedKey DATA_KEY = new NamespacedKey(WlPlugin.instance, "compactified");
	public static NamespacedKey INFO_KEY = new NamespacedKey(WlPlugin.instance, "compactified-info");

	public Compactified(Location[] structure) {
		Location a = structure[0], b = structure[1];
		width = b.getBlockX()-a.getBlockX()+1;
		height = b.getBlockY()-a.getBlockY()+1;
		length = b.getBlockZ()-a.getBlockZ()+1;
		data = new BlockData[length*height*width];
		int x = a.getBlockX(), y = a.getBlockY(), z = a.getBlockZ();
		World w  = a.getWorld();
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height; j++)
				for(int k = 0; k < length; k++) {
					data[k*height*width+j*width+i] = w.getBlockAt(x+i, j+y, z+k).getBlockData();
				}
	}
	
	private void deserialize(String sdata, String info) throws IllegalArgumentException {
		// load data
		String[] rows = sdata.split("\n");
		try {
			String[] size = rows[0].split(",");
			if(size.length != 3)
				throw new IllegalArgumentException("Invalid structure size.");
			width = Integer.parseInt(size[0]);
			height = Integer.parseInt(size[1]);
			length = Integer.parseInt(size[2]);
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Structure sizes are not integers.");
		}
		if(width*height*length != rows.length-1)
			throw new IllegalArgumentException("Structure size does not match data length.");
		data = new BlockData[width*height*length];
		try {
			for(int i = 1; i < rows.length; i++) {
				data[i-1] = WlPlugin.instance.getServer().createBlockData(rows[i]);
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Error loading block data: "+e.getMessage());
		}
		// load info
		if(info == null) {
			solid = true;
			old = true;
			return;
		}
		rows = info.split("\n");
		if(rows.length < 1)
			throw new IllegalArgumentException("Invalid structure info.");
		old = false;
		switch(rows[0]) {
			case "0": {
				solid = rows[1].equals("true");
				break;
			}
			default:
				throw new IllegalArgumentException("Unrecognized info format version: "+rows[0]);
		}
	}

	public static boolean is(ItemStack is) {
		return is.getItemMeta().getPersistentDataContainer().has(Compactified.DATA_KEY, PersistentDataType.STRING);

	}

	public Compactified(ItemStack is) throws IllegalArgumentException {
		PersistentDataContainer pdc = is.getItemMeta().getPersistentDataContainer();
		if(!pdc.has(DATA_KEY, PersistentDataType.STRING))
			throw new IllegalArgumentException("Item does not contain compactifed structure information.");
		if(!pdc.has(INFO_KEY, PersistentDataType.STRING)) {
			deserialize(pdc.get(DATA_KEY, PersistentDataType.STRING), null);
			return;
		}
		deserialize(pdc.get(DATA_KEY, PersistentDataType.STRING), pdc.get(INFO_KEY, PersistentDataType.STRING));
	}
	
	// returns data then info
	public String[] serialize() {
		StringBuilder datasb = new StringBuilder(width+","+height+","+length);
		for(int i = 0; i < data.length; i++) {
			datasb.append('\n');
			datasb.append(data[i].getAsString());
		}
		StringBuilder infosb = new StringBuilder("0\n");
		infosb.append(solid);
		infosb.append('\n');
		return new String[]{datasb.toString(), infosb.toString()};
	}

	public ItemStack toItem() {
		ItemStack is = new ItemStack(Material.STRUCTURE_VOID);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(Utils.escapeText("&3Compactified Structure"));
		String[] s = serialize();
		im.getPersistentDataContainer().set(DATA_KEY, PersistentDataType.STRING, s[0]);
		im.getPersistentDataContainer().set(INFO_KEY, PersistentDataType.STRING, s[1]);
		is.setItemMeta(im);
		return is;
	}

	public void place(Location l) {
		float[] scale = new float[]{ 1f/width, 1f/height, 1f/length };
		if(old) {
			// kinda copypaste but wtv
			for(int i = 0; i < length; i++)
				for(int j = 0; j < height; j++)
					for(int k = 0; k < width; k++) {
						BlockData d = data[i*height*width+j*width+k];
						// skip invisible blocks
						if(d.getMaterial() == Material.AIR || d.getMaterial() == Material.BARRIER)
							continue;
						BlockDisplay bd = (BlockDisplay)l.getWorld().spawnEntity(l.clone().add(i*scale[0], j*scale[1], k*scale[2]), EntityType.BLOCK_DISPLAY);
						bd.setBlock(d);
						Transformation t = bd.getTransformation();
						t.getScale().set(scale[0], scale[1], scale[2]);
						bd.setTransformation(t);
					}
			return;
		}
		for(int i = 0; i < width; i++)
			for(int j = 0; j < height; j++)
				for(int k = 0; k < length; k++) {
					BlockData d = data[k*height*width+j*width+i];
					// skip invisible blocks
					if(d.getMaterial() == Material.AIR || d.getMaterial() == Material.BARRIER)
						continue;
					BlockDisplay bd = (BlockDisplay)l.getWorld().spawnEntity(l.clone().add(i*scale[0], j*scale[1], k*scale[2]), EntityType.BLOCK_DISPLAY);
					bd.setBlock(d);
					Transformation t = bd.getTransformation();
					t.getScale().set(scale[0], scale[1], scale[2]);
					bd.setTransformation(t);
				}
	}
}
