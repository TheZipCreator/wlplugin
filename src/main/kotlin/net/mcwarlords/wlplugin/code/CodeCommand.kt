package net.mcwarlords.wlplugin.code;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.*;

import java.time.*;
import java.time.format.*;

import net.mcwarlords.wlplugin.*;

object CodeCommand : CommandExecutor {
	private fun sendHelpMessage(p: Player) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLCODE &_s]======"));
    p.sendMessage(Utils.escapeText("&_p/wlcode h | help &_s- &_dDisplays this help information."));
    p.sendMessage(Utils.escapeText("&_p/wlcode n | new <name> &_s- &_dCreates a new unit with the given name. NOTE: make sure there's nothing near (32 blocks), because this may destroy it."));
    p.sendMessage(Utils.escapeText("&_p/wlcode w | warp <name> &_s- &_dWarps to a given unit."));
    p.sendMessage(Utils.escapeText("&_p/wlcode b | build <name> &_s- &_dBuilds a unit. Should be called every time it is modified."));
		p.sendMessage(Utils.escapeText("&_p/wlcode d | delete <name> &_s- &_dDeletes a unit."));
		p.sendMessage(Utils.escapeText("&_p/wlcode s | subscribe <name> &_s- &_dSubscribes or unsubscribes to a unit."));
		p.sendMessage(Utils.escapeText("&_p/wlcode sl | subscribelist &_s- &_dGets a list of all units you're currently subscribed to."));
		p.sendMessage(Utils.escapeText("&_p/wlcode l | log <name> &_s- &_dPrints out the debug logs for a unit."));
		p.sendMessage(Utils.escapeText("&_p/wlcode cc | cacheclear <name> &_s- &_dClears the cache of a unit and builds it without loading cache."));
		p.sendMessage(Utils.escapeText("&_p/wlcode cv | cacheview <name> &_s- &_dViews the cache of a unit."));
		p.sendMessage(Utils.escapeText("&_p/wlcode ha | halt <name> &_s- &_dHalts a unit."));
		p.sendMessage(Utils.escapeText("&_p/wlcode t | threads [name] &_s- &_dViews all active threads. If [name] is specified, it views threads attached to that specific unit."));
    p.sendMessage(Utils.escapeText("&_p/wlcode m | mode &_s- &_dToggles code mode"));
	}

	val invalidArguments = Utils.escapeText("&_p* &_dInvalid Arguments");

  override fun onCommand(p: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
    if(!(p is Player)) {
      p.sendMessage("wlmisc can only be used by a player");
      return false;
    }
    if(args.size == 0) {
      sendHelpMessage(p);
      return true;
    }
    val pd = Data.getPlayerData(p);
		fun buildUnit(u: CodeUnit, loadCache: Boolean = true) {
			p.sendMessage(Utils.escapeText("&_p* &_dBuilding unit &_e${u.name}&_d..."));
			try {
				u.build(loadCache);
				p.sendMessage(Utils.escapeText("&_p* &_dFinished building unit."));
			} catch(e: ParseException) {
				p.sendMessage(Utils.escapeText("&_p* &_eError building unit: ${e.toChatString()}"));
			}
		}
		fun getCodeUnit(owned: Boolean = false): CodeUnit? {
			if(args.size != 2) {
				p.sendMessage(invalidArguments);
				return null;
			}
			val name = args[1];
			if(!Data.codeUnits.contains(name)) {
				p.sendMessage(Utils.escapeText("&_p* &_eUnknown unit &_e$name&_d."));
				return null;
			}
			val cu = Data.codeUnits[name]!!;
			if(owned) {
				if(cu.owner != Data.uuidOf(p.name)) {
					p.sendMessage(Utils.escapeText("&_p* &_eYou do not own unit $name."));
					return null;
				}
			}
			return cu;
		}
		when(args[0]) {
			"h", "help" -> sendHelpMessage(p)
			"n", "new" -> run {
				if(args.size != 2) {
					p.sendMessage(invalidArguments);
					return@run;
				}
				val name = args[1];
				if(Data.codeUnits.contains(name)) {
					p.sendMessage(Utils.escapeText("&_p* &_eUnit $name already exists."));
					return@run;
				}
				p.sendMessage(Utils.escapeText("&_p* &_dCreating unit...&_d."));
				var l = p.location.block.location;
				l.y = 319.0;
				var cu = CodeUnit(l, name, Data.uuidOf(p.name));
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
				Data.codeUnits.put(name, cu);
				p.sendMessage(Utils.escapeText("&_p* &_dCreated unit &_e$name&_d."));
			}
			"w", "warp" -> run {
				if(args.size != 2) {
					p.sendMessage(invalidArguments);
					return@run;
				}
				val name = args[1];
				if(!Data.codeUnits.contains(name)) {
					p.sendMessage(Utils.escapeText("&_p* &_eUnknown unit &_e$name&_d."));
					return@run;
				}
				p.teleport(Data.codeUnits[name]!!.location.clone().add(0.0, 2.0, 0.0));
				p.sendMessage(Utils.escapeText("&_p* &_dTeleported to unit &_e$name&_d."));
			}
			"m", "mode" -> toggleCodeMode(p)
			"b", "build" -> run {
				buildUnit(getCodeUnit() ?: return@run);
			}
			"s", "subscribe" -> run {
				val cu = getCodeUnit() ?: return@run;
				if(pd.subscribed.contains(cu.name)) {
					pd.subscribed.remove(cu.name);
					cu.handleEvent(CUnsubscribeEvent(p));
					p.sendMessage(Utils.escapeText("&_p* &_dUnsubscribed from unit &_e${cu.name}&_d."));
					return@run;
				}
				cu.handleEvent(CSubscribeEvent(p));
				pd.subscribed.add(cu.name);
				p.sendMessage(Utils.escapeText("&_p* &_dSubscribed to unit &_e${cu.name}&_d."));
			}
			"d", "delete" -> run {
				val cu = getCodeUnit(true) ?: return@run;
				Data.codeUnits.remove(cu.name);
				p.sendMessage(Utils.escapeText("&_p* &_dUnit &_e${cu.name}&_d deleted. Note: Deleting a unit does not physically delete it; you have to do that yourself."));
			}
			"l", "log" -> run {
				val cu = getCodeUnit() ?: return@run;
				p.sendMessage(Utils.escapeText("&_p* &_dLog:"));
				for(msg in cu.log)
					p.sendMessage(Utils.escapeText(msg));
			}
			"cc", "cacheclear" -> run {
				val cu = getCodeUnit(true) ?: return@run;
				Data.cacheClear(cu.name);
				p.sendMessage(Utils.escapeText("&_p* &_dCleared cache."));
				buildUnit(cu, false);
			}
			"cv", "cacheview" -> run {
				val cu = getCodeUnit(true) ?: return@run;
				var obj = Data.cacheObject(cu.name);
				obj.put("__type", "map"); // make it readable by Value.deserialize()
				p.sendMessage(Value.deserialize(obj).toString());
			}
			"ha", "halt" -> run {
				val cu = getCodeUnit(true) ?: return@run;
				cu.halt();
				p.sendMessage(Utils.escapeText("&_p* &_dHalted unit ${cu.name}."));
			}
			"sl", "subscribelist" -> run {
				if(args.size != 1) {
					p.sendMessage(invalidArguments);
					return@run;
				}
				p.sendMessage(Utils.escapeText("&_dYour currently subscribed units are: ${pd.subscribed.map { "&_e$it" }.joinToString("&_d, ")}&_d."));
			}
			"t", "threads" -> run {
				var units = listOf<CodeUnit>();
				if(args.size == 1)
					units = Data.codeUnits.values.toMutableList();
				else
					units = listOf(getCodeUnit() ?: return@run);
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
			else -> p.sendMessage(Utils.escapeText("&_p* &_dInvalid subcommand."))
		}
		return true;
	}
}
