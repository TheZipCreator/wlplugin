package net.mcwarlords.wlplugin.code;

import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.*;
import org.bukkit.block.sign.*;
import org.bukkit.block.Sign;
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
	companion object {
		private fun makeSign(b: Block, ln0: String, ln1: String = "", ln2: String = "", ln3: String = "") {
			var b2 = b.getRelative(BlockFace.EAST);
			b2.type = Material.OAK_WALL_SIGN;
			var bd = b2.blockData as WallSign;
			bd.facing = BlockFace.EAST;
			b2.blockData = bd;
			var sign = b2.state as Sign;
			/* 
			when we update to 1.20, this will replace the other code:
			var front = sign.getSide(Side.FRONT);
			front.setLine(0, Utils.escapeText(ln0));
			front.setLine(1, Utils.escapeText(ln1));
			front.setLine(2, Utils.escapeText(ln2));
			front.setLine(3, Utils.escapeText(ln3));
			*/
			sign.setLine(0, Utils.escapeText(ln0));
			sign.setLine(1, Utils.escapeText(ln1));
			sign.setLine(2, Utils.escapeText(ln2));
			sign.setLine(3, Utils.escapeText(ln3));
			sign.update(true);
			// update the sign again later.
			// I believe I have to do this because the client doesn't realize it's a sign immediately?
			Bukkit.getScheduler().runTaskLater(WlPlugin.instance, Runnable {
				sign.update(true);
			}, 5);
		}
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
			Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
				if(block.type != Material.BLACK_STAINED_GLASS)
					return;
				var item = e.item;
				if(item == null)
					return;
				if(item == blocksItem) {
					var inv = Bukkit.createInventory(null, 18, "Code Blocks");
					for(item in CodeItem.values()) {
						inv.addItem(item.item);
					}
					p.openInventory(inv);
					return;
				}
				if(e.action == Action.RIGHT_CLICK_AIR)
					return;
				fun makeInputSign(name: String) {
					makeSign(block, "Input name for", "$name in chat");
					Utils.getInput(p, { makeSign(block, "&l${name.uppercase()}", it) });
				}
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
						makeInputSign("event");
					}
					CodeItem.BUILTIN -> {
						block.type = Material.FURNACE;
						var bd = block.blockData as Directional;
						bd.facing = BlockFace.EAST;
						block.blockData = bd;
						makeInputSign("builtin");
					}
					CodeItem.IF -> {
						block.type = Material.OAK_PLANKS
						makeSign(block, "&lIF");
					}
					CodeItem.VARIABLE -> {
						block.type = Material.OBSIDIAN;
						makeInputSign("variable");
					}
					CodeItem.STRING -> {
						block.type = Material.WHITE_WOOL;
						makeSign(block, "Input string in", "chat");
						Utils.getInput(p, {
							// split a string into multiple blocks.
							// this code feels ugly there's probably a better way to do it
							var l = block.location;
							var width = 0;
							var lines = mutableListOf<String>();
							var start = 0;
							fun add() {
								var b = l.block;
								b.type = Material.WHITE_WOOL;
								while(lines.size < 3)
									lines.add("");
								makeSign(b, "&lSTRING", lines[0], lines[1], lines[2]);
								lines = mutableListOf<String>();
								l.add(0.0, 0.0, -1.0);
							}
							for(i in it.indices) {
								val c = it[i];
								width += Utils.charWidth(c);
								if(width > 80) {
									lines.add(it.substring(start, i));
									start = i;
									width = 0;
								}
								if(lines.size >= 3)
									add();
							}
							lines.add(it.substring(start, it.length));
							add();
						});
					}
					null -> { e.setCancelled(false); return; }
				}
			}
			Action.LEFT_CLICK_BLOCK -> {
				if(block.getRelative(BlockFace.WEST).type == Material.BLACK_CONCRETE) {
					block.type = Material.BLACK_STAINED_GLASS;
					block.getRelative(BlockFace.EAST).type = Material.AIR;
					return;
				}
				if(block.getRelative(BlockFace.WEST, 2).type == Material.BLACK_CONCRETE && block.type == Material.OAK_WALL_SIGN) {
					block.type = Material.AIR;
					block.getRelative(BlockFace.WEST).type = Material.BLACK_STAINED_GLASS;
				}
			}
			else -> {}
		}
	}
}
