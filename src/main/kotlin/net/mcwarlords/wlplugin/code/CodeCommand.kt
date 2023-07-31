package net.mcwarlords.wlplugin.code;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.*;

import net.mcwarlords.wlplugin.*;

class CodeCommand : CommandExecutor {
	private fun sendHelpMessage(p: Player) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLCODE &_s]======"));
    p.sendMessage(Utils.escapeText("&_p/wlcode h | help &_s- &_dDisplays this help information."));
    p.sendMessage(Utils.escapeText("&_p/wlcode n | new <name> &_s- &_dCreates a new unit with the given name. NOTE: make sure there's nothing near (32 blocks), because this may destroy it."));
    p.sendMessage(Utils.escapeText("&_p/wlcode w | warp <name> &_s- &_dWarps to a given unit."));
    p.sendMessage(Utils.escapeText("&_p/wlcode r | reload <name> &_s- &_dReloads a unit. Should be called every time it is modified."));
    p.sendMessage(Utils.escapeText("&_p/wlcode m | codemode &_s- &_dToggles code mode"));
	}

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
		when(args[0]) {
			"h", "help" -> sendHelpMessage(p)
			"n", "new" -> run {
				if(args.size != 2) {
					p.sendMessage(Utils.escapeText("&_p* &_dInvalid arguments."));
					return@run;
				}
				val name = args[1];
				if(Data.codeUnits.contains(name)) {
					p.sendMessage(Utils.escapeText("&_p* &_eUnit $name already exists."));
					return@run;
				}
				var l = p.location.block.location;
				l.y = 319.0;
				var cu = CodeUnit(l, Data.uuidOf(p.name));
				var w = l.world!!;
				for(y in -64..319) {
					for(x in 0..8) {
						for(z in 0..32) {
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
					p.sendMessage(Utils.escapeText("&_p* &_dInvalid arguments."));
					return@run;
				}
				val name = args[1];
				if(!Data.codeUnits.contains(name)) {
					p.sendMessage(Utils.escapeText("&_p* &_eUnknown unit $name."));
					return@run;
				}
				p.teleport(Data.codeUnits[name]!!.location.clone().add(0.0, 2.0, 0.0));
				p.sendMessage(Utils.escapeText("&_p* &_dTeleported to unit &_e$name&_d."));
			}
			"m", "codemode" -> toggleCodeMode(p)
			else -> p.sendMessage(Utils.escapeText("&_p* &_dInvalid subcommand."))
		}
		return true;
	}
}
