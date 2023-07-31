package net.mcwarlords.wlplugin.code;

import org.bukkit.entity.*;
import org.bukkit.*;

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

internal val builtins = mapOf<String, (exec: Executor, loc: Location, args: List<Value>) -> Value>(
	"send-message" to fn@ { exec: Executor, loc: Location, args: List<Value> ->
		argsAtLeast(loc, args, "send-message", 1);
		val vplayer = args[0];
		if(vplayer !is Value.Entity)
			throw ExecutionException(loc, "First argument to send-message must be entity.");
		val player = vplayer.entity;
		if(player !is Player)
			throw ExecutionException(loc, "Entity is not player.");
		var msg = buildString {
			for(v in args.slice(1..args.size-1))
				append("$v");
		}
		runTask { player.sendMessage(msg); }
		return@fn Value.Unit;
	}
);
