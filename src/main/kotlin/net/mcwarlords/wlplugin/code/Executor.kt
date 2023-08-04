// contains Executor

package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import java.util.concurrent.atomic.*;

import org.json.simple.*;

import kotlin.reflect.*;

import net.mcwarlords.wlplugin.*;

class SerializationException(msg: String) : Exception(msg);

sealed class Value private constructor() {
	object Unit : Value();
	class String(val string: KString) : Value();
	class Number(val num: Double) : Value();
	class Bool(val bool: Boolean) : Value();
	class Entity(val entity: org.bukkit.entity.Entity) : Value();
	class Item(val item: ItemStack) : Value()
	class List(val list: MutableList<Value>) : Value()
	class Map(val map: MutableMap<KString, Value>) : Value()
	class Loc(val loc: Location) : Value()

	fun truthy(): Boolean = when(this) {
		is Unit -> false
		is String -> string.length != 0
		is Number -> num != 0.0
		is Bool -> bool
		is Entity -> true
		is Item -> true
		is List -> list.size > 0
		is Map -> true
		is Loc -> true
	}

	override fun toString(): kotlin.String = when(this) {
		is Unit -> "unit"
		is String -> string
		is Number -> num.toString()
		is Bool -> bool.toString()
		is Entity -> entity.uniqueId.toString()
		is Item -> item.toString()
		is List -> "[${list.joinToString(", ")}]";
		is Map -> map.toString()
		is Loc -> loc.toString()
	}

	fun typeName(): kotlin.String = when(this) {
		is Unit -> "unit"
		is String -> "string"
		is Number -> "number"
		is Bool -> "bool"
		is Entity -> "entity"
		is Item -> "item"
		is List -> "list"
		is Map -> "map"
		is Loc -> "location"
	}

	override operator fun equals(o: Any?): Boolean {
		if(o !is Value)
			return false;
		return when(this) {
			is Unit -> o is Unit
			is String -> o is String && o.string == string
			is Number -> o is Number && o.num == num
			is Bool -> o is Bool && o.bool == bool
			is Entity -> o is Entity && o.entity == entity
			is Item -> o is Item && o.item.isSimilar(item)
			is List -> {
				if(o !is List)
					return false;
				if(list.size != o.list.size)
					return false;
				for(i in list.indices) {
					if(list[i] != o.list[i])
						return false;
				}
				return true;
			}
			is Map -> o is Map && o.map == map
			is Loc -> o is Loc && o.loc == loc
		}
	}
	
	// converts to a directly serializable format.
	@JvmOverloads fun serialize(depth: Int = 0): Any? {
		if(depth > 100)
			throw SerializationException("Depth limit (100) exceeded");
		return when(this) {
			is Unit -> null
			is String -> string
			is Number -> num
			is Bool -> bool
			is Entity -> throw SerializationException("Entity can not be serialized!")
			is Item -> {
				var obj = JSONObject();
				obj.put("__type", "item");
				obj.put("data", item.serialize());
				obj;
			}
			is List -> {
				var array = JSONArray();
				for(item in list)
					array.add(item.serialize(depth+1));
				array;
			}
			is Map -> {
				var obj = JSONObject();
				for(key in map.keys)
					obj.put(key, map[key]!!.serialize(depth+1));
				obj.put("__type", "map");
				obj;
			}
			is Loc -> Utils.serializeLocation(loc)
		}
	}

	companion object {
		@JvmStatic fun deserialize(obj: Any?): Value = when(obj) {
			null -> Value.Unit
			is KString -> String(obj)
			is Double -> Number(obj)
			is Boolean -> Bool(obj)
			is JSONArray -> {
				var list = mutableListOf<Value>();
				for(item in obj)
					list.add(deserialize(item));
				List(list);
			}
			is JSONObject -> {
				val type = obj["__type"]!! as KString;
				when(type) {
					"map" -> {
						var map = mutableMapOf<KString, Value>();
						obj.forEach fn@ { key, v ->
							val k = key as KString;
							if(k == "__type")
								return@fn;
							map[k] = deserialize(v);
						}
						Map(map)
					}
					"item" -> Value.Item(ItemStack.deserialize(obj["data"]!! as MutableMap<KString, Any?>))
					else -> throw SerializationException("Invalid type '$type'.");
				}
			}
			else -> throw SerializationException("Unknown value.")
		}
	}
}
// represents a function
class CFunction(val args: List<String>, val body: Tree) {
	fun call(ctx: ExecutorContext, params: List<Value>): Value {
		var vars = ctx.unit.globals.toMutableMap();
		for(i in args.indices) {
			vars[args[i]] = Var(1u, if(i < args.size) params[i] else Value.Unit);
		}
		var exec = Executor(1u, ctx, vars);
		try {
			return exec.eval(body);
		} catch(e: Return) {
			return e.value;
		}
	}
}

// context for interpreting - shared between an executor and all its children
// operationsLeft is an operation limit - an error is thrown if it exceeds this. Set to -1 to disable
class ExecutorContext(val unit: CodeUnit, val event: CEvent?, var operationsLeft: Int = -1) {
	private var _stopped: AtomicReference<Boolean> = AtomicReference(false);
	var stopped
		get() = _stopped.`get`()
		set(v) = _stopped.`set`(v)
}

class ExecutionException(loc: Location?, msg: String) : CodeException(loc, msg);
class Return(loc: Location, val value: Value) : CodeException(loc, "Unexpected return.");
class Break(loc: Location) : CodeException(loc, "Unexpected break.");
class Continue(loc: Location) : CodeException(loc, "Unexpected continue.");

// a variable
class Var(val scope: UInt, var value: Value);

class Executor(val scope: UInt, val ctx: ExecutorContext, var vartable: MutableMap<String, Var> = mutableMapOf()) {	
	// signals that an operation has been done
	private fun op(l: Location) {
		if(ctx.stopped)
			throw ExecutionException(l, "Halted");
		if(ctx.operationsLeft == 0)
			throw ExecutionException(l, "Exceeded operation limit!");
		if(ctx.operationsLeft != -1)
			ctx.operationsLeft -= 1;
	}
	// evals children
	private fun evalChildren(children: List<Tree>): Value {
		var v: Value = Value.Unit;
		for(t in children)
			v = eval(t);
		return v;
	}
	// creates a sub-context
	private fun sub() = Executor(scope+1u, ctx, vartable.toMutableMap());
	// evaluates a tree. if called synchronously, an operations limit should be applied
	fun eval(tree: Tree): Value {
		op(tree.loc);
		when(tree) {
			is Tree.Event -> return evalChildren(tree.children)
			is Tree.Function -> throw ExecutionException(tree.loc, "Unexpected function")
			is Tree.Do -> return sub().evalChildren(tree.children)
			is Tree.If -> {
				for(i in tree.conds.indices) {
					if(eval(tree.conds[i]).truthy())
						return sub().eval(tree.actions[i]);
				}
				return if(tree.failure == null) Value.Unit else sub().eval(tree.failure)
			}
			is Tree.Builtin -> {
				if(!builtins.containsKey(tree.name))
					throw ExecutionException(tree.loc, "Invalid builtin ${tree.name}.");
				return builtins[tree.name]!!(this, tree.loc, tree.args.map { eval(it) });
			}
			is Tree.Call -> {
				if(!ctx.unit.functions.containsKey(tree.name))
					throw ExecutionException(tree.loc, "Unknown function ${tree.name}.");
				return ctx.unit.functions[tree.name]!!.call(ctx, tree.args.map { eval(it) });
			}
			is Tree.For -> {
				var list = eval(tree.list);
				if(list !is Value.List)
					throw ExecutionException(tree.list.loc, "${list.typeName()} is not iterable.");
				var ret: Value = Value.Unit;
				try {
					for(elem in list.list) {
						op(tree.loc);
						var subExec = sub();
						subExec.vartable[tree.varName] = Var(subExec.scope, elem);
						try {
							ret = subExec.evalChildren(tree.body);
						} catch(e: Continue) {
							continue;
						}
					}
				} catch(e: Break) {}
				return ret;
			}
			is Tree.While -> {
				var ret: Value = Value.Unit;
				try {
					while(eval(tree.cond).truthy()) {
						var subExec = sub();
						try {
							ret = subExec.evalChildren(tree.body);
						} catch(e: Continue) {
							continue;
						}
					}
				} catch(e: Break) {}
				return ret;
			}
			is Tree.Token -> {
				val tk = tree.token;
				when(tk) {
					is Token.String -> return Value.String(tk.string);
					is Token.Number -> return Value.Number(tk.num);
					is Token.Bool -> return Value.Bool(tk.bool);
					is Token.Variable -> {
						if(!vartable.containsKey(tk.name))
							throw ExecutionException(tk.loc, "Unbound variable ${tk.name}");
						return vartable[tk.name]!!.value;
					}
					is Token.Parameter -> when(tk.name) {
						"player" -> {
							if(ctx.event !is CPlayerEvent) 
								throw ExecutionException(tk.loc, "Parameter 'player' is only valid for player events.");
							return Value.Entity(ctx.event.player)
						}
						"command" -> {
							if(ctx.event !is CCommandEvent)
								throw ExecutionException(tk.loc, "Parameter 'command' is only valid for the command event.");
							return Value.String(ctx.event.command);
						}
						"args" -> {
							if(ctx.event !is CCommandEvent)
								throw ExecutionException(tk.loc, "Parameter 'args' is only valid for the command event.");
							return Value.List(ctx.event.args.map { Value.String(it) }.toMutableList());
						}
						"location" -> {
							if(ctx.event !is CLocationEvent)
								throw ExecutionException(tk.loc, "Parameter 'location' is only valid for location events.");
							return Value.Loc(ctx.event.location);
						}
						"item" -> {
							if(ctx.event !is CItemEvent)
								throw ExecutionException(tk.loc, "Parameter 'item' is only valid for item events.")
							return if(ctx.event.item == null) Value.Unit else Value.Item(ctx.event.item!!);
						}
						"gamemode" -> {
							if(ctx.event !is CGameModeChangeEvent)
								throw ExecutionException(tk.loc, "Parameter 'gamemode' is only valid for the change gamemode event.");
							return Value.String(ctx.event.gamemode.lispCase())
						}
						"entity" -> {
							if(ctx.event !is CEntityEvent)
								throw ExecutionException(tk.loc, "Parameter 'entity' is only valid for entity events.");
							return Value.Entity(ctx.event.entity);
						}
						"slot" -> {
							if(ctx.event !is CSlotEvent)
								throw ExecutionException(tk.loc, "Parameter 'slot' is only valid for slot events.");
							return Value.Number(ctx.event.slot.toDouble());
						}
						"block" -> {
							if(ctx.event !is CBlockEvent)
								throw ExecutionException(tk.loc, "Parameter 'block' is only valid for block events.");
							return Value.Item(ItemStack(ctx.event.block.type));
						}
						else -> throw ExecutionException(tk.loc, "Invalid parameter ${tk.name}")
					}
					is Token.Item -> return Value.Item(tk.item)
					is Token.Unit -> return Value.Unit
					is Token.Loc -> return Value.Loc(tk.location)
					is Token.Break -> throw Break(tk.loc)
					is Token.Continue -> throw Continue(tk.loc)
					else -> throw ExecutionException(tk.loc, "This should not appear.")
				}
			}
			is Tree.List -> return Value.List(tree.list.map { eval(it) }.toMutableList())
			is Tree.Map -> return Value.Map((tree.keys.map { eval(it).toString() } zip tree.values.map { eval(it) }).toMap().toMutableMap()) // one liner!
			is Tree.Declare -> {
				if(vartable.containsKey(tree.name) && vartable[tree.name]!!.scope == scope)
					throw ExecutionException(tree.loc, "Variable ${tree.name} already declared. Did you mean to use SET?");
				vartable[tree.name] = Var(scope, eval(tree.value));
				return vartable[tree.name]!!.value;
			}
			is Tree.Set -> {
				if(!vartable.containsKey(tree.name))
					throw ExecutionException(tree.loc, "Unknown variable ${tree.name}. Did you mean to use DECLARE?");
				val v = vartable[tree.name]!!.value;
				vartable[tree.name]!!.value = eval(tree.value);
				return v;
			}
			is Tree.Return -> throw Return(tree.loc, eval(tree.value));
		}
	}
	
	// runs this executor on a tree asynchronously
	fun run(tree: Tree, returnExpected: Boolean = false, sync: Boolean = false) { 
		fun log(msg: String) {
			if(ctx.event is CPlayerEvent)
				runTask { ctx.event.player.sendMessage(Utils.escapeText("&_p* &_eError running module: $msg")); }
			ctx.unit.log(msg);
		}
		val task: () -> Unit = {
			try {
				eval(tree);
			} catch(e: Return) {
				ctx.stopped = true;
				if(!returnExpected)
					log(e.toChatString())
			} catch(e: CodeException) {
				ctx.stopped = true;
				log(e.toChatString());
			} catch(e: Exception) {
				ctx.stopped = true;
				log("&c${e.message}");
			}
		}
		if(sync)
			task();
		else
			runTaskAsync(task);
	}
	// halt this executor
	fun halt() {
		ctx.stopped = true;
	}
}
