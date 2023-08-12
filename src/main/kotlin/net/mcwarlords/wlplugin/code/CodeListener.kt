package net.mcwarlords.wlplugin.code;

import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.block.*;
import org.bukkit.block.data.type.*;
import org.bukkit.block.data.*;
import org.bukkit.block.sign.*;
import org.bukkit.block.Sign;
import org.bukkit.block.Barrel;
import org.bukkit.*;
import org.bukkit.inventory.*;

import net.mcwarlords.wlplugin.*;

object CodeListener : Listener {
	private fun findType(item: ItemStack): CodeItem? {
		for(ci in CodeItem.values()) {
			if(ci.item == item)
				return ci;
		}
		return null;
	}
	
	private fun makeSign(b: Block, ln0: String, ln1: String = "", ln2: String = "", ln3: String = "") {
		var b2 = b.getRelative(BlockFace.EAST);
		b2.type = Material.OAK_WALL_SIGN;
		var bd = b2.blockData as WallSign;
		bd.facing = BlockFace.EAST;
		b2.blockData = bd;
		var sign = b2.state as Sign;
		var front = sign.getSide(Side.FRONT);
		front.setLine(0, Utils.escapeText(ln0));
		front.setLine(1, Utils.escapeText(ln1));
		front.setLine(2, Utils.escapeText(ln2));
		front.setLine(3, Utils.escapeText(ln3));
		// update the sign again later.
		// I believe I have to do this because the client doesn't realize it's a sign immediately?
		Bukkit.getScheduler().runTaskLater(WlPlugin.instance!!, Runnable {
			sign.update(true);
		}, 5);
	}

	@EventHandler fun onPlayerInteract(e: PlayerInteractEvent) {
		var p = e.player;
		var pd = Data.getPlayerData(p);
		if(!pd.codeMode)
			return;
		var block = e.clickedBlock;
		when(e.action) {
			Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
				var item = e.item;
				if(item == null)
					return;
				if(item == blocksItem) {
					var inv = Bukkit.createInventory(null, 36, "Code Blocks");
					for(i in CodeItem.values()) {
						inv.addItem(i.item);
					}
					p.openInventory(inv);
					e.setCancelled(true);
					return;
				}
				if(block == null)
					return;
				if(item in editingItems) {
					e.setCancelled(true);
					fun edge(b: Block, dir: BlockFace): Block {
						var bl = b;
						var dist = 0;
						while(true) {
							if(dist >= CodeUnit.WIDTH)
								break;
							val rel = bl.getRelative(dir);
							val sign = rel.getRelative(BlockFace.EAST);
							if(rel.type == Material.AIR || (sign.type != Material.OAK_WALL_SIGN && sign.type != Material.AIR))
								break;
							bl = rel;
							dist += 1;
						}
						return bl;
					}
					fun copy(dest: Block, src: Block) {
						dest.blockData = src.blockData;
						if(src.type == Material.OAK_WALL_SIGN) {
							var srcState = src.state as Sign;
							var destState = dest.state as Sign;
							val srcFront = srcState.getSide(Side.FRONT);
							val destFront = destState.getSide(Side.FRONT);
							for(i in 0..3)
								destFront.setLine(i, srcFront.getLine(i));
							destState.update(true);
						}
						if(src.type == Material.BARREL) {
							var srcState = src.state as Barrel;
							var destState = dest.state as Barrel;
							destState.inventory.contents = srcState.inventory.contents;
						}
					}
					if(block.type == Material.OAK_WALL_SIGN)
						block = block.getRelative(BlockFace.WEST);
					if(block.getRelative(BlockFace.WEST).type != Material.BLACK_CONCRETE)
						return;
					when(item) {
						editingItems[0] -> {
							// add line
							val left = edge(block, BlockFace.SOUTH);
							val w = left.world;
							for(y in -64..left.y-1) {
								for(x in left.x..left.x+1) {
									for(z in left.z downTo left.z-CodeUnit.WIDTH) {
										copy(w.getBlockAt(x, y, z), w.getBlockAt(x, y+1, z));
									}
								}
							}
							for(z in left.z downTo left.z-CodeUnit.WIDTH) {
								var b = w.getBlockAt(left.x, left.y, z);
								b.type = Material.BLACK_STAINED_GLASS;
								b.getRelative(BlockFace.EAST).type = Material.AIR;
							}
						}
						editingItems[1] -> {
							// remove line
							val left = edge(block, BlockFace.SOUTH);
							val w = left.world;
							for(y in left.y downTo -64) {
								for(x in left.x..left.x+1) {
									for(z in left.z downTo left.z-CodeUnit.WIDTH) {
										copy(w.getBlockAt(x, y, z), w.getBlockAt(x, y-1, z));
									}
								}
							}
							for(z in left.z downTo left.z-CodeUnit.WIDTH) {
								var b = w.getBlockAt(left.x, -64, z);
								b.type = Material.BLACK_STAINED_GLASS;
								b.getRelative(BlockFace.EAST).type = Material.AIR;
							}
						}
						editingItems[2] -> {
							// add space
							val right = edge(block, BlockFace.NORTH);
							val w = right.world;
							for(x in right.x..right.x+1) {
								for(z in right.z..block.z) {
									copy(w.getBlockAt(x, block.y, z), w.getBlockAt(x, block.y, z+1));
								}
							}
							block.type = Material.BLACK_STAINED_GLASS;
							block.getRelative(BlockFace.EAST).type = Material.AIR;
						}
						editingItems[3] -> {
							// remove space
							val right = edge(block, BlockFace.NORTH);
							val w = right.world;
							for(x in right.x..right.x+1) {
								for(z in block.z downTo right.z) {
									copy(w.getBlockAt(x, block.y, z), w.getBlockAt(x, block.y, z-1));
								}
							}
							right.type = Material.BLACK_STAINED_GLASS;
							right.getRelative(BlockFace.EAST).type = Material.AIR;
						}
						else -> {}
					}
					return;
				}
				if(block.type != Material.BLACK_STAINED_GLASS)
					return;
				e.setCancelled(true);
				fun makeInputSign(name: String, validator: ((s: String) -> Boolean)? = null, displayName: String = name) {
					makeSign(block, "Input name for", "$displayName in chat");
					Utils.getInput(p, fn@ { 
						if(validator != null && !validator(it)) {
							makeSign(block, "&cInvalid $displayName");
							return@fn
						}
						var start = 0;
						var width = 0;
						var lines = mutableListOf<String>();
						for(i in it.indices) {
							val c = it[i];
							width += Utils.charWidth(c);
							if(width > 80) {
								lines.add(it.substring(start, i));
								start = i;
								width = 0;
							}
						}
						lines.add(it.substring(start, it.length));
						while(lines.size < 3)
							lines.add("");
						makeSign(block, "&l${name.uppercase()}", lines[0], lines[1], lines[2])
					});
				}
				fun makeLongInputSign(name: String, type: Material) {
					makeSign(block, "Input $name", "in chat.");
					Utils.getInput(p, {
						// split a string into multiple blocks.
						// this code feels ugly there's probably a better way to do it
						var l = block.location;
						var width = 0;
						var lines = mutableListOf<String>();
						var start = 0;
						fun add() {
							var b = l.block;
							b.type = type;
							while(lines.size < 3)
								lines.add("");
							makeSign(b, "&l${name.uppercase()}", lines[0], lines[1], lines[2]);
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
				val codeItem = findType(item);
				if(codeItem == null)
					return;
				block.type = item.type;
				when(codeItem) {
					CodeItem.LBRACK -> {
						var bd = block.blockData as Piston;
						bd.facing = BlockFace.NORTH;
						block.blockData = bd;
					}
					CodeItem.RBRACK -> {
						var bd = block.blockData as Piston;
						bd.facing = BlockFace.SOUTH;
						block.blockData = bd;
					}
					CodeItem.EVENT -> makeInputSign("event", { it in validEvents})
					CodeItem.FUNCTION -> makeInputSign("function")
					CodeItem.IMPORT -> makeInputSign("import", { Data.codeUnits.containsKey(it) }, "unit")
					CodeItem.BUILTIN -> {
						var bd = block.blockData as Directional;
						bd.facing = BlockFace.EAST;
						block.blockData = bd;
						makeInputSign("builtin", { builtins.containsKey(it) });
					}
					CodeItem.CALL -> makeInputSign("call", displayName="function")
					CodeItem.IF -> makeSign(block, "&lIF");
					CodeItem.DO -> makeSign(block, "&lDO");
					CodeItem.FOR -> makeSign(block, "&lFOR");
					CodeItem.WHILE -> makeSign(block, "&lWHILE");
					CodeItem.RETURN -> makeSign(block, "&lRETURN");
					CodeItem.BREAK -> makeSign(block, "&lBREAK");
					CodeItem.CONTINUE -> makeSign(block, "&lCONTINUE");
					CodeItem.VARIABLE -> makeInputSign("variable");
					CodeItem.STRING -> makeLongInputSign("string", item.type);
					CodeItem.NUMBER -> makeInputSign("number", { it.toDoubleOrNull() != null });
					CodeItem.TRUE -> makeSign(block, "&lBOOL", "true");
					CodeItem.FALSE -> makeSign(block, "&lBOOL", "false");
					CodeItem.PARAMETER -> makeInputSign("parameter");
					CodeItem.ITEM -> {
						var bd = block.blockData as Directional;
						bd.facing = BlockFace.EAST;
						block.blockData = bd;
						makeSign(block, "&lITEM");
					}
					CodeItem.LIST -> makeSign(block, "&lLIST");
					CodeItem.MAP -> makeSign(block, "&lMAP");
					CodeItem.UNIT -> makeSign(block, "&lUNIT");
					CodeItem.LOCATION -> makeInputSign("location", fn@ {
						var s = it.split(" ").map { it.toDoubleOrNull() }
						if(s.size != 3 && s.size != 5)
							return@fn false;
						return@fn s.all { it != null };
					});
					CodeItem.DECLARE -> {
						makeInputSign("declare", displayName="variable");
					}
					CodeItem.SET -> {
						makeInputSign("set", displayName="variable");
					}
					CodeItem.COMMENT -> {
						makeLongInputSign("comment", Material.REDSTONE_LAMP);
					}
				}
			}
			Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR -> {
				val item = e.item;
				if(item in editingItems) {
					e.setCancelled(true);
					val idx = editingItems.indexOf(item);
					p.inventory.setItemInMainHand(editingItems[(idx+1)%editingItems.size]);
					p.playSound(p.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
					return;
				}
				if(block == null)
					return;
				if(block.getRelative(BlockFace.WEST).type == Material.BLACK_CONCRETE) {
					e.setCancelled(true);
					block.type = Material.BLACK_STAINED_GLASS;
					block.getRelative(BlockFace.EAST).type = Material.AIR;
					return;
				}
				if(block.getRelative(BlockFace.WEST, 2).type == Material.BLACK_CONCRETE && block.type == Material.OAK_WALL_SIGN) {
					e.setCancelled(true);
					block.type = Material.AIR;
					block.getRelative(BlockFace.WEST).type = Material.BLACK_STAINED_GLASS;
				}
			}
			else -> {}
		}
	}
}
