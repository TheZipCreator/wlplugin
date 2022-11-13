package net.mcwarlords.wlplugin;

import org.bukkit.Location;
import org.json.simple.*;

public class GameData {
  public String id;
  public String name;
  public String author; /** UUID */
  public String description = "No description set.";
  public String dateCreated = "unknown";
  public Location location;

  public GameData(String id) {
    this.id = id;
    JSONObject games = (JSONObject)Data.jsonServerData.get("games");
    if(games.containsKey(id)) {
      JSONObject game = (JSONObject)games.get(id);
      if(game.containsKey("name"))
        name = (String)game.get("name");
      if(game.containsKey("author"))
        author = (String)game.get("author");
      if(game.containsKey("description"))
        description = (String)game.get("description");
      if(game.containsKey("location"))
        location = Utils.deserializeLocation(game.get("location"));
      if(game.containsKey("dateCreated"))
        dateCreated = (String)game.get("dateCreated");
      return;
    }
    name = id;
  }

  public JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("name", name);
    obj.put("author", author);
    obj.put("description", description);
    obj.put("dateCreated", dateCreated);
    obj.put("location", Utils.serializeLocation(location));
    return obj;
  }
}
