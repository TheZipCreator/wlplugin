package net.mcwarlords.wlplugin.code;

import org.json.simple.*;
import org.bukkit.*;
import net.mcwarlords.wlplugin.*;

class CodeUnit(val location: Location, val owner: String) {
	companion object {
		@JvmStatic fun fromJSON(o: JSONObject): CodeUnit {
			return CodeUnit(Utils.deserializeLocation(o["location"]), o["owner"] as String);
		}
	}

	fun toJSON(): JSONObject {
		var o = JSONObject();
		o["location"] = Utils.serializeLocation(location);
		o["owner"] = owner;
		return o;
	}
}
