package net.mcwarlords.wlplugin;

import java.io.*;
import java.util.*;

import org.bukkit.entity.Player;
import org.json.simple.*;
import org.json.simple.parser.*;

import com.google.common.collect.HashBiMap;

/** Manages player and server data */
public class Data {
  public static HashMap<String, PlayerData> playerData; /** UUID -> Player data */
  public static HashMap<String, String> lockedChannels; /** Channel name -> Password (channel is not locked if it's not in here) */
  public static HashMap<String, GameData> games; /** ID -> GameData */
  public static HashBiMap<String, String> UUIDs; /** UUID -> Player Name */
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
}
