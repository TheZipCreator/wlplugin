package net.mcwarlords.wlplugin.code;

import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.projectiles.*;
import kotlin.math.pow;
import kotlin.random.*;
import java.util.concurrent.atomic.*;

import java.util.UUID;

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

private fun argsOneOf(loc: Location, args: List<Value>, name: String, vararg amounts: Int) {
	if(args.size !in amounts)
		throw ExecutionException(loc, "$name takes ${amounts.toList().subList(0, amounts.size-1).joinToString(", ")} or ${amounts[amounts.size-1]} arguments.");
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

// idt there's a way to make a template for these

private fun getPlayer(loc: Location, v: Value): Player {
	if(v !is Value.Entity || v.entity !is Player)
		throw ExecutionException(loc, "Player expected.");
	return v.entity;
}

private fun Value.getNum(loc: Location): Double {
	if(this !is Value.Number)
		throw ExecutionException(loc, "Number expected, got ${typeName()}.");
	return num;
}
private fun Value.getBool(loc: Location): Boolean {
	if(this !is Value.Bool)
		throw ExecutionException(loc, "Bool expected, got ${typeName()}.");
	return bool;
}
private fun Value.getList(loc: Location): MutableList<Value> {
	if(this !is Value.List)
		throw ExecutionException(loc, "List expected, got ${typeName()}.");
	return list;
}
private fun Value.getMap(loc: Location): MutableMap<String, Value> {
	if(this !is Value.Map)
		throw ExecutionException(loc, "Map expected, got ${typeName()}.");
	return map;
}
private fun Value.getEntity(loc: Location): Entity {
	if(this !is Value.Entity)
		throw ExecutionException(loc, "Entity expected, got ${typeName()}.");
	return entity;
}
private fun Value.getLocation(loc: Location): Location {
	if(this !is Value.Loc)
		throw ExecutionException(loc, "Location expected, got ${typeName()}.");
	return this.loc;
}

private fun <T> List<T>.boundsCheck(loc: Location, i: Int) {
	if(i < 0 || i >= size)
		throw ExecutionException(loc, "Index $i is out of range for list of size $size");
}
private fun Map<String, Value>.boundsCheck(loc: Location, key: String) {
	if(!containsKey(key))
		throw ExecutionException(loc, "Key '$key' is not in map.");
}

private fun getRange(loc: Location, args: List<Value>, name: String): Pair<Int, Int> {
	argsBetween(loc, args, name, 1, 2);
	var min: Int = 0;
	var max: Int = 0;
	if(args.size == 1)
		max = args[0].getNum(loc).toInt();
	else {
		min = args[0].getNum(loc).toInt();
		min = args[1].getNum(loc).toInt();
	}
	if(max < min)
		min = max.also { max = min };
	return Pair(min, max);
}

private fun entityTypeFromValue(loc: Location, v: Value): EntityType {
	val name = v.toString();
	try {
		return EntityType.valueOf(name.uppercase().replace('-', '_'));
	} catch(e: IllegalArgumentException) {
		throw ExecutionException(loc, "Invalid entity type '$name'");
	}
}

internal val builtins = mapOf<String, Builtin>(
	// player actions
	// sends a message to a player
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
	// gives an item to a player
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
	// player values
	// gets the item a player is holding in their main hand
	"main-hand-item" to fn@ { _, loc, args ->
		argsEqual(loc, args, "held-item", 1);
		Value.Item(getPlayer(loc, args[0]).inventory.itemInMainHand);
	},
	// gets the item a player is holding in their off hand
	"off-hand-item" to fn@ { _, loc, args ->
		argsEqual(loc, args, "held-item", 1);
		Value.Item(getPlayer(loc, args[0]).inventory.itemInOffHand);
	},
	// gets a player by a name.
	"player-by-name" to fn@ { _, loc, args ->
		argsEqual(loc, args, "player-by-name", 1);
		val name = args[0].toString();
		val player = Bukkit.getPlayer(name);
		if(player == null)
			throw ExecutionException(loc, "Player '$name' is offline or does not exist.");
		Value.Entity(player);
	},
	// gets a player by a UUID
	"player-by-uuid" to fn@ { _, loc, args ->
		argsEqual(loc, args, "player-by-name", 1);
		val name = args[0].toString();
		try {
			val player = Bukkit.getPlayer(UUID.fromString(name));
			if(player == null)
				throw ExecutionException(loc, "Player with UUID '$name' is offline or does not exist.");
			Value.Entity(player);
		} catch(e: IllegalArgumentException) {
			throw ExecutionException(loc, "Invalid UUID '$name'");
		}
	},
	// gets the UUID of an entity or player (a string may be passed in that represents the player's name; they don't have to be online for that to work). Returns Unit if the UUID can not be found.
	"uuid-of" to fn@ { _, loc, args ->
		argsEqual(loc, args, "uuid-of", 1);
		val v = args[0];
		when(v) {
			is Value.Entity -> Value.String(v.entity.uniqueId.toString())
			is Value.String -> {
				val uuid = Data.uuidOf(v.toString());
				if(uuid == null)
					Value.Unit
				else
					Value.String(uuid)
			}
			else -> throw ExecutionException(loc, "Expected string or entity, got ${v.typeName()}");
		}
	},
	// gets the name of a player. You can pass in a UUID instead of a player.
	"name-of" to fn@ { _, loc, args ->
		argsEqual(loc, args, "name-of", 1);
		val uuid = args[0].toString();
		val name = Data.nameOf(uuid);
		if(name == null)
			throw ExecutionException(loc, "Could not get name of player with UUID '$uuid'");
		Value.String(name);
	},
	// entity actions
	// teleports an entity
	"teleport" to { _, loc, args ->
		argsEqual(loc, args, "teleport", 2);
		args[0].getEntity(loc).teleport(args[1].getLocation(loc));
		Value.Unit
	},
	// spawns an entity. returns the entity spawned.
	"spawn" to { _, loc, args ->
		argsEqual(loc, args, "spawn", 2);
		val type = entityTypeFromValue(loc, args[0]);
		val l = args[1].getLocation(loc);
		var e: AtomicReference<Entity?> = AtomicReference(null);
		runTask {
			e.`set`(l.world!!.spawnEntity(l, type));
		}
		while(e.`get`() == null); // wait until spawned
		Value.Entity(e.`get`()!!);
	},
	// launches a projectile from a given entity. returns the projectile.
	"launch-projectile" to { _, loc, args ->
		argsEqual(loc, args, "launch-projectile", 2);
		val source = args[0].getEntity(loc);
		if(source !is ProjectileSource)
			throw ExecutionException(loc, "Entity is not a projectile source.");
		val typeName = args[1];
		val type = entityTypeFromValue(loc, typeName);
		val clazz = type.getEntityClass();
		if(!Projectile::class.java.isAssignableFrom(type.entityClass!!))
			throw ExecutionException(loc, "Entity type '$typeName' is not a projectile.");
		var e: AtomicReference<Entity?> = AtomicReference(null);
		runTask {
			e.`set`(source.launchProjectile(clazz as Class<Projectile>)); // honestly surprised this cast even works
		}
		while(e.`get`() == null);
		Value.Entity(e.`get`()!!);
	},
	// gets the location of an entity
	"location-of" to { _, loc, args ->
		argsEqual(loc, args, "location-of", 1);
		Value.Loc(args[0].getEntity(loc).location)
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
	"!" to { _, loc, args ->
		argsEqual(loc, args, "!", 1);
		Value.Bool(!args[0].truthy());
	},
	// numeric actions
	// gets a random float between 0 and 1
	"rand-float" to fn@ { _, loc, args ->
		argsEqual(loc, args, "rand-float", 0);
		Value.Number(Random.nextDouble())
	},
	// gets a random int between 0 and 1
	"rand-int" to fn@ { _, loc, args ->
		val (min, max) = getRange(loc, args, "rand-int");
		Value.Number(Random.nextInt(min, max+1).toDouble())
	},
	// location functions
	// shifts a location in the order (x, y, z, [yaw, pitch])
	"location-shift" to { _, loc, args ->
		argsOneOf(loc, args, "location-shift", 4, 6);
		var l = args[0].getLocation(loc).clone();
		var s = args.subList(1, args.size).map { it.getNum(loc) }.toMutableList();
		l.add(s[0], s[1], s[2]);
		if(s.size == 5) {
			l.yaw += s[4].toFloat();
			l.pitch += s[5].toFloat();
		}
		Value.Loc(l)
	},
	// list functions
	// generates a range from a (inclusive) to b (exclusive)
	"range" to fn@ { _, loc, args ->
		val (min, max) = getRange(loc, args, "range");
		if(min == max)
			return@fn Value.List(mutableListOf());
		return@fn Value.List((min..max-1).map { it -> Value.Number(it.toDouble()) }.toMutableList());
	},
	// get list value
	"list-get" to fn@ { _, loc, args ->
		argsEqual(loc, args, "list-get", 2);
		val list = args[0].getList(loc);
		val idx = args[1].getNum(loc).toInt();
		list.boundsCheck(loc, idx);
		return@fn list[idx];
	},
	// set list value
	"list-set" to fn@ { _, loc, args ->
		argsEqual(loc, args, "list-get", 3);
		val list = args[0].getList(loc);
		val idx = args[1].getNum(loc).toInt();
		list.boundsCheck(loc, idx);
		val v = list[idx];
		list[idx] = args[2];
		return@fn v;
	},
	// push to end of list
	"list-push" to fn@ { _, loc, args ->
		argsAtLeast(loc, args, "list-push", 2);
		val list = args[0].getList(loc);
		list.addAll(args.subList(1, args.size));
		return@fn args[0]
	},
	// concatenate to list
	"list-cat" to fn@ { _, loc, args ->
		argsAtLeast(loc, args, "list-cat", 2)
		for(v in args.subList(1, args.size)) {
			args[0].getList(loc).addAll(v.getList(loc));
		}
		return@fn args[0];
	},
	// remove from list
	"list-remove" to fn@ { _, loc, args ->
		argsEqual(loc, args, "list-remove", 2);
		val list = args[0].getList(loc);
		val idx = args[1].getNum(loc).toInt();
		list.boundsCheck(loc, idx);
		return@fn list.removeAt(idx);
	},
	// get list length
	"list-length" to fn@ { _, loc, args ->
		argsEqual(loc, args, "list-length", 1);
		Value.Number(args[0].getList(loc).size.toDouble());
	},
	// map operations
	// get a map value
	"map-get" to { _, loc, args ->
		argsEqual(loc, args, "map-get", 2);
		val map = args[0].getMap(loc);
		val key = args[1].toString();
		map.boundsCheck(loc, key);
		map[key]!!
	},
	// set a map value
	"map-set" to { _, loc, args ->
		argsEqual(loc, args, "map-get", 3);
		val map = args[0].getMap(loc);
		val key = args[1].toString();
		map[key] = args[2];
		Value.Unit
	},
	// test if a map has a value
	"map-has" to { _, loc, args ->
		argsEqual(loc, args, "map-has", 2);
		Value.Bool(args[0].getMap(loc).containsKey(args[1].toString()));
	},
	// conversions
	"to-string" to { _, loc, args ->
		argsEqual(loc, args, "to-string", 1);
		Value.String(args[0].toString())
	},
	// returns Unit on failure
	"to-number" to { _, loc, args ->
		argsEqual(loc, args, "to-number", 1);
		val v = args[0];
		when(v) {
			is Value.Number -> v
			is Value.String -> {
				val s = v.string.toDoubleOrNull()
				if(s == null)
					Value.Unit
				else
					Value.Number(s)
			}
			is Value.Bool -> Value.Number(if(v.bool) 1.0 else 0.0)
			else -> Value.Unit
		}
	},
	// misc
	// logs to the unit's log
	"log" to { exec, _, args ->
		exec.ctx.unit.log(args.joinToString(""));
		Value.Unit
	},
	// cancels event
	"set-cancelled" to { exec, loc, args ->
		argsEqual(loc, args, "set-cancelled", 1);
		val evt = exec.ctx.event;
		if(evt !is CCancellable) {
			if(evt == null)
				throw ExecutionException(loc, "No event to cancel.");
			throw ExecutionException(loc, "Event '${evt.name}' is not cancellable.");
		}
		var b = args[0].getBool(loc);
		runTask { evt.cancelled = b }
		Value.Unit
	},
	// broadcasts a message
	"broadcast" to { _, _, args ->
		Utils.sendMessage("global", args.joinToString(""));
		Value.Unit
	}
);
