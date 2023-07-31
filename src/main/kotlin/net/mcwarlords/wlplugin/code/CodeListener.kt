package net.mcwarlords.wlplugin.code;

import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.*;
import org.bukkit.*;
import org.bukkit.inventory.*;

import net.mcwarlords.wlplugin.*;

class CodeListener : Listener {
	private fun findType(item: ItemStack): CodeItem? {
		for(ci in CodeItem.values()) {
			if(ci.item == item)
				return ci;
		}
		return null;
	}

	@EventHandler fun onPlayerInteract(e: PlayerInteractEvent) {
		if(e.isCancelled())
			return;
		var p = e.player;
		var pd = Data.getPlayerData(p);
		if(!pd.codeMode)
			return;
		var block = e.clickedBlock!!;
		e.setCancelled(true);
		when(e.action) {
			Action.RIGHT_CLICK_BLOCK -> {
				if(block.type != Material.BLACK_STAINED_GLASS)
					return;
				var item = e.item;
				if(item == null)
					return;
				when(findType(item)) {
					CodeItem.LBRACE -> {
						block.type = Material.PISTON;
						var bd = block.blockData as Piston;
						bd.facing = BlockFace.NORTH;
						block.blockData = bd;
					}
					CodeItem.RBRACE -> {
						block.type = Material.PISTON;
						var bd = block.blockData as Piston;
						bd.facing = BlockFace.SOUTH;
						block.blockData = bd;
					}
					CodeItem.EVENT -> {
						block.type = Material.DIAMOND_BLOCK;
					}
					CodeItem.BUILTIN -> {
						block.type = Material.FURNACE;
						var bd = block.blockData as Directional;
						bd.facing = BlockFace.EAST;
						block.blockData = bd;
					}
					CodeItem.IF -> {
						block.type = Material.OAK_PLANKS
					}
					null -> return
				}
			}
			Action.LEFT_CLICK_BLOCK -> {
				if(block.getRelative(BlockFace.WEST).type == Material.BLACK_CONCRETE) {
					block.type = Material.BLACK_STAINED_GLASS;
					block.getRelative(BlockFace.EAST).type = Material.AIR;
					return;
				}
				if(block.getRelative(BlockFace.WEST, 2).type == Material.BLACK_CONCRETE && block.type == Material.OAK_SIGN) {
					block.type = Material.AIR;
					block.getRelative(BlockFace.WEST).type = Material.BLACK_STAINED_GLASS;
				}
			}
			else -> {}
		}
	}
}
