package net.mcwarlords.wlplugin.code;

import org.json.simple.*;
import org.bukkit.*;
import net.mcwarlords.wlplugin.*;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

class CodeUnit(val location: Location, val name: String, val owner: String) {
	companion object {
		@JvmStatic fun fromJSON(name: String, o: JSONObject): CodeUnit {
			return CodeUnit(Utils.deserializeLocation(o["location"]), name, o["owner"] as String);
		}
	}

	var events: Map<String, Tree> = mapOf();
	var globals: MutableMap<String, Var> = mutableMapOf(); // stores global variables. all variables inside should have scope 0
	var log: MutableList<String> = mutableListOf(); // stores a log

	fun toJSON(): JSONObject {
		var o = JSONObject();
		o["location"] = Utils.serializeLocation(location);
		o["owner"] = owner;
		return o;
	}

	fun build() {
		try {
			var tree = Parser.parse(location.clone().add(1.0, 0.0, 0.0));
			globals = mutableMapOf();
			if(tree !is Tree.Do)
				throw ParseException(tree.loc, "Invalid tree.");
			var map  = mutableMapOf<String, Tree>();
			for(t in tree.children) {
				when(t) {
					is Tree.Event -> {
						map[t.name] = t;
					}
					is Tree.Declare -> {
						var exec = Executor(0u, ExecutorContext(this, null, false), globals)
						exec.run(t);
					}
					else -> throw ParseException(t.loc, "Non-event at top level.")
				}
			}
			events = map;
		} catch(e: CodeException) {
			log("&c${e.toChatString()}");
			throw e;
		} catch(e: Exception) {
			log("&c${e.message}");
			throw e;
		}
	}

	fun handleEvent(e: CEvent) {
		if(!events.containsKey(e.name))
			return;
		var exec = Executor(1u, ExecutorContext(this, e, false), globals);
		exec.run(events[e.name]!!, true);
		return;
	}

	val MAX_LOG_SIZE = 100;

	fun log(s: Any) {
		log.add(s.toString());
		if(log.size > MAX_LOG_SIZE)
			log.removeAt(0);
	}
}
