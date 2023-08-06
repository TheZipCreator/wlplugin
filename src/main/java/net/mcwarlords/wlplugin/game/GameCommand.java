package net.mcwarlords.wlplugin.game;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import net.mcwarlords.wlplugin.*;

public class GameCommand implements CommandExecutor {
	private void sendHelpMessage(Player p) {
		p.sendMessage(Utils.escapeText("&_s======[ &_eWLGAME &_s]======"));
		p.sendMessage(Utils.escapeText("&_p/wlgame h | help &_s- &_dDisplays this help message."));
		p.sendMessage(Utils.escapeText("&_p/wlgame n | new &_e<ID> &_s- &_dCreates a new game with id &_e<ID>&_d at your current location."));
		p.sendMessage(Utils.escapeText("&_p/wlgame w | warp &_e<ID> &_s- &_dTeleports to the game with id &_e<ID>&_d."));
		p.sendMessage(Utils.escapeText("&_p/wlgame i | info &_e<ID> &_s- &_dDisplays info about game with id &_e<ID>&_d."));
		p.sendMessage(Utils.escapeText("&_p/wlgame l | list &_s- &_dLists all existing games."));
		p.sendMessage(Utils.escapeText("&_p/wlgame s | set &_e<ID> <name | description | location> [value] &_s- &_dSets a property of a given game."));
		p.sendMessage(Utils.escapeText("&_p/wlgame d | delete &_e<ID> &_s- &_dDeletes a game."));
	}

	static final String invalidArguments = Utils.escapeText("&_p* &_eInvalid arguments.");

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage("wlgame can only be used by a player");
			return false;
		}
		Player p = (Player)sender;
		if(args.length == 0) {
			sendHelpMessage(p);
			return true;
		}
		PlayerData pd = Data.getPlayerData(p);
		switch(args[0]) {
			case "n":
			case "new": {
				if(args.length != 2) {
					p.sendMessage(invalidArguments);
					return true;
				}
				String id = args[1];
				if(Data.games.containsKey(id)) {
					p.sendMessage(Utils.escapeText("&_p* &_eGame with ID &_p"+id+"&_e already exists."));
					return true;
				}
				GameData gd = new GameData(id);
				gd.author = Utils.getUUID(p);
				gd.dateCreated = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
				gd.location = p.getLocation();
				Data.games.put(id, gd);
				p.sendMessage(Utils.escapeText("&_p* &_dGame &_e"+id+"&_d created."));
				break;
			}
			case "w":
			case "warp": {
				if(args.length != 2) {
					p.sendMessage(invalidArguments);
					return true;
				}
				String id = args[1];
				if(!Data.games.containsKey(id)) {
					p.sendMessage(Utils.escapeText("&_p* &_eGame with ID &_p"+id+"&_e does not exist!"));
					return true;
				}
				p.teleport(Data.games.get(id).location);
				p.sendMessage(Utils.escapeText("&_p* &_dTeleported to game &_e"+id+"&_d."));
				return true;
			}
			case "i":
			case "info": {
				if(args.length != 2) {
					p.sendMessage(invalidArguments);
					return true;
				}
				String id = args[1];
				if(!Data.games.containsKey(id)) {
					p.sendMessage(Utils.escapeText("&_p* &_eGame with ID &_p"+id+"&_e does not exist!"));
					return true;
				}
				GameData gd = Data.games.get(id);
				p.sendMessage(Utils.escapeText("&_s-[ &_e"+gd.name+" &_s]-"));
				p.sendMessage(Utils.escapeText("&_pID &_s- &_d"+gd.id));
				p.sendMessage(Utils.escapeText("&_pTime Created &_s- &_d"+gd.dateCreated));
				p.sendMessage(Utils.escapeText("&_pAuthor &_s- &_d"+Data.nameOf(gd.author)));
				p.sendMessage(Utils.escapeText("&_pDescription &_s- &_d"+gd.description));
				break;
			}
			case "l":
			case "list": {
				List<String> list = Utils.getSortedGames();
				StringBuilder sb = new StringBuilder("&_sList of all &_e"+Integer.toString(list.size())+"&_s games:\n&_d");
				for(int i = 0; i < list.size(); i++) {
					if(i != 0)
						sb.append(", ");
					sb.append(list.get(i));
				}
				p.sendMessage(Utils.escapeText(sb.toString()));
				break;
			}
			case "s":
			case "set": {
				if(args.length < 3) {
					p.sendMessage(invalidArguments);
					break;
				}
				String id = args[1];
				if(!Data.games.containsKey(id)) {
					p.sendMessage(Utils.escapeText("&_p* &_eGame &_p"+id+"&_e does not exist!"));
					return true;
				}
				GameData gd = Data.games.get(id);
				if(!gd.author.equals(Utils.getUUID(p))) {
					p.sendMessage(Utils.escapeText("&_p* &_eYou do not own game &_p"+id+"&_e!"));
					return true;
				}
				if(args[2].equals("location")) {
					gd.location = p.getLocation();
				} else {
					String value;
					{
						StringBuilder sb = new StringBuilder();
						for(int i = 3; i < args.length; i++) {
							if(i != 3)
								sb.append(" ");
							sb.append(args[i]);
						}
						value = sb.toString();
					}
					switch(args[2]) {
						case "name":
							gd.name = value;
							break;
						case "description":
							gd.description = value;
							break;
						default:
							p.sendMessage(Utils.escapeText("&_p* &_eUnknown property "+args[2]+"."));
							return true;
					}
				}
				p.sendMessage(Utils.escapeText("&_p* &_dSuccessfully set property "+args[2]));
				break;
			}
			case "d":
			case "delete":
				if(args.length != 2) {
					p.sendMessage(invalidArguments);
					return true;
				}
				String id = args[1];
				if(!Data.games.containsKey(id)) {
					p.sendMessage(Utils.escapeText("&_p* &_eGame with ID &_p"+id+"&_e does not exist!"));
					return true;
				}
				GameData gd = Data.games.get(id);
				if(!gd.author.equals(Utils.getUUID(p))) {
					p.sendMessage(Utils.escapeText("&_p* &_eYou do not own game &_p"+id+"&_e!"));
					return true;
				}
				Data.games.remove(id);
				p.sendMessage(Utils.escapeText("&_p* &_dGame &_e"+id+"&_d has been removed."));
				break;
			case "h":
			case "help":
				sendHelpMessage(p);
				break;
			default:
				p.sendMessage(Utils.escapeText("&_p* &_eInvalid subcommand "+args[0]+"."));
		}
		return true;
	}
}
