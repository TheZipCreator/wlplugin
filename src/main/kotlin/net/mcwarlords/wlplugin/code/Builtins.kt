package net.mcwarlords.wlplugin.code;

import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.projectiles.*;
import org.bukkit.potion.*
import org.bukkit.util.*;
import kotlin.math.*;
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

private fun Value.getPlayer(loc: Location): Player {
	if(this !is Value.Entity || entity !is Player)
		throw ExecutionException(loc, "Player expected.");
	return entity;
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

private fun Value.getItem(loc: Location): ItemStack {
	if(this !is Value.Item)
		throw ExecutionException(loc, "Item expected, got ${typeName()}.");
	return this.item;
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
		max = args[1].getNum(loc).toInt();
	}
	if(max < min)
		min = max.also { max = min };
	return Pair(min, max);
}

private inline fun <reified T : Enum<T>> Value.toEnum(loc: Location): T {
	val name = toString();
	try {
		return java.lang.Enum.valueOf(T::class.java, name.uppercase().replace('-', '_'));
	} catch(e: IllegalArgumentException) {
		throw ExecutionException(loc, "Invalid ${T::class.simpleName} '$name'");
	}
}

private fun potionTypeFromValue(loc: Location, v: Value): PotionEffectType =
	PotionEffectType.getByName(v.toString().uppercase().replace('-', '_')) ?: throw ExecutionException(loc, "Invalid potion effect type '$v'");



internal val builtins = mapOf<String, Builtin>(
	// player actions
	// sends a message to a player
	"send-message" to fn@ { _, loc, args ->
		argsAtLeast(loc, args, "send-message", 1);
		val player = args[0].getPlayer(loc);
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
		val player = args[0].getPlayer(loc);
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
		Value.Item(args[0].getPlayer(loc).inventory.itemInMainHand);
	},
	// gets the item a player is holding in their off hand
	"off-hand-item" to fn@ { _, loc, args ->
		argsEqual(loc, args, "held-item", 1);
		Value.Item(args[0].getPlayer(loc).inventory.itemInOffHand);
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
	"set-gamemode" to fn@ { _, loc, args ->
		argsEqual(loc, args, "gamemode", 2);
		val player = args[0].getPlayer(loc);
		val gameMode = args[1].toEnum<GameMode>(loc);
		runTask { player.gameMode = gameMode }
		Value.Unit
	},
	"subscribed?" to fn@ { exec, loc, args ->
		argsEqual(loc, args, "subscribed?", 1)
		val player = args[0].getPlayer(loc);
		val pd = Data.getPlayerData(player);
		Value.Bool(exec.ctx.unit.name in pd.subscribed);
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
		val type = args[0].toEnum<EntityType>(loc);
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
		val type = args[1].toEnum<EntityType>(loc);
		val clazz = type.getEntityClass();
		if(!Projectile::class.java.isAssignableFrom(type.entityClass!!))
			throw ExecutionException(loc, "Entity type '${args[1]}' is not a projectile.");
		var e: AtomicReference<Entity?> = AtomicReference(null);
		runTask {
			e.`set`(source.launchProjectile(clazz as Class<Projectile>)); // honestly surprised this cast even works
		}
		while(e.`get`() == null);
		Value.Entity(e.`get`()!!);
	},
	// damages an entity
	"damage" to { _, loc, args ->
		argsEqual(loc, args, "damage", 2);
		val entity = args[0].getEntity(loc);
		if(entity !is Damageable)
			throw ExecutionException(loc, "Entity is not damageable.");
		val amt = args[1].getNum(loc);
		runTask { entity.damage(amt); }
		Value.Unit
	},
	"effect" to { _, loc, args ->
		argsBetween(loc, args, "effect", 4, 7);
		val entity = args[0].getEntity(loc);
		if(entity !is LivingEntity)
			throw ExecutionException(loc, "Can not apply potion effect to entity.");
		val type = potionTypeFromValue(loc, args[0]);
		val duration = args[2].getNum(loc).toInt();
		val amplifier = args[3].getNum(loc).toInt();
		val effect = when(args.size) {
			4 -> PotionEffect(type, duration, amplifier)
			5 -> PotionEffect(type, duration, amplifier, args[4].getBool(loc))
			6 -> PotionEffect(type, duration, amplifier, args[4].getBool(loc), args[5].getBool(loc))
			7 -> PotionEffect(type, duration, amplifier, args[4].getBool(loc), args[5].getBool(loc), args[6].getBool(loc))
			else -> throw ExecutionException(loc, "This should not appear.")
		}
		runTask { entity.addPotionEffect(effect); }
		Value.Unit
	},
	// gets the location of an entity
	"location-of" to { _, loc, args ->
		argsEqual(loc, args, "location-of", 1);
		Value.Loc(args[0].getEntity(loc).location)
	},
	"kill" to { _, loc, args ->
		argsEqual(loc, args, "kill", 1);
		val entity = args[0].getEntity(loc);
		if(entity is Damageable)
			runTask { entity.setHealth(0.0) }
		else
			runTask { entity.remove() }
		Value.Unit
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
		var s = args.subList(1, args.size).map { it.getNum(loc) };
		l.add(s[0], s[1], s[2]);
		if(s.size == 5) {
			l.yaw += s[4].toFloat();
			l.pitch += s[5].toFloat();
		}
		Value.Loc(l)
	},
	"location-shift-forwards" to { _, loc, args ->
		argsEqual(loc, args, "location-shift-forwards", 2);
		var l = args[0].getLocation(loc).clone();
		val amt = args[1].getNum(loc);
		l.add(l.getDirection().multiply(amt));
		Value.Loc(l);
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
	// world
	"set-block" to { _, loc, args ->
		argsEqual(loc, args, "set-block", 2);
		val l = args[0].getLocation(loc);
		val type = args[1].getItem(loc).type;
		if(!type.isBlock())
			throw ExecutionException(loc, "Item type ${type.lispCase()} is not a block.");
		runTask { l.world?.getBlockAt(l)?.type = type }
		Value.Unit
	},
	"get-block" to { _, loc, args ->
		argsEqual(loc, args, "get-block", 1);
		val l = args[0].getLocation(loc);
		var mat: AtomicReference<Material?> = AtomicReference(null);
		runTask { mat.`set`(l.world?.getBlockAt(l)?.type ?: Material.AIR) }
		while(mat.`get`() == null);
		Value.Item(ItemStack(mat.`get`()!!, 1));
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
		Utils.sendMessage("global", args.joinToString("").replace("§", "&"));
		Value.Unit
	},
	"cache-set" to { exec, loc, args ->
		argsEqual(loc, args, "cache-set", 2);
		val name = args[0].toString();
		Data.cacheSet(exec.ctx.unit.name, name, args[1]);
		Value.Unit
	},
	"cache-get" to fn@ { exec, loc, args ->
		argsEqual(loc, args, "cache-get", 1);
		val name = args[0].toString();
		Data.cacheGet(exec.ctx.unit.name, name) ?: Value.Unit
	},
	"spawn-particle" to { _, loc, args ->
		argsEqual(loc, args, "spawn-particle", 3);
		val type = args[0].toEnum<Particle>(loc);
		val l = args[1].getLocation(loc);
		val count = args[2].getNum(loc).toInt();
		runTask { loc.world?.spawnParticle(type, l, count, 0.0, 0.0, 0.0, 0.0) }
		Value.Unit
	},
	"online-players" to { _, _, _ ->
		Value.List(Bukkit.getServer().onlinePlayers.map { Value.Entity(it) }.toMutableList())
	},
	"play-sound" to { _, loc, args ->
		argsBetween(loc, args, "play-sound", 4, 5);
		val sound = args[0].toEnum<Sound>(loc);
		val l = args[1].getLocation(loc);
		val volume = args[2].getNum(loc).toFloat();
		val pitch = args[3].getNum(loc).toFloat();
		if(args.size == 4)
			runTask { l.world?.playSound(l, sound, volume, pitch) }
		else {
			val player = args[4].getPlayer(loc);
			runTask { player.playSound(l, sound, volume, pitch) }
		}
		Value.Unit
	}
);
