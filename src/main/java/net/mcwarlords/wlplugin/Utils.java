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

public class Utils {
	public static void init() {
		WlPlugin.addListener(new Listener() {
			@EventHandler(priority = EventPriority.LOWEST) 
			public void onPlayerChat(AsyncPlayerChatEvent e) {
				var pd = Data.getPlayerData(e.getPlayer());
				if(pd.inputCB == null)
					return;
				e.setCancelled(true);
				Bukkit.getScheduler().runTask(WlPlugin.instance, () -> {
					// might remove the escape text later, idk
					pd.inputCB.accept(Utils.escapeText(e.getMessage()));
					pd.inputCB = null;
				});
			}
		});
	}

	/** Adds section symbols and stuff to text */
	public static String escapeText(String txt) {
		StringBuilder sb = new StringBuilder("");
		for(int i = 0; i < txt.length(); i++) {
			char a = txt.charAt(i);
			char b = i+1 < txt.length() ? txt.charAt(i+1) : '\0';
			switch(a) {
				case '&':
					switch(b) {
						case '&':
							i++;
							sb.append('&');
							break;
						case '_':
							// Theme colors
							if(i+2 < txt.length()) {
								sb.append('§');
								switch(txt.charAt(i+2)) {
									case 'p':
										sb.append(WlPlugin.prefixCol);
										break;
									case 'd':
										sb.append(WlPlugin.defaultCol);
										break;
									case 'e':
										sb.append(WlPlugin.errorCol);
										break;
									case 's':
										sb.append(WlPlugin.seperatorCol);
										break;
								}
							}
							i += 2;
							break;
						case '#':
							// hex colors
							i += 2;
							if(i+6 < txt.length()) {
								String hex = txt.substring(i, i+6);
								sb.append(ChatColor.of("#"+hex));
							}
							i += 5;
							break;
						// I'm not even surprised that java doesn't have case ranges
						case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'x': case 'X': case 'k': case 'K': case 'l': case 'L': case 'm': case 'M': case 'n': case 'N': case 'o': case 'O': case 'r': case 'R':
							sb.append('§');
							break;
						default:
							sb.append("&");
							break;
					}
					break;
				case '\\':
					i++;
					switch(b) {
						case 'n':
							sb.append("\n§7... §f");
							break;
						case '\\':
							sb.append("\\");
							break;
						case '\0':
							sb.append("\\");
							break;
						default:
							sb.append(a);
							sb.append(b);
					}
					break;
				default:
					sb.append(a);
			}
		}
		return sb.toString();
	}

	/** similar to escapeText, except it changes it to ANSII escape sequences */
	public static String escapeTextAnsi(String txt) {
		StringBuilder sb = new StringBuilder("");
		for(int i = 0; i < txt.length(); i++) {
			char a = txt.charAt(i);
			char b = i+1 < txt.length() ? txt.charAt(i+1) : '\0';
			switch(a) {
				case '&':
					switch(b) {
						case '&':
							i++;
							sb.append('&');
							break;
						case '_':
							// Theme colors
							if(i+2 < txt.length()) {
								switch(txt.charAt(i+2)) {
									case 'p':
										sb.append("\u001B[0;31m");
										break;
									case 'd':
										sb.append("\u001B[0;37m");
										break;
									case 'e':
										sb.append("\u001B[0;31m");
										break;
									case 's':
										sb.append("\u001B[0;30m");
										break;
								}
							}
							i += 2;
							break;
						case '#':
							// hex colors
							i += 2;
							if(i+6 < txt.length()) {
								String hex = txt.substring(i, i+6);
								// sb.append(ChatColor.of("#"+hex));
							}
							i += 5;
							break;
						default:
							i++;
							switch(txt.charAt(i)) {
								case '0':
									sb.append("\u001B[0;34m");
									break;
								case '1':
									sb.append("\u001B[0;34m");
									break;
								case '2':
									sb.append("\u001B[0;32m");
									break;
								case '3':
									sb.append("\u001B[0;34m");
									break;
								case '4':
									sb.append("\u001B[0;31m");
									break;
								case '5':
									sb.append("\u001B[0;35m");
									break;
								case '6':
									sb.append("\u001B[0;33m");
									break;
								case '7':
									sb.append("\u001B[0;30m");
									break;
								case '8':
									sb.append("\u001B[0;30m");
									break;
								case '9':
									sb.append("\u001B[0;34m");
									break;
								case 'a':
								case 'A':
									sb.append("\u001B[0;32m");
									break;
								case 'b':
								case 'B':
									sb.append("\u001B[0;34m");
									break;
								case 'c':
								case 'C':
									sb.append("\u001B[0;31m");
									break;
								case 'd':
								case 'D':
									sb.append("\u001B[0;35m");
									break;
								case 'e':
								case 'E':
									sb.append("\u001B[0;33m");
									break;
								case 'f':
								case 'F':
								case 'r':
								case 'R':
									sb.append("\u001B[0;37m");
									break;
								// no equivalents to these
								case 'k': case 'K': case 'l': case 'L': case 'm':  case 'M': case 'n': case 'N': case 'o': case 'O':
									break;
								default:
									sb.append("&"+txt.charAt(i));
							}
							break;
					}
					break;
				case '\\':
					i++;
					switch(b) {
						case 'n':
							sb.append("\n\u001B[0;30m... \u001B[0;37m");
							break;
						case '\\':
							sb.append("\\");
							break;
						case '\0':
							sb.append("\\");
							break;
						default:
							sb.append(a);
							sb.append(b);
					}
					break;
				default:
					sb.append(a);
			}
		}
		return sb.toString();
	}

	/** Gets the UUID of a player */
	public static String getUUID(Player p) {
		return p.getUniqueId().toString();
	}

	/** Tries to convert an object to an int.
	 * {@throws ClassCastException on failure}
	 */
	public static int asInt(Object o) {
		if(o instanceof Integer)
			return (int)o;
		else if(o instanceof Long)
			return (int)(((Long)o).longValue());
		else if(o instanceof Double)
			return (int)(((Double)o).doubleValue());
		else if(o instanceof Float)
			return (int)(((Float)o).floatValue());
		throw new ClassCastException();
	}

	/** Tries to convert an object to a float.
	 * {@throws ClassCastException on failure}
	 */
	public static float asFloat(Object o) {
		if(o instanceof Integer)
			return (float)(((Integer)o).intValue());
		else if(o instanceof Long)
			return (float)(((Long)o).longValue());
		else if(o instanceof Double)
			return (float)(((Double)o).doubleValue());
			else if(o instanceof Float)
			return (float)o;
		throw new ClassCastException();
	}

	/** Gets all players in a given channel */
	public static Player[] playersInChannel(String channel) {
		ArrayList<Player> players = new ArrayList<Player>();
		for(Player p : Bukkit.getOnlinePlayers())
			if(Data.getPlayerData(p).channel.equals(channel))
				players.add(p);
		return (Player[])players.toArray();
	}

	/** Get amount of players in a given channel */
	public static int channelPlayerCount(String channel) {
		int i = 0;
		for(Player p : Bukkit.getOnlinePlayers())
			if(Data.getPlayerData(p).channel.equals(channel))
				i++;
		return i;
	}

	/** Returns a bounding box of a plot */
	public static BoundingBox plot(int id) {
		Point tl = new Point(WlPlugin.PLOT_SIZE*(id%10), WlPlugin.PLOT_SIZE*(id/10));
		Point br = new Point(tl.x+(WlPlugin.PLOT_SIZE-1), tl.y+(WlPlugin.PLOT_SIZE-1));
		return new BoundingBox(tl, br);
	}

	/** Gets all plots owned by a given player */
	public static int[] plotsOwnedBy(Player p) {
		return plotsOwnedBy(getUUID(p));
	}

	public static int[] plotsOwnedBy(String uuid) {
		ArrayList<Integer> plots = new ArrayList<Integer>();
		for(int i = 0; i < Data.plotOwners.size(); i++) {
			if(Data.plotOwners.get(i).equals(uuid))
				plots.add(i);
		}
		WlPlugin.info(Integer.toString(plots.size()));
		int[] b = new int[plots.size()];
		for(int i = 0; i < b.length; i++)
			b[i] = plots.get(i);
		return b;
	}

	/** Gets a uniform random number between two integers */
	public static int randInt(int min, int max) {
		return WlPlugin.rand.nextInt(max-min) + min;
	}

	/** Gets a plot id at a given coordinate */
	public static int getPlotAt(int x, int z) {
		if(x < 0 || x > WlPlugin.PLOT_SIZE*10 || z < 0)
			return -1;
		return ((z/WlPlugin.PLOT_SIZE)*10)+((x/WlPlugin.PLOT_SIZE)%10);
	}

	/** Gets a player from a UUID */
	public static Player getPlayer(String uuid) {
		return Bukkit.getPlayer(UUID.fromString(uuid));
	}

	public static JSONArray serializeLocation(Location loc) {
		JSONArray arr = new JSONArray();
		arr.add(loc.getWorld().getName());
		arr.add(loc.getX());
		arr.add(loc.getY());
		arr.add(loc.getZ());
		return arr;
	}

	public static Location deserializeLocation(Object o) {
		JSONArray arr = (JSONArray)o;
		return new Location(
			Bukkit.getServer().getWorld((String)arr.get(0)),
			Utils.asFloat(arr.get(1)),
			Utils.asFloat(arr.get(2)),
			Utils.asFloat(arr.get(3))
		);
	}

	public static ArrayList<String> getSortedGames() {
		ArrayList<String> games = new ArrayList<String>();
		for(String s : Data.games.keySet())
			games.add(s);
		games.sort(String::compareToIgnoreCase);
		return games;
	}

	/** Downloads a file from a URL and stores it to {@code path} */
	public static void downloadTo(String urlString, String path) throws MalformedURLException, IOException {
		URL url = new URL(urlString);
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		FileOutputStream fos = new FileOutputStream(path);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
	}

	/** Downloads a file and stores it to a temporary location (returned by the function) */
	public static String downloadToTemp(String urlString) throws MalformedURLException, IOException {
		String path = "plugins/wlplugin/tmp/"+UUID.randomUUID();
		downloadTo(urlString, path);
		return path;
	}

	/** Returns a random file name */
	public static String tmpFileName() {
		return UUID.randomUUID().toString();
	}

	/** Converts a string to a location */
	public static Location stringToLocation(@NotNull String loc) {
		String[] coords = loc.split(",");
		if(coords.length != 3)
			return null;
		float x = Float.parseFloat(coords[0]);
		float y = Float.parseFloat(coords[1]);
		float z = Float.parseFloat(coords[2]);
		return new Location(Bukkit.getWorlds().get(0), x, y, z);
	}

	/** Creates an item with the specified (escaped) name */
	public static ItemStack createItem(Material m, String name) {
		ItemStack is = new ItemStack(m);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(Utils.escapeText(name));
		is.setItemMeta(im);
		return is;
	}

	/** Creates an item with the specified (escaped) name and the specified (escaped) lore */
	public static ItemStack createItem(Material m, String name, String[] lore) {
		ItemStack is = new ItemStack(m);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(Utils.escapeText(name));
		ArrayList<String> l = new ArrayList<String>();
		for(int i = 0; i < lore.length; i++)
			l.add(escapeText(lore[i]));
		im.setLore(l);
		is.setItemMeta(im);
		return is;
	}

	/** Sends a message to all people in a given channel */
	public static void sendMessage(String channel, String message) {
		if(channel.equals("global")) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				PlayerData pd = Data.getPlayerData(p);
				if(!pd.hideGlobal)
					p.sendMessage(escapeText(message));
			}
			DiscordModule.message(message);
			WlPlugin.info("[CHAT] "+escapeText(message));
			return;
		}
		for(Player p : Bukkit.getOnlinePlayers()) {
			PlayerData pd = Data.getPlayerData(p);
			if(pd.channel.equals(channel))
				p.sendMessage(escapeText(message));
		}
	}
	
	/** Strips color codes from a string */
	public static String stripColorCodes(String msg) {
		StringBuilder ret = new StringBuilder();
		outer:
		for(int i = 0; i < msg.length(); i++) {
			char c = msg.charAt(i);
			switch(c) {
				case '&':
					i++;
					if(i >= msg.length())
						break outer;
					if(msg.charAt(i) == '#')
						i += 6;
					break;
				default:
					ret.append(c);
			}
		}
		System.out.println(ret.toString());
		return ret.toString();
	}

	/** Sends a message to all people in a given channel with a sender */
	public static void sendMessage(Player sender, String channel, String message) {
		if(channel.equals("global")) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				PlayerData pd = Data.getPlayerData(p);
				if(!pd.hideGlobal && !pd.ignored.contains(getUUID(sender)))
					p.sendMessage(escapeText(message));
			}
			DiscordModule.message(message);
			WlPlugin.info("[CHAT] "+escapeText(message));
			return;
		}
		for(Player p : Bukkit.getOnlinePlayers()) {
			PlayerData pd = Data.getPlayerData(p);
			if(pd.channel.equals(channel) && !pd.ignored.contains(getUUID(sender)))
				p.sendMessage(escapeText(message));
		}
	}

	/** Returns true if the object equals any of the other objects */
	public static boolean isAny(Object o, Object... others) {
		for(Object o2 : others)
			if(o.equals(o2))
				return true;
		return false;
	}

	/** Gets an input */
	public static void getInput(Player p, String msg, Consumer<String> callback) {
		p.sendMessage(Utils.escapeText(msg));
		getInput(p, callback);
	}
	
	public static void getInput(Player p, Consumer<String> callback) {
		var pd = Data.getPlayerData(p);
		pd.inputCB = callback;
	}
	private static int[] charWidths = {
			4,2,5,6,6,6,6,3,5,5,5,6,2,6,2,6,6,6,6,6,6,6,6,6,6,6,2,2,5,6,5,6,7,6,6,6,6,6,6,6,6,4,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,4,6,4,6,6,0,6,6,6,6,6,5,6,6,2,6,5,3,6,6,6,6,6,6,6,4,6,6,6,6,6,6,5,2,5,7
	};

	/** Gets the width of a character */
	public static int charWidth(char c) {
		return c < 32 || c > 126 ? 8 : charWidths[c-32];
	}
}
