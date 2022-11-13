package net.mcwarlords.wlplugin;

import org.json.simple.*;

public class PlayerData {
  // saved
  public String prefix = "";
  public String nick   = null;
  public int    plots  = 3;
  // not saved
  public String channel = "global";
  public String uuid;
  
  public PlayerData(String uuid) {
    this.uuid = uuid;
    if(Data.jsonPlayerData.containsKey(uuid)) {
      JSONObject obj = (JSONObject)(Data.jsonPlayerData.get(uuid));
      if(obj.containsKey("prefix"))
        prefix = (String)obj.get("prefix");
      if(obj.containsKey("nick"))
        nick = (String)obj.get("nick");
      if(obj.containsKey("plots"))
        plots = Utils.asInt(obj.get("plots"));
    }
  }

  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("prefix", prefix);
    if(nick != null)
      obj.put("nick", nick);
    obj.put("plots", plots);
    return obj;
  }
}
