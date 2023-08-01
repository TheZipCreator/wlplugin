// contains Executor

package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;

import net.mcwarlords.wlplugin.*;

sealed class Value private constructor() {
	object Unit : Value();
	class String(val string: kotlin.String) : Value();
	class Number(val num: Double) : Value();
	class Bool(val bool: Boolean) : Value();
	class Entity(val entity: org.bukkit.entity.Entity) : Value();
	class Item(val item: ItemStack) : Value()

	fun truthy(): Boolean = when(this) {
		is Unit -> false
		is String -> string.length != 0
		is Number -> num != 0.0
		is Bool -> bool
		is Entity -> true
		is Item -> true
	}

	override fun toString(): kotlin.String = when(this) {
		is Unit -> "unit"
		is String -> string
		is Number -> num.toString()
		is Bool -> bool.toString()
		is Entity -> entity.toString()
		is Item -> item.toString()
	}

	fun typeName(): kotlin.String = when(this) {
		is Unit -> "unit"
		is String -> "string"
		is Number -> "number"
		is Bool -> "bool"
		is Entity -> "entity"
		is Item -> "item"
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
			is Item -> o is Item && o.item == item
		}
	}
}

// context for interpreting - shared between an executor and all its children
class ExecutorContext(val event: CEvent, var stopped: Boolean);

class ExecutionException(loc: Location?, msg: String) : CodeException(loc, msg);

// a variable
class Var(val scope: UInt, var value: Value);

class Executor(val scope: UInt, val ctx: ExecutorContext, table: MutableMap<String, Var>? = null) {
	var vartable: MutableMap<String, Var> = table ?: mutableMapOf();
	
	// evals children
	private fun evalChildren(children: List<Tree>): Value {
		var v: Value = Value.Unit;
		for(t in children)
			v = eval(t);
		return v;
	}
	// creates a sub-context
	private fun sub() = Executor(scope+1u, ctx, vartable.toMutableMap());
	// evaluates a tree. should be called asynchronously.
	fun eval(tree: Tree): Value {
		when(tree) {
			is Tree.Event -> return evalChildren(tree.children)
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
				var args = mutableListOf<Value>();
				for(t in tree.args)
					args.add(eval(t));
				return builtins[tree.name]!!(this, tree.loc, args);
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
					is Token.Parameter -> return when(tk.name) {
						"player" -> {
							if(ctx.event !is CPlayerEvent) 
								throw ExecutionException(tk.loc, "Paramater 'player' is only valid for player events.");
							return Value.Entity(ctx.event.player)
						}
						else -> throw ExecutionException(tk.loc, "Invalid parameter ${tk.name}")
					}
					is Token.Item -> return Value.Item(tk.item)
					else -> throw ExecutionException(tk.loc, "This should not appear.")
				}
			}
			is Tree.Declare -> {
				if(vartable.containsKey(tree.name))
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
