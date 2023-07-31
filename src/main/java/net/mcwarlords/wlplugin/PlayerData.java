package net.mcwarlords.wlplugin;

import java.util.*;
import java.util.function.*;

import org.json.simple.*;
import org.bukkit.*;

import org.bukkit.inventory.*;

public class PlayerData {
	// saved //
	public String prefix = "";
	public String nick	 = null;
	public int		plots	= 3;
	public Set<String> ignored;
	public Set<String> discordIgnored;
	public Set<String> subscribed; // subscribed code units
	// not saved //
	// general
	public String uuid;
	public Consumer<String> inputCB = null; // used by `Utils.getInput()`
	// chat
	public String channel = "global";
	public boolean hideGlobal = false;
	// selection information. currently used by /wm c
	public boolean selecting = false; // whether the player is currently selecting
	public Location selStart = null, selEnd = null;
	// wlcode
	public boolean codeMode = false; // used by wlcode
	public ItemStack[] prevInv; // previous inventory before switching to code mode

	public PlayerData(String uuid) {
		this.uuid = uuid;
		ignored = new HashSet<String>();
		discordIgnored = new HashSet<String>();
		subscribed = new HashSet<String>();
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
			if(obj.containsKey("discordIgnored")) {
				JSONArray arr = (JSONArray)obj.get("discordIgnored");
				for(Object o : arr)
					ignored.add((String)o);
			}
			if(obj.containsKey("subscribed")) {
				JSONArray arr = (JSONArray)obj.get("subscribed");
				for(Object o : arr)
					subscribed.add((String)o);
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
		{
			JSONArray arr = new JSONArray();
			for(String s : discordIgnored)
				arr.add(s);
			obj.put("discordIgnored", arr);
		}
		{
			JSONArray arr = new JSONArray();
			for(String s : subscribed)
				arr.add(s);
			obj.put("subscribed", arr);
		}
		return obj;
	}
	
	/** Returns true if the player currently has a selection */
	public boolean hasSelection() {
		return selStart != null && selEnd != null;
	}

	/** Gets a selection with the locations in order */
	public Location[] getSelection() {
		int[] a = new int[]{selStart.getBlockX(), selStart.getBlockY(), selStart.getBlockZ()};
		int[] b = new int[]{selEnd.getBlockX(), selEnd.getBlockY(), selEnd.getBlockZ()};
		for(int i = 0; i < 3; i++) {
			if(a[i] < b[i])
				continue;
			int x = a[i];
			a[i] = b[i];
			b[i] = x;
		}
		return new Location[]{new Location(selStart.getWorld(), a[0], a[1], a[2]), new Location(selEnd.getWorld(), b[0], b[1], b[2])};
	}

	/** Get the dimensions of the selection */
	public int[] selectionSize() {
		Location[] s = getSelection();
		Location a = s[0], b = s[1];
		return new int[]{
			b.getBlockX()-a.getBlockX()+1,
			b.getBlockY()-a.getBlockY()+1,
			b.getBlockZ()-a.getBlockZ()+1
		};
	}
}
