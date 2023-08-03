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
		// CodeUnit dimensions
		val WIDTH = 32;
	}

	var events: Map<String, Tree> = mapOf(); // events
	var functions: Map<String, CFunction> = mapOf(); // functions
	var globals: MutableMap<String, Var> = mutableMapOf(); // stores global variables. all variables inside should have scope 0
	var log: MutableList<String> = mutableListOf(); // stores a log
	private var executors: MutableList<Executor> = mutableListOf(); // stores all executors

	// converts to json
	fun toJSON(): JSONObject {
		var o = JSONObject();
		o["location"] = Utils.serializeLocation(location);
		o["owner"] = owner;
		return o;
	}
	
	// builds the unit
	fun build(loadCache: Boolean = true) {
		try {
			if(loadCache)
				handleEvent(CCacheEvent(), true);
			var tree = Parser.parse(location.clone().add(1.0, 0.0, 0.0));
			globals = mutableMapOf();
			if(tree !is Tree.Do)
				throw ParseException(tree.loc, "Invalid tree.");
			var eventsMap = mutableMapOf<String, Tree>();
			var functionsMap = mutableMapOf<String, CFunction>();
			for(t in tree.children) {
				when(t) {
					is Tree.Event -> {
						eventsMap[t.name] = t;
					}
					is Tree.Declare -> {
						var exec = Executor(0u, ExecutorContext(this, null), globals)
						exec.run(t);
					}
					is Tree.Function -> {
						functionsMap[t.name] = t.fn;
					}
					else -> throw ParseException(t.loc, "Non-event at top level.")
				}
			}
			events = eventsMap;
			functions = functionsMap;
			handleEvent(CInitEvent());
		} catch(e: CodeException) {
			log("&c${e.toChatString()}");
			throw e;
		} catch(e: Exception) {
			log("&c${e.message}");
			throw e;
		}
	}
	
	// handles an event
	fun handleEvent(e: CEvent, sync: Boolean = e is CCancellable) {
		if(!events.containsKey(e.name))
			return;
		var exec = Executor(1u, ExecutorContext(this, e, if(sync) 10000 else -1), globals);
		exec.run(events[e.name]!!, true, sync);
		executors.add(exec);
		return;
	}

	val MAX_LOG_SIZE = 100; // maximum log size
	
	// log a value
	fun log(s: Any) {
		log.add(s.toString());
		if(log.size > MAX_LOG_SIZE)
			log.removeAt(0);
	}

	// should be called every tick
	fun update() {
		executors.removeAll { it.ctx.stopped };
	}

	// halts all executors
	fun halt() {
		for(exec in executors)
			exec.halt();
	}
}
