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

	public static NamespacedKey KEY = new NamespacedKey(WlPlugin.instance, "compactified");

	public Compactified(Location[] structure) {
		Location a = structure[0], b = structure[1];
		width = b.getBlockX()-a.getBlockX()+1;
		height = b.getBlockY()-a.getBlockY()+1;
		length = b.getBlockZ()-a.getBlockZ()+1;
		data = new BlockData[length*height*width];
		int x = a.getBlockX(), y = a.getBlockY(), z = a.getBlockZ();
		World w  = a.getWorld();
		for(int i = 0; i < length; i++)
			for(int j = 0; j < height; j++)
				for(int k = 0; k < width; k++) {
					data[i*height*width+j*width+k] = w.getBlockAt(x+i, j+y, k+z).getBlockData();
				}
	}
	
	// this only exists because for some reason a call to another constructor must always be the first statement in a constructor.
	// why?
	// that doesn't even make sense on a JVM level; constructors are just functions named '<this>'.
	private void deserialize(String s) throws IllegalArgumentException {
		String[] rows = s.split("\n");
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
	}
	
	// deserializes a string
	public Compactified(String s) throws IllegalArgumentException {
		deserialize(s);
	}

	public Compactified(ItemStack is) throws IllegalArgumentException {
		PersistentDataContainer pdc = is.getItemMeta().getPersistentDataContainer();
		if(!pdc.has(KEY, PersistentDataType.STRING))
			throw new IllegalArgumentException("Item does not contain compacitifed structure information.");
		deserialize(pdc.get(KEY, PersistentDataType.STRING));
	}

	public String serialize() {
		StringBuilder sb = new StringBuilder(width+","+height+","+length);
		for(int i = 0; i < data.length; i++) {
			sb.append('\n');
			sb.append(data[i].getAsString());
		}
		return sb.toString();
	}

	public ItemStack toItem() {
		ItemStack is = new ItemStack(Material.STRUCTURE_VOID);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(Utils.escapeText("&3Compactified Structure"));
		im.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, serialize());
		is.setItemMeta(im);
		return is;
	}

	public void place(Location l) {
		float[] scale = new float[]{ 1f/width, 1f/height, 1f/length };
		for(int i = 0; i < length; i++)
			for(int j = 0; j < height; j++)
				for(int k = 0; k < width; k++) {
					BlockData d = data[i*height*width+j*width+k];
					// skip invisible blocks
					if(d.getMaterial() == Material.AIR || d.getMaterial() == Material.BARRIER)
						continue;
					BlockDisplay bd = (BlockDisplay)l.getWorld().spawnEntity(l.clone().add(i*scale[2], j*scale[1], k*scale[0]), EntityType.BLOCK_DISPLAY);
					bd.setBlock(d);
					Transformation t = bd.getTransformation();
					t.getScale().set(scale[0], scale[1], scale[2]);
					bd.setTransformation(t);
				}
	}
}
