// contains Executor

package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;

import net.mcwarlords.wlplugin.*;

sealed class Value private constructor() {
	object Unit : Value();
	class String(val string: kotlin.String) : Value();
	class Number(val num: Double) : Value();
	class Bool(val bool: Boolean) : Value();
	class Entity(val entity: org.bukkit.entity.Entity) : Value();
	
	fun truthy(): Boolean = when(this) {
		is Unit -> false
		is String -> string.length != 0
		is Number -> num != 0.0
		is Bool -> bool
		is Entity -> true
	}

	override fun toString(): kotlin.String = when(this) {
		is Unit -> "unit"
		is String -> string
		is Number -> num.toString()
		is Bool -> bool.toString()
		is Entity -> entity.toString()
	}
}

// context for interpreting - shared between an executor and all its children
class ExecutorContext(val event: Event, var stopped: Boolean);

class ExecutionException(loc: Location?, msg: String) : CodeException(loc, msg);

// a variable
class Var(val scope: UInt, var value: Value);

class Executor(val scope: UInt, val ctx: ExecutorContext, table: MutableMap<String, Var>? = null) {
	var vartable: MutableMap<String, Var> = table ?: mutableMapOf();
	
	// evals children
	private inline fun evalChildren(children: List<Tree>): Value {
		var v: Value = Value.Unit;
		for(t in children)
			v = eval(t);
		return v;
	}
	// evaluates a tree. should be called asynchronously.
	fun eval(tree: Tree): Value {
		when(tree) {
			is Tree.Event -> return evalChildren(tree.children)
			is Tree.Do -> return evalChildren(tree.children)
			is Tree.If -> {
				if(eval(tree.cond).truthy())
					return eval(tree.success)
				else
					return if(tree.failure == null) Value.Unit else eval(tree.failure)
			}
			is Tree.Builtin -> {
				if(!builtins.containsKey(tree.name))
					throw ExecutionException(tree.loc, "Invalid builtin ${tree.name}.");
				var args = mutableListOf<Value>();
				for(t in tree.args)
					args.add(eval(t));
				return builtins[tree.name]!!(this, tree.loc, args);
			}
			is Tree.String -> return Value.String(tree.string);
			is Tree.Number -> return Value.Number(tree.num);
			is Tree.Bool -> return Value.Bool(tree.bool);
			is Tree.Variable -> {
				if(tree.name.startsWith("$")) {
					when(tree.name) {
						"\$player" -> {
							if(ctx.event !is PlayerEvent) 
								throw ExecutionException(tree.loc, "\$player is only valid for player events.");
							return Value.Entity(ctx.event.player) 
						}
						"\$action" -> {
							if(ctx.event !is PlayerInteractEvent)
								throw ExecutionException(tree.loc, "\$action is only valid for event 'interact'.");
							return Value.String(when(ctx.event.action) {
								Action.LEFT_CLICK_AIR -> "left-click-air"
								Action.LEFT_CLICK_BLOCK -> "left-click-block"
								Action.PHYSICAL -> "physical"
								Action.RIGHT_CLICK_AIR -> "right-click-air"
								Action.RIGHT_CLICK_BLOCK -> "right-click-block"
							});
						}
						else -> throw ExecutionException(tree.loc, "Invalid special variable ${tree.name}")
					}
				}
				if(!vartable.containsKey(tree.name))
					throw ExecutionException(tree.loc, "Unbound variable ${tree.name}");
				return vartable[tree.name]!!.value;
			}
		}
	}
	
	// runs this executor on a tree asynchronously
	fun run(tree: Tree) {
		runTaskAsync {
			try {
				eval(tree);
			} catch(e: CodeException) {
				ctx.stopped = true;
				if(ctx.event is PlayerEvent)
					runTask { ctx.event.player.sendMessage(Utils.escapeText("&_p* &_eError running module: ${e.toChatString()}")); }
			}
		}
	}
}
