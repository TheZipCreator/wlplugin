package net.mcwarlords.wlplugin;

import java.util.*;
import java.util.function.*;

import org.json.simple.*;
import org.bukkit.*;

import org.bukkit.inventory.*;

public class PlayerData(val uuid: String) {
	// saved //
	@JvmField var prefix: String = "";
	@JvmField var nick: String? = null;
	@JvmField var plots: Int = 3;
	@JvmField val ignored = mutableSetOf<String>();
	@JvmField var discordIgnored = mutableSetOf<String>();
	@JvmField var subscribed = mutableSetOf<String>(); // subscribed code units
	// not saved //
	// general
	@JvmField var inputCB: ((String) -> Unit)? = null;
	@JvmField var isSneaking = false;
	@JvmField var isFlying = false;
	@JvmField var isSprinting = false;
	// chat
	@JvmField var channel = "global";
	@JvmField var hideGlobal = false;
	// selection information. currently used by /wm c
	@JvmField var selecting = false;
	@JvmField var selStart: Location? = null;
	@JvmField var selEnd: Location? = null;
	// wlcode
	@JvmField var codeMode = false;
	@JvmField var prevInv: Array<ItemStack>? = null;

	init {
		if(Data.jsonPlayerData.containsKey(uuid)) {
			val obj = Data.jsonPlayerData.get(uuid) as JSONObject;
			if(obj.containsKey("prefix"))
				prefix = obj.get("prefix")!! as String;
			if(obj.containsKey("nick"))
				nick = obj.get("nick")!! as String;
			if(obj.containsKey("plots"))
				plots = Utils.asInt(obj.get("plots")!!);
			if(obj.containsKey("ignored")) {
				val arr = obj.get("ignored")!! as JSONArray;
				for(o in arr)
					ignored.add(o as String);
			}
			if(obj.containsKey("discordIgnored")) {
				val arr = obj.get("discordIgnored")!! as JSONArray;
				for(o in arr)
					ignored.add(o as String);
			}
			if(obj.containsKey("subscribed")) {
				val arr = obj.get("subscribed")!! as JSONArray;
				for(o in arr)
					subscribed.add(o as String);
			}
		}
	}

	fun toJSON(): JSONObject {
		val obj = JSONObject();
		obj.put("prefix", prefix);
		if(nick != null)
			obj.put("nick", nick);
		obj.put("plots", plots);
		run {
			val arr = JSONArray();
			for(s in ignored)
				arr.add(s);
			obj.put("ignored", arr);
		}
		run {
			val arr = JSONArray();
			for(s in discordIgnored)
				arr.add(s);
			obj.put("discordIgnored", arr);
		}
		run {
			var arr = JSONArray();
			for(s in subscribed)
				arr.add(s);
			obj.put("subscribed", arr);
		}
		return obj;
	}
	
	/** Returns true if the player currently has a selection */
	fun hasSelection(): Boolean {
		return selStart != null && selEnd != null;
	}

	/** Gets a selection with the locations in order */
	val selection: Array<Location>
		get() {
			val a = arrayOf(selStart!!.getBlockX(), selStart!!.getBlockY(), selStart!!.getBlockZ());
			val b = arrayOf(selEnd!!.getBlockX(), selEnd!!.getBlockY(), selEnd!!.getBlockZ());
			for(i in 0..<3) {
				if(a[i] < b[i])
					continue;
				val x = a[i];
				a[i] = b[i];
				b[i] = x;
			}
			return arrayOf(Location(selStart!!.getWorld(), a[0].toDouble(), a[1].toDouble(), a[2].toDouble()), Location(selEnd!!.getWorld(), b[0].toDouble(), b[1].toDouble(), b[2].toDouble()));
		}

	/** Get the dimensions of the selection */
	fun selectionSize(): Array<Int> {
		val s = selection;
		val a = s[0];
		val b = s[1];
		return arrayOf(
			b.getBlockX()-a.getBlockX()+1,
			b.getBlockY()-a.getBlockY()+1,
			b.getBlockZ()-a.getBlockZ()+1
		);
	}
}
