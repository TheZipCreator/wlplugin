package net.mcwarlords.wlplugin;

import java.util.*;

import org.json.simple.*;

public class PlayerData {
  // saved
  public String prefix = "";
  public String nick   = null;
  public int    plots  = 3;
  public Set<String> ignored;
  // not saved
  public String channel = "global";
  public String uuid;
  public boolean hideGlobal = false;
  
  public PlayerData(String uuid) {
    this.uuid = uuid;
    ignored = new HashSet<String>();
    if(Data.jsonPlayerData.containsKey(uuid)) {
      JSONObject obj = (JSONObject)(Data.jsonPlayerData.get(uuid));
      if(obj.containsKey("prefix"))
        prefix = (String)obj.get("prefix");
      if(obj.containsKey("nick"))
        nick = (String)obj.get("nick");
      if(obj.containsKey("plots"))
        plots = Utils.asInt(obj.get("plots"));
      if(obj.containsKey("ignored")) {
        JSONArray arr = (JSONArray)obj.get("ignored");
        for(Object o : arr)
          ignored.add((String)o);
      }
    }
  }

  JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("prefix", prefix);
    if(nick != null)
      obj.put("nick", nick);
    obj.put("plots", plots);
    {
      JSONArray arr = new JSONArray();
      for(String s : ignored)
        arr.add(s);
      obj.put("ignored", arr);
    }
    return obj;
  }
}
