package net.mcwarlords.wlplugin;

import java.io.*;
import java.util.*;

import net.mcwarlords.wlplugin.code.*;


import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.*;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.*;

import com.google.common.collect.HashBiMap;

/** A bit of data that can be stored */
interface ServerDataField {
	val name: String; /** Name of the field */
	fun load(obj: JSONObject); /** Called on load. Should not modify the object. */
	fun save(obj: JSONObject); /** Called on save. Should modify the passed in object */
}

object Data {
	@JvmField var playerData: MutableMap<String, PlayerData> = mutableMapOf(); /** UUID -> Player data */
	@JvmField var lockedChannels: MutableMap<String, String> = mutableMapOf(); /** Channel name -> Password (channel is not locked if it's not in here) */
	@JvmField var games: MutableMap<String, GameData> = mutableMapOf(); /** ID -> GameData */
	@JvmField var uuids: HashBiMap<String, String> = HashBiMap.create(); /** UUID -> Player Name */
	@JvmField var plotOwners: MutableList<String> = mutableListOf(); /** Arraylist of uuids, each an owner of a plot */
	@JvmField var jsonPlayerData: JSONObject = JSONObject();
	@JvmField var jsonServerData: JSONObject = JSONObject();
	val fields = mutableListOf<ServerDataField>(); /** List of all of the fields */

	@JvmStatic fun onEnable() {
		val parser = JSONParser();
		WlPlugin.info("Loading data...");
		// create files if they don't exist
		for(f in listOf("/", "/tmp", "/cache"))
			if(!File("plugins/wlplugin$f").exists())
				File("plugins/wlplugin$f").mkdir();
		for(f in listOf("playerData.json", "serverData.json", "uuid.json")) {
			if(!File("plugins/wlplugin/$f").exists()) {
				WlPlugin.info("$f does not exist. Creating...");
				val fw = FileWriter("plugins/wlplugin/$f");
				fw.write("{}");
				fw.close();
			}
		}
		try {
			jsonPlayerData = parser.parse(FileReader("plugins/wlplugin/playerData.json")) as JSONObject;
		} catch(e: Exception) {
			WlPlugin.warn("Error loading player data:");
			e.printStackTrace();
		}
		try {
			jsonServerData = parser.parse(FileReader("plugins/wlplugin/serverData.json")) as JSONObject;
		} catch(e: Exception) {
			WlPlugin.warn("Error loading server data:");
			e.printStackTrace();
		}
		// load uuids
		try {
			val jsonuuids: JSONObject = parser.parse(FileReader("plugins/wlplugin/uuid.json")) as JSONObject;
			jsonuuids.forEach { k, v ->
				uuids.put(k as String, v as String);
			}
		} catch(e: Exception) {
			WlPlugin.info("Error loading uuids:");
			e.printStackTrace();
		}
		if(jsonServerData.containsKey("plotOwners")) {
			for(o in jsonServerData["plotOwners"]!! as JSONArray)
				plotOwners.add(o as String);
		}
		if(jsonServerData.containsKey("games")) {
			val jsonGames = jsonServerData["games"]!! as JSONObject;
			jsonGames.forEach { k, _ ->
				games.put(k as String, GameData(k));
			}
		}
		WlPlugin.addListener(object : Listener {
			@EventHandler fun onJoin(e: PlayerJoinEvent) {
				val p = e.getPlayer();
				uuids.put(Utils.getUUID(p), p.getName());
			}
			@EventHandler fun onSneak(e: PlayerToggleSneakEvent) {
				getPlayerData(e.getPlayer()).isSneaking = e.isSneaking();
			}
			@EventHandler fun onSprint(e: PlayerToggleSprintEvent) {
				getPlayerData(e.getPlayer()).isSprinting = e.isSprinting();
			}
			@EventHandler fun onFly(e: PlayerToggleFlightEvent) {
				getPlayerData(e.getPlayer()).isFlying = e.isFlying();
			}
		});
		for(f in fields) {
			WlPlugin.info("Loading field '${f.name}'...");
			f.load(jsonServerData);
		}
		WlPlugin.info("Data loaded.");
	}
	@JvmStatic fun onDisable() {
		saveData();
	}

	@JvmStatic fun saveData() {
		WlPlugin.info("Saving data...");
		for(f in fields) {
			WlPlugin.info("Saving field '${f.name}'...");
			f.save(jsonServerData);
		}
		for(pd in playerData.values) {
			jsonPlayerData.put(pd.uuid, pd.toJSON());
		}
		run {
			val jsonPlotOwners = JSONArray();
			for(s in plotOwners)
				jsonPlotOwners.add(s);
			jsonServerData.put("plotOwners", jsonPlotOwners);
			val jsonGames = JSONObject();
			for(s in games.keys) {
				jsonGames.put(s, games[s]!!.toJSON());
			}
			jsonServerData.put("games", jsonGames);
		}
		val jsonuuids = JSONObject();
		for(s in uuids.keys)
			jsonuuids.put(s, uuids.get(s));
		try {
			var fw = FileWriter("plugins/wlplugin/playerData.json");
			fw.write(jsonPlayerData.toString());
			fw.close();
			fw = FileWriter("plugins/wlplugin/serverData.json");
			fw.write(jsonServerData.toString());
			fw.close();
			fw = FileWriter("plugins/wlplugin/uuid.json");
			fw.write(jsonuuids.toString());
			fw.close();
		} catch(e: IOException) {
			WlPlugin.warn("Error writing data:");
			e.printStackTrace();
		}
		CCacheEvent().execute();
		WlPlugin.info("Data saved.");
	}

	/** Get data for a player */
	@JvmStatic fun getPlayerData(p: Player): PlayerData {
		val uuid = Utils.getUUID(p);
		uuids.put(uuid, p.getName());
		if(!playerData.containsKey(uuid))
			playerData.put(uuid, PlayerData(uuid));
		return playerData[uuid]!!;
	}

	/** Get UUID from player name */
	@JvmStatic fun uuidOf(player: String): String? {
		return uuids.inverse()[player];
	}

	/** Get Player name from UUID */
	@JvmStatic fun nameOf(uuid: String): String? {
		return uuids[uuid];
	}

	/** Test if player exists */
	@JvmStatic fun playerExists(name: String): Boolean {
		return uuids.inverse().containsKey(name);
	}

	@JvmStatic fun cacheName(namespace: String): String {
		return "plugins/wlplugin/cache/$namespace.json";
	}

	// I should probably store these temporarily somewhere (so the file isn't constantly opened and closed upon cache event) but
	// it's probably not enough of an issue to bother optimizing

	/** Gets a cached value */
	@JvmStatic fun cacheGet(namespace: String, name: String): Value? {
		if(!File(cacheName(namespace)).exists())
			return null;
		val obj = cacheObject(namespace);
		if(!obj.containsKey(name))
			return null;
		return Value.deserialize(obj[name]!!);
	}

	/** Sets a cached value */
	@JvmStatic fun cacheSet(namespace: String, name: String, value: Value) {
		val cname = cacheName(namespace);
		val obj = cacheObject(namespace);
		obj.put(name, value.serialize());
		val fw = FileWriter(cname);
		fw.write(obj.toString());
		fw.close();
	}

	/** Clears a cache */
	@JvmStatic fun cacheClear(namespace: String) {
		val cname = cacheName(namespace);
		if(!File(cname).exists())
			return;
		val fw = FileWriter(cname);
		fw.write("{}");
		fw.close();
	}

	/** Gets a cache object */
	@JvmStatic fun cacheObject(namespace: String): JSONObject {
		var cname = cacheName(namespace);
		try {
			return JSONParser().parse(FileReader(cname)) as JSONObject;
		} catch(e: FileNotFoundException) {
			return JSONObject();
		}
	}

	/** Registers a field */
	fun register(field: ServerDataField) {
		fields.add(field);
	}

}

val Player.data
	get() = Data.getPlayerData(this);
