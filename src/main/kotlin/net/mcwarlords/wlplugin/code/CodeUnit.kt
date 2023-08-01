package net.mcwarlords.wlplugin.code;

import org.json.simple.*;
import org.bukkit.*;
import net.mcwarlords.wlplugin.*;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

class CodeUnit(val location: Location, val owner: String) {
	companion object {
		@JvmStatic fun fromJSON(o: JSONObject): CodeUnit {
			return CodeUnit(Utils.deserializeLocation(o["location"]), o["owner"] as String);
		}
	}

	var events: Map<String, Tree> = mapOf();

	fun toJSON(): JSONObject {
		var o = JSONObject();
		o["location"] = Utils.serializeLocation(location);
		o["owner"] = owner;
		return o;
	}

	fun build() {
		var tree = Parser.parse(location.clone().add(1.0, 0.0, 0.0));
		if(tree !is Tree.Do)
			throw ParseException(tree.loc, "Invalid tree.");
		var map  = mutableMapOf<String, Tree>();
		for(t in tree.children) {
			when(t) {
				is Tree.Event -> {
					map[t.name] = t;
				}
				else -> throw ParseException(t.loc, "Non-event at top level.")
			}
		}
		events = map;
	}

	fun handleEvent(e: CEvent) {
		if(!events.containsKey(e.name))
			return;
		var exec = Executor(0u, ExecutorContext(e, false));
		exec.run(events[e.name]!!);
		return;
	}
}
