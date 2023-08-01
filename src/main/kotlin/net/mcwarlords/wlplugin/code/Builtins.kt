package net.mcwarlords.wlplugin.code;

import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import kotlin.math.pow;

import net.mcwarlords.wlplugin.*;

private fun argsEqual(loc: Location, args: List<Value>, name: String, size: Int) {
	if(args.size != size)
		throw ExecutionException(loc, "$name takes exactly $size arguments.");
}

private fun argsBetween(loc: Location, args: List<Value>, name: String, min: Int, max: Int) {
	if(args.size < min || args.size > max)
		throw ExecutionException(loc, "$name takes between $min and $max arguments.");
}

private fun argsAtLeast(loc: Location, args: List<Value>, name: String, min: Int) {
	if(args.size < min)
		throw ExecutionException(loc, "$name takes at least $min arguments.");
}

internal fun runTask(task: () -> Unit) {
	Bukkit.getScheduler().runTask(WlPlugin.instance, task);
}
internal fun runTaskAsync(task: () -> Unit) {
	Bukkit.getScheduler().runTaskAsynchronously(WlPlugin.instance, task);
}

typealias Builtin = (exec: Executor, loc: Location, args: List<Value>) -> Value;

private fun operation(name: String, op: (a: Double, b: Double) -> Double): Builtin {
	return fn@ { _, loc, args ->
		argsAtLeast(loc, args, name, 1);
		var va = args[0];
		if(va !is Value.Number)
			throw ExecutionException(loc, "Number expected, got ${va.typeName()}");
		var a = va.num;
		for(b in args.subList(1, args.size)) {
			if(b !is Value.Number)
				throw ExecutionException(loc, "Number expected, got ${b.typeName()}");
			a = op(a, b.num);
		}
		return@fn Value.Number(a)
	};
}

private fun comparison(name: String, op: (a: Double, b: Double) -> Boolean): Builtin {
	return fn@ { _, loc, args ->
		argsAtLeast(loc, args, name, 1);
		var va = args[0];
		if(va !is Value.Number)
			throw ExecutionException(loc, "Number expected, got ${va.typeName()}");
		var a = va.num;
		for(vb in args.subList(1, args.size)) {
			if(vb !is Value.Number)
				throw ExecutionException(loc, "Number expected, got ${vb.typeName()}");
			var b = vb.num;
			if(!op(a, b))
				return@fn Value.Bool(false);
			a = b;
		}
		return@fn Value.Bool(true);
	};
}

private fun equality(name: String, op: (a: Value, b: Value) -> Boolean): Builtin {
	return fn@ { _, loc, args ->
		argsAtLeast(loc, args, name, 1);
		var a = args[0];
		for(b in args.subList(1, args.size)) {
			if(!op(a, b))
				return@fn Value.Bool(false);
			a = b;
		}
		return@fn Value.Bool(true);
	};
}

private fun getPlayer(loc: Location, v: Value): Player {
	if(v !is Value.Entity || v.entity !is Player)
		throw ExecutionException(loc, "Player expected.");
	return v.entity;
}

internal val builtins = mapOf<String, Builtin>(
	// player actions
	"send-message" to fn@ { _, loc, args ->
		argsAtLeast(loc, args, "send-message", 1);
		val player = getPlayer(loc, args[0]);
		var msg = buildString {
			for(v in args.subList(1, args.size))
				append("$v");
		}
		runTask { player.sendMessage(msg); }
		return@fn Value.Unit;
	},
	"give-item" to fn@ { _, loc, args ->
		argsAtLeast(loc, args, "give-item", 1);
		val player = getPlayer(loc, args[0]);
		var items = mutableListOf<ItemStack>();
		for(v in args.subList(1, args.size)) {
			if(v !is Value.Item)
				throw ExecutionException(loc, "Item expected, got ${v.typeName()}.");
			items.add(v.item);
		}
		runTask {
			val inv = player.getInventory();
			for(item in items)
				inv.addItem(item);
		};
		return@fn Value.Unit;
	},
	// operators
	"+" to operation("+", {a, b -> a+b}),
	"-" to operation("-", {a, b -> a-b}),
	"*" to operation("*", {a, b -> a*b}),
	"/" to operation("/", {a, b -> a/b}),
	"%" to operation("%", {a, b -> a%b}),
	"^" to operation("^", {a, b -> a.pow(b)}),
	"<" to comparison("<", {a, b -> a < b}),
	"<=" to comparison("<=", {a, b -> a <= b}),
	">" to comparison(">", {a, b -> a > b}),
	">=" to comparison(">=", {a, b -> a >= b}),
	"=" to equality("=", {a, b -> a == b}),
	"!=" to equality("!=", {a, b -> a != b}),
);
