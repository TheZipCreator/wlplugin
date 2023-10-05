package net.mcwarlords.wlplugin;

import java.util.*;
import java.util.function.*;

import org.bukkit.*;

import net.mcwarlords.wlplugin.discord.DiscordModule;
import net.mcwarlords.wlplugin.util.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import java.net.*;
import java.nio.channels.*;
import java.io.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import kotlin.math.*;

object Utils {
	init {
		WlPlugin.addListener(object : Listener {
			@EventHandler(priority = EventPriority.LOWEST) 
			fun onPlayerChat(e: AsyncPlayerChatEvent) {
				var pd = Data.getPlayerData(e.player);
				if(pd.inputCB == null)
					return;
				e.setCancelled(true);
				Bukkit.getScheduler().runTask(WlPlugin.instance!!, Runnable {
					// might remove the escape text later, idk
					var msg = e.message;
					if(msg.length >= 1 && msg[0] == '\\')
						msg = msg.substring(1);
					pd.inputCB.accept(Utils.escapeText(msg));
					pd.inputCB = null;
				});
			}
		});
	}
	
	// escapes text
	private interface ColorTranslator {
		// translates a simple color with one char
		fun translate(color: Char): String
		// translates a theme color
		fun translateTheme(color: Char): String
		// translates a hex code
		fun translateHex(color: String): String
	}
	
	private fun escapeTextImpl(txt: String, defaultColor: String, ct: ColorTranslator): String {
		return buildString {
			var i = 0;
			while(i < txt.length) {
				val a = txt[i];
				val b = if(i+1 < txt.length) txt[i+1] else '\u0000';
				when(a) {
					'&' -> {
						when(b) {
							'&' -> {
								i += 1;
								append('&');
							}
							'_' -> {
								// Theme colors
								if(i+2 < txt.length)
									append(ct.translateTheme(txt[i+2]));
								i += 2;
							}
							'#' -> {
								// hex colors
								i += 2;
								if(i+5 < txt.length) 
									append(ct.translateHex(txt.substring(i, i+6)));
								i += 5;
							}
							'r' -> {
								// default color
								i += 1;
								append(escapeTextImpl(defaultColor, "&f", ct));
							}
							in '0'..'9', in 'A'..'F', in 'a'..'z', in 'K'..'O', in 'k'..'o' -> {
								append(ct.translate(b));
								i += 1;
							}
							else -> append('&')
						}
					}
					'\\' -> {
						i += 1;
						when(b) {
							'n' -> {
								append('\n');
								append(ct.translate('7'));
								append("... ");
								append(ct.translate('f'));
							}
							'\\' -> append('\\')
							'\u0000' -> append('\\')
							else -> {
								append(a);
								append(b);
							}
						}
					}
					else -> append(a)
				}
				i += 1;
			}
		};
	}
	
	/** Adds section symbols and stuff to text */
	@JvmStatic @JvmOverloads fun escapeText(txt: String, defaultColor: String = "&f"): String {
		return escapeTextImpl(txt, defaultColor, object : ColorTranslator {
			override fun translate(color: Char) = "§"+color
			override fun translateTheme(color: Char) = "§"+when(color) {
				'p' -> WlPlugin.prefixCol
				'd' -> WlPlugin.defaultCol
				'e' -> WlPlugin.errorCol
				's' -> WlPlugin.separatorCol
				else -> "0"
			}
			override fun translateHex(color: String) = ChatColor.of('#'+color).toString();
		});
	}

	private data class Color(val code: String, val r: Int, val g: Int, val b: Int);
	/** similar to escapeText, except it changes it to ANSII escape sequences */
	@JvmStatic @JvmOverloads fun escapeTextAnsi(txt: String, defaultColor: String = "&f"): String {
		return escapeTextImpl(txt, defaultColor, object : ColorTranslator {
			override fun translate(color: Char) = when(color) {
				'0' -> "\u001B[0;30m"
				'1' -> "\u001B[0;34m"
				'2' -> "\u001B[0;32m"
				'3' -> "\u001B[0;34m"
				'4' -> "\u001B[0;31m"
				'5' -> "\u001B[0;35m"
				'6' -> "\u001B[0;33m"
				'7' -> "\u001B[0;37m"
				'8' -> "\u001B[0;30m"
				'9' -> "\u001B[0;34m"
				'a', 'A' -> "\u001B[0;32m"
				'b', 'B' -> "\u001B[0;34m"
				'c', 'C' -> "\u001B[0;31m"
				'd', 'D' -> "\u001B[0;35m"
				'e', 'E' -> "\u001B[0;33m"
				'f', 'F' -> "\u001B[0;37m"
				// no equivalents to these
				'k', 'K', 'l', 'L', 'm',  'M', 'n', 'N', 'o', 'O' -> ""
				else -> "&"+color
			}
			override fun translateTheme(color: Char) = when(color) {
				'p' -> "\u001B[0;31m";
				'd' -> "\u001B[0;37m";
				'e' -> "\u001B[0;31m";
				's' -> "\u001B[0;30m";
				else -> "&_"+color
			}
			val hexColors = listOf(
				Color("30", 0, 0, 0),
				Color("34", 38, 139, 210),
				Color("32", 132, 152, 0),
				Color("31", 220, 50, 47),
				Color("35", 209, 54, 129),
				Color("33", 173, 132, 3),
				Color("37", 255, 255, 255)
			);
			override fun translateHex(color: String): String {
				val r = color.substring(0, 2).toInt(16).toDouble();
				val g = color.substring(2, 4).toInt(16).toDouble();
				val b = color.substring(4, 6).toInt(16).toDouble();
				return "\u001B["+hexColors.minBy { sqrt((it.r-r).pow(2.0)+(it.g-g).pow(2.0)+(it.b-b).pow(2.0)) }.code+"m";
			}
		});
	}

	/** Gets the UUID of a player */
	@JvmStatic fun getUUID(p: Player): String {
		return p.uniqueId.toString();
	}

	/** Tries to convert an object to an int.
	 * {@throws ClassCastException on failure}
	 */
	@JvmStatic fun asInt(o: Any?): Int = when(o) {
		is Int -> o
		is Long -> o.toInt()
		is Double -> o.toInt()
		is Float -> o.toInt()
		else -> throw ClassCastException()
	}
	
	/** Tries to convert an object to a float.
	 * {@throws ClassCastException on failure}
	 */
	@JvmStatic fun asFloat(o: Any?): Float = when(o) {
		is Int -> o.toFloat()
		is Long -> o.toFloat()
		is Double -> o.toFloat()
		is Float -> o
		else -> throw ClassCastException();
	}

	/** Gets all players in a given channel */
	@JvmStatic fun playersInChannel(channel: String): Array<Player> = Bukkit.getOnlinePlayers().filter { Data.getPlayerData(it).channel == channel }.toTypedArray();
	/** Get amount of players in a given channel */
	@JvmStatic fun channelPlayerCount(channel: String) = Bukkit.getOnlinePlayers().count { Data.getPlayerData(it).channel == channel };

	/** Returns a bounding box of a plot */
	@JvmStatic fun plot(id: Int): BoundingBox {
		val tl: Point = Point(WlPlugin.PLOT_SIZE*(id%10), WlPlugin.PLOT_SIZE*(id/10));
		val br: Point = Point(tl.x+(WlPlugin.PLOT_SIZE-1), tl.y+(WlPlugin.PLOT_SIZE-1));
		return BoundingBox(tl, br);
	}

	/** Gets all plots owned by a given player */
	@JvmStatic fun plotsOwnedBy(p: Player) = plotsOwnedBy(getUUID(p));
	@JvmStatic fun plotsOwnedBy(uuid: String) = (0..<Data.plotOwners.size).filter { Data.plotOwners[it] == uuid }.toTypedArray();
	/** Gets a uniform random number between two integers */
	@JvmStatic fun randInt(min: Int, max: Int) = WlPlugin.rand!!.nextInt(max-min) + min;

	/** Gets a plot id at a given coordinate */
	@JvmStatic fun getPlotAt(x: Int, z: Int): Int {
		if(x < 0 || x > WlPlugin.PLOT_SIZE*10 || z < 0)
			return -1;
		return ((z/WlPlugin.PLOT_SIZE)*10)+((x/WlPlugin.PLOT_SIZE)%10);
	}

	/** Gets a player from a UUID */
	@JvmStatic fun getPlayer(uuid: String) = Bukkit.getPlayer(UUID.fromString(uuid));

	@JvmStatic fun serializeLocation(loc: Location): JSONArray {
		var arr = JSONArray();
		arr.add(loc.world?.name);
		arr.add(loc.x);
		arr.add(loc.y);
		arr.add(loc.z);
		return arr;
	}

	@JvmStatic fun deserializeLocation(o: Any?): Location {
		val arr = o as JSONArray;
		return Location(
			Bukkit.getServer().getWorld(arr[0] as String),
			Utils.asFloat(arr[1]).toDouble(),
			Utils.asFloat(arr[2]).toDouble(),
			Utils.asFloat(arr[3]).toDouble()
		);
	}

	@JvmStatic fun getSortedGames() = Data.games.keys.toList().sortedWith { a, b -> a.lowercase().compareTo(b.lowercase()) }

	/** Downloads a file from a URL and stores it to {@code path} */
	@Throws(IOException::class, MalformedURLException::class)
	@JvmStatic fun downloadTo(urlString: String, path: String) {
		val url = URL(urlString);
		val rbc = Channels.newChannel(url.openStream());
		val fos = FileOutputStream(path);
		fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}

	/** Downloads a file and stores it to a temporary location (returned by the function) */
	@Throws(IOException::class, MalformedURLException::class)
	@JvmStatic fun downloadToTemp(urlString: String): String {
		val path = "plugins/wlplugin/tmp/"+UUID.randomUUID();
		downloadTo(urlString, path);
		return path;
	}

	/** Returns a random file name */
	@JvmStatic fun tmpFileName(): String {
		return UUID.randomUUID().toString();
	}

	/** Converts a string to a location */
	@JvmStatic fun stringToLocation(loc: String): Location? {
		val coords = loc.split(",");
		if(coords.size != 3)
			return null;
		val x = coords[0].toDouble();
		val y = coords[1].toDouble();
		val z = coords[2].toDouble();
		return Location(Bukkit.getWorlds()[0], x, y, z);
	}

	/** Creates an item with the specified (escaped) name */
	@JvmStatic fun createItem(m: Material, name: String): ItemStack {
		val item = ItemStack(m);
		var im = item.itemMeta!!;
		im.setDisplayName(escapeText(name));
		item.setItemMeta(im);
		return item;
	}

	/** Creates an item with the specified (escaped) name and the specified (escaped) lore */
	@JvmStatic fun createItem(m: Material, name: String, lore: Array<String>): ItemStack {
		val item = ItemStack(m);
		var im = item.itemMeta!!;
		im.setDisplayName(Utils.escapeText(name));
		im.setLore(lore.map { escapeText(it) });
		item.setItemMeta(im);
		return item;
	}

	/** Sends a message to all people in a given channel */
	@JvmStatic fun sendMessage(channel: String, message: String, sender: Player? = null) {
		val default = if(sender == null) "&f" else Data.getPlayerData(sender).prefix;
		if(channel.equals("global")) {
			for(p in Bukkit.getOnlinePlayers()) {
				val pd = Data.getPlayerData(p);
				if(!pd.hideGlobal) {
					if(sender == null || !pd.ignored.contains(getUUID(sender)))
						p.sendMessage(escapeText(message, default));
				}
			}
			DiscordModule.message(message);
			WlPlugin.info("[CHAT] "+escapeTextAnsi(message, default));
			return;
		}
		for(p in Bukkit.getOnlinePlayers()) {
			val pd = Data.getPlayerData(p);
			if(pd.channel == channel && (sender == null || !pd.ignored.contains(getUUID(sender))))
				p.sendMessage(escapeText(message, default));
		}
	}
	// for java compatibility
	@JvmStatic fun sendMessage(channel: String, message: String) = sendMessage(channel, message, null)
	@JvmStatic fun sendMessage(player: Player, channel: String, message: String) = sendMessage(channel, message, player)
	
	/** Strips color codes from a string */
	@JvmStatic fun stripColorCodes(msg: String): String = buildString {
		var i = 0;
		while(i < msg.length) {
			val c = msg[i];
			when(c) {
				'&' -> {
					i += 1;
					if(i >= msg.length)
						break;
					if(msg[i] == '#')
						i += 6;
				}
				else -> append(c)
			}
			i += 1;
		}
	}

	/** Returns true if the object equals any of the other objects */
	@JvmStatic fun isAny(o: Any, vararg others: Any): Boolean {
		for(o2 in others)
			if(o == o2)
				return true;
		return false;
	}

	/** Gets an input */
	@JvmStatic fun getInput(p: Player, msg: String, callback: Consumer<String>) {
		p.sendMessage(escapeText(msg));
		getInput(p, callback);
	}
	
	@JvmStatic fun getInput(p: Player, callback: Consumer<String>) {
		var pd = Data.getPlayerData(p);
		pd.inputCB = callback;
	}
	private val charWidths = listOf(
			4,2,5,6,6,6,6,3,5,5,5,6,2,6,2,6,6,6,6,6,6,6,6,6,6,6,2,2,5,6,5,6,7,6,6,6,6,6,6,6,6,4,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,4,6,4,6,6,0,6,6,6,6,6,5,6,6,2,6,5,3,6,6,6,6,6,6,6,4,6,6,6,6,6,6,5,2,5,7
	);

	/** Gets the width of a character */
	@JvmStatic fun charWidth(c: Char) = if(c < 32.toChar() || c > 126.toChar()) 8 else charWidths[c.code-32]
}
