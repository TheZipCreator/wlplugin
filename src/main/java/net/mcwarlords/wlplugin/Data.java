package net.mcwarlords.wlplugin;

import java.io.*;
import java.util.*;

import net.mcwarlords.wlplugin.code.*;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.*;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.*;

import com.google.common.collect.HashBiMap;

/** Manages player and server data */
public class Data {
	public static HashMap<String, PlayerData> playerData; /** UUID -> Player data */
	public static HashMap<String, String> lockedChannels; /** Channel name -> Password (channel is not locked if it's not in here) */
	public static HashMap<String, GameData> games; /** ID -> GameData */
	public static HashBiMap<String, String> UUIDs; /** UUID -> Player Name */
	public static HashMap<String, CodeUnit> codeUnits; /** Code units */
	public static List<String> plotOwners; /** Arraylist of UUIDs, each an owner of a plot */
	public static JSONObject jsonPlayerData;
	public static JSONObject jsonServerData;

	public static void onEnable() {
		playerData = new HashMap<String, PlayerData>();
		lockedChannels = new HashMap<String, String>();
		UUIDs = HashBiMap.create();
		JSONParser parser = new JSONParser();
		WlPlugin.info("Loading data...");
		// create files if they don't exist
		try {
			if(!new File("plugins/wlplugin").exists())
				new File("plugins/wlplugin").mkdir();
			if(!new File("plugins/wlplugin/tmp").exists())
				new File("plugins/wlplugin/tmp").mkdir();
			if(!new File("plugins/wlplugin/cache").exists())
				new File("plugins/wlplugin/cache").mkdir();
			String[] files = {"playerData.json", "serverData.json", "uuid.json"};
			for(String file : files) {
				if(!new File("plugins/wlplugin/"+file).exists()) {
					WlPlugin.info(file+" does not exist. Creating...");
					FileWriter fw = new FileWriter("plugins/wlplugin/"+file);
					fw.write("{}");
					fw.close();
				}
			}
		} catch(IOException e) {}
		try {
			jsonPlayerData = (JSONObject)(parser.parse(new FileReader("plugins/wlplugin/playerData.json")));
		} catch(Exception e) {
			WlPlugin.info("Error loading player data:");
			e.printStackTrace();
			jsonPlayerData = new JSONObject();
		}
		try {
			jsonServerData = (JSONObject)(parser.parse(new FileReader("plugins/wlplugin/serverData.json")));
		} catch(Exception e) {
			WlPlugin.info("Error loading server data:");
			e.printStackTrace();
			jsonServerData = new JSONObject();
		}
		// load UUIDs
		try {
			JSONObject jsonUUIDs = (JSONObject)(parser.parse(new FileReader("plugins/wlplugin/uuid.json")));
			for(Object k : jsonUUIDs.keySet())
				UUIDs.put((String)k, (String)jsonUUIDs.get(k));
		} catch(Exception e) {
			WlPlugin.info("Error loading UUIDs:");
			e.printStackTrace();
		}
		plotOwners = new ArrayList<String>();
		if(jsonServerData.containsKey("plotOwners")) {
			for(Object o : (JSONArray)jsonServerData.get("plotOwners"))
				plotOwners.add((String)o);
		}
		games = new HashMap<String, GameData>();
		if(jsonServerData.containsKey("games")) {
			JSONObject jsonGames = (JSONObject)jsonServerData.get("games");
			for(Object o : jsonGames.keySet()) {
				games.put((String)o, new GameData((String)o));
			}
		}
		codeUnits = new HashMap<String, CodeUnit>();
		if(jsonServerData.containsKey("codeUnits")) {
			JSONObject jsonCodeUnits = (JSONObject)jsonServerData.get("codeUnits");
			for(Object o : jsonCodeUnits.keySet()) {
				String name = (String)o;
				codeUnits.put(name, CodeUnit.fromJSON(name, (JSONObject)jsonCodeUnits.get(o)));
			}
		}
		WlPlugin.addListener(
			new Listener() {
				@EventHandler public void onJoin(PlayerJoinEvent e) {
					Player p = e.getPlayer();
					UUIDs.put(Utils.getUUID(p), p.getName());
				}
			}
		);
		WlPlugin.info("Data loaded.");
	}
	public static void onDisable() {
		saveData();
	}

	public static void saveData() {
		WlPlugin.info("Saving data...");
		for(PlayerData pd : playerData.values()) {
			jsonPlayerData.put(pd.uuid, pd.toJSON());
		}
		{
			JSONArray jsonPlotOwners = new JSONArray();
			for(String s : plotOwners)
				jsonPlotOwners.add(s);
			jsonServerData.put("plotOwners", jsonPlotOwners);
			JSONObject jsonGames = new JSONObject();
			for(String s : games.keySet()) {
				jsonGames.put(s, games.get(s).toJSON());
			}
			jsonServerData.put("games", jsonGames);
			JSONObject jsonCodeUnits = new JSONObject();
			for(String s : codeUnits.keySet()) {
				jsonCodeUnits.put(s, codeUnits.get(s).toJSON());
			}
			jsonServerData.put("codeUnits", jsonCodeUnits);
		}
		JSONObject jsonUUIDs = new JSONObject();
		for(String s : UUIDs.keySet())
			jsonUUIDs.put(s, UUIDs.get(s));
		try {
			FileWriter fw = new FileWriter("plugins/wlplugin/playerData.json");
			fw.write(jsonPlayerData.toString());
			fw.close();
			fw = new FileWriter("plugins/wlplugin/serverData.json");
			fw.write(jsonServerData.toString());
			fw.close();
			fw = new FileWriter("plugins/wlplugin/uuid.json");
			fw.write(jsonUUIDs.toString());
			fw.close();
		} catch(IOException e) {
			WlPlugin.info("Error writing data:");
			e.printStackTrace();
		}
		new CCacheEvent().execute();
		WlPlugin.info("Data saved.");
	}

	/** Get data for a player */
	public static PlayerData getPlayerData(Player p) {
		String uuid = Utils.getUUID(p);
		UUIDs.put(uuid, p.getName());
		if(!playerData.containsKey(uuid))
			playerData.put(uuid, new PlayerData(uuid));
		return playerData.get(uuid);
	}

	/** Get UUID from player name */
	public static String uuidOf(String player) {
		return UUIDs.inverse().get(player);
	}

	/** Get Player name from UUID */
	public static String nameOf(String uuid) {
		return UUIDs.get(uuid);
	}

	/** Test if player exists */
	public static boolean playerExists(String name) {
		return UUIDs.inverse().containsKey(name);
	}

	static private String cacheName(String namespace) {
		return "plugins/wlplugin/cache/"+namespace+".json";
	}

	// I should probably store these temporarily somewhere (so the file isn't constantly opened and closed upon cache event) but
	// it's probably not enough of an issue to bother optimizing

	/** Gets a cached value */
	public static Value cacheGet(String namespace, String name) throws IOException {
		if(!new File(cacheName(namespace)).exists())
			return null;
		var cname = cacheName(namespace);
		try {
			JSONObject obj = cacheObject(namespace);
			if(!obj.containsKey(name))
				return null;
			return Value.deserialize(obj.get(name));
		} catch(FileNotFoundException e) {
			// unreachable
			e.printStackTrace();
			return null;	
		}
	}

	/** Sets a cached value */
	public static void cacheSet(String namespace, String name, Value val) throws IOException {
		var cname = cacheName(namespace);
		try {
			JSONObject obj = cacheObject(namespace);
			obj.put(name, val.serialize());
			FileWriter fw = new FileWriter(cname);
			fw.write(obj.toString());
			fw.close();
		} catch(FileNotFoundException e) {
			// unreachable
			e.printStackTrace();
		}
	}

	/** Clears a cache */
	public static void cacheClear(String namespace) throws IOException {
		var cname = cacheName(namespace);
		if(!new File(cname).exists())
			return;
		try {
			FileWriter fw = new FileWriter(cname);
			fw.write("{}");
			fw.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/** Gets a cache object */
	public static JSONObject cacheObject(String namespace) throws IOException {
		var cname = cacheName(namespace);
		try {
			return (JSONObject)(new JSONParser().parse(new FileReader(cname)));
		} catch(FileNotFoundException e) {
			return new JSONObject();
		} catch(ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

}
