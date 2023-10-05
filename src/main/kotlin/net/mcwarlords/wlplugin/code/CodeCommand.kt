package net.mcwarlords.wlplugin.code;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.*;

import java.time.*;
import java.time.format.*;

import net.mcwarlords.wlplugin.*;

object CodeCommand : ModuleCommand {
	override val name = "wlcode";
	override val clazz = CodeCommand::class;
	
	fun buildUnit(p: Player, u: CodeUnit, loadCache: Boolean = true) {
		p.sendMessage(Utils.escapeText("&_p* &_dBuilding unit &_e${u.name}&_d..."));
		try {
			u.build(loadCache);
			p.sendMessage(Utils.escapeText("&_p* &_dFinished building unit."));
		} catch(e: CodeException) {
			p.sendMessage(Utils.escapeText("&_p* &_eError building unit: ${e.toChatString()}"));
		}
	}
	fun getCodeUnit(p: Player, name: String, owned: Boolean = false): CodeUnit? {
		if(!codeUnits.contains(name)) {
			p.sendMessage(Utils.escapeText("&_p* &_eUnknown unit &_e$name&_d."));
			return null;
		}
		val cu = codeUnits[name]!!;
		if(owned) {
			if(cu.owner != Data.uuidOf(p.name)) {
				p.sendMessage(Utils.escapeText("&_p* &_eYou do not own unit $name."));
				return null;
			}
		}
		return cu;
	}

	@SubCommand(["n", "new"], "Creates a new unit with the given name. NOTE: make sure there's nothing near(32 blocks), because this may destroy it.") 
	fun new(@CommandPlayer p: Player, name: String) {
		if(codeUnits.contains(name)) {
			p.sendMessage(Utils.escapeText("&_p* &_eUnit $name already exists."));
			return;
		}
		p.sendMessage(Utils.escapeText("&_p* &_dCreating unit...&_d."));
		var l = p.location.block.location;
		l.y = 319.0;
		var cu = CodeUnit(l, name, Data.uuidOf(p.name)!!);
		var w = l.world!!;
		for(y in -64..319) {
			for(x in 0..8) {
				for(z in 0..CodeUnit.WIDTH) {
					var b = w.getBlockAt((l.x+x).toInt(), y, (l.z-z).toInt());
					b.type = when(x) {
						0 -> Material.BLACK_CONCRETE
						1 -> Material.BLACK_STAINED_GLASS
						else -> Material.AIR
					};
				}
			}
		}
		p.teleport(l.clone().add(0.0, 2.0, 0.0));
		codeUnits.put(name, cu);
		p.sendMessage(Utils.escapeText("&_p* &_dCreated unit &_e$name&_d."));
	}
	
	@SubCommand(["w", "warp"], "Warps to a given unit.") 
	fun warp(@CommandPlayer p: Player, name: String) {
		if(!codeUnits.contains(name)) {
			p.sendMessage(Utils.escapeText("&_p* &_eUnknown unit &_e$name&_d."));
			return;
		}
		p.teleport(codeUnits[name]!!.location.clone().add(0.0, 2.0, 0.0));
		p.sendMessage(Utils.escapeText("&_p* &_dTeleported to unit &_e$name&_d."));
	}
	
	@SubCommand(["m", "mode"], "Toggles code mode.")
	fun mode(@CommandPlayer p: Player) = toggleCodeMode(p);

	@SubCommand(["b", "build"], "Builds a unit. Should be ran every time it is modified.")
	fun build(@CommandPlayer p: Player, name: String) {
		buildUnit(p, getCodeUnit(p, name) ?: return);
	}

	@SubCommand(["s", "subscribe"], "Subscribes or unsubscribes to a unit.")
	fun subscribe(@CommandPlayer p: Player, name: String) {
		val pd = Data.getPlayerData(p);
		val cu = getCodeUnit(p, name) ?: return;
		if(pd.subscribed.contains(cu.name)) {
			pd.subscribed.remove(cu.name);
			cu.handleEvent(CUnsubscribeEvent(p));
			p.sendMessage(Utils.escapeText("&_p* &_dUnsubscribed from unit &_e${cu.name}&_d."));
			return;
		}
		cu.handleEvent(CSubscribeEvent(p));
		pd.subscribed.add(cu.name);
		p.sendMessage(Utils.escapeText("&_p* &_dSubscribed to unit &_e${cu.name}&_d."));
	}

	@SubCommand(["d", "delete"], "Deletes a code unit.")
	fun delete(@CommandPlayer p: Player, name: String) {
		val cu = getCodeUnit(p, name, true) ?: return;
		codeUnits.remove(cu.name);
		p.sendMessage(Utils.escapeText("&_p* &_dUnit &_e${cu.name}&_d deleted. Note: Deleting a unit does not physically delete it; you have to do that yourself."));
	}

	@SubCommand(["l", "log"], "Prints out the debug logs for a unit.")
	fun log(@CommandPlayer p: Player, name: String) {
		val cu = getCodeUnit(p, name) ?: return;
		p.sendMessage(Utils.escapeText("&_p* &_dLog:"));
		for(msg in cu.log)
			p.sendMessage(Utils.escapeText(msg));
	}

	@SubCommand(["cc", "cacheclear"], "Clears the cache of a unit and builds it without loading cache.")
	fun cacheClear(@CommandPlayer p: Player, name: String) {
		val cu = getCodeUnit(p, name, true) ?: return;
		Data.cacheClear(cu.name);
		p.sendMessage(Utils.escapeText("&_p* &_dCleared cache."));
		buildUnit(p, cu, false);
	}

	@SubCommand(["ha", "halt"], "Halts a unit.")
	fun halt(@CommandPlayer p: Player, name: String) {
		val cu = getCodeUnit(p, name, true) ?: return;
		cu.halt();
		p.sendMessage(Utils.escapeText("&_p* &_dHalted unit ${cu.name}."));
	}

	@SubCommand(["sl", "subscribelist"], "Gets a list of all units you're currently subscribed to.")
	fun subscribeList(@CommandPlayer p: Player) {
		val pd = Data.getPlayerData(p);
		p.sendMessage(Utils.escapeText("&_dYour currently subscribed units are: ${pd.subscribed.map { "&_e$it" }.joinToString("&_d, ")}&_d."));
	}
	
	@SubCommand(["t", "threads"], "Views all active threads. If [name] is specified, it views threads attached to that specific unit.")
	fun threads(@CommandPlayer p: Player, name: String? = null) {
		var units = 
			if(name == null)
				codeUnits.values.toMutableList()
			else
				listOf(getCodeUnit(p, name) ?: return);
		p.sendMessage(Utils.escapeText("&_p* &_dActive threads:"));
		for(u in units) {
			if(u.executors.size == 0)
				continue;
			p.sendMessage(Utils.escapeText("&_e${u.name}:"));
			var amt = 0;
			for(e in u.executors) {
				amt += 1;
				if(amt > 10) {
					p.sendMessage(Utils.escapeText("&_p* &_d... ${u.executors.size-amt} more threads."));
					break;
				}
				p.sendMessage(Utils.escapeText("&_p* &3[${formatInstant(e.ctx.startTime)}] &_d${e.ctx.event?.name ?: "unknown"}"));
			}
		}
	}
}
