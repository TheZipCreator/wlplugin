package net.mcwarlords.wlplugin.code;

import org.json.simple.*;
import org.bukkit.*;
import net.mcwarlords.wlplugin.*;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.time.*;
import java.time.format.*;

class CodeUnit(val location: Location, val name: String, val owner: String) {
	companion object {
		@JvmStatic fun fromJSON(name: String, o: JSONObject): CodeUnit {
			return CodeUnit(Utils.deserializeLocation(o["location"]), name, o["owner"] as String);
		}
		// CodeUnit dimensions
		val WIDTH = 32;
	}
	
	private var parsed: Tree.Do? = null; // original AST
	var events: Map<String, Tree> = mapOf(); // events
	var functions: Map<String, CFunction> = mapOf(); // functions
	var globals: MutableMap<String, Var> = mutableMapOf(); // stores global variables. all variables inside should have scope 0
	var log: AtomicList<String> = AtomicList<String>(); // stores a log
	var executors: MutableList<Executor> = mutableListOf(); // stores all executors

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
			log = AtomicList<String>();
			if(loadCache)
				handleEvent(CCacheEvent(), true);
			var tree = Parser.parse(location.clone().add(1.0, 0.0, 0.0));
			globals = mutableMapOf();
			if(tree !is Tree.Do)
				throw ParseException(tree.loc, "Invalid tree.");
			var eventsMap = mutableMapOf<String, Tree>();
			var functionsMap = mutableMapOf<String, CFunction>();
			var imported = mutableSetOf(name);
			fun add(t: Tree) {
				when(t) {
					is Tree.Event -> {
						if(eventsMap.containsKey(t.name))
							throw ParseException(t.loc, "Event ${t.name} already declared.");
						eventsMap[t.name] = t;
					}
					is Tree.Declare -> {
						if(globals.containsKey(t.name))
							throw ParseException(t.loc, "Global variable ${t.name} already declared.");
						var exec = Executor(0u, ExecutorContext(this, null, Instant.now(), 100), globals)
						exec.eval(t);
					}
					is Tree.Function -> {
						if(functionsMap.containsKey(t.name))
							throw ParseException(t.loc, "Function ${t.name} already declared.");
						functionsMap[t.name] = t.fn;
					}
					is Tree.Token -> {
						val tk = t.token;
						if(tk !is Token.Import)
							throw ParseException(t.loc, "Unexpected ${tk.name()} at top-level.")
						val other = tk.name;
						if(other in imported)
							throw ParseException(t.loc, "Circular import detected.");
						imported.add(other);
						if(!Data.codeUnits.containsKey(other))
							throw ParseException(t.loc, "Unknown unit '${tk.name}'");
						val cu = Data.codeUnits[other]!!;
						cu.build();
						val children = cu.parsed?.children;
						if(children != null)
							for(t2 in children)
								add(t2);
					}
					else -> throw ParseException(t.loc, "Unexpected ${t.name()} at top-level.")
				}
			}
			for(t in tree.children)
				add(t);
			parsed = tree;
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
		var exec = Executor(1u, ExecutorContext(this, e, Instant.now(), if(sync) 10000 else -1), globals);
		exec.run(events[e.name]!!, true, sync);
		executors.add(exec);
		return;
	}

	val MAX_LOG_SIZE = 100; // maximum log size
	
	// log a value
	fun log(s: Any) {
		log.add("&3[${formatInstant(Instant.now())}]&r $s");
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
