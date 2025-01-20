package net.mcwarlords.wlplugin.game;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import net.mcwarlords.wlplugin.*;

object GameCommand : ModuleCommand {
	override val name = "wlgame";
	override val clazz = GameCommand::class;

	val gameCompletionList = listOf("w", "warp", "i", "info", "s", "set", "d", "delete");

	override fun complete(args: Array<String>): MutableList<String> {
		if(args.size == 2 && gameCompletionList.contains(args[0])) {
			return Utils.getSortedGames().filter { it.contains(args[1]) }.toMutableList();
		}
		return mutableListOf();
	}

	@SubCommand(["n", "new"], "Creates a new game with ID &_e<id>&_d at your current location.") fun new(@CommandPlayer p: Player, id: String) {
		if(Data.games.containsKey(id)) {
			p.sendEscaped("&_p* &_eGame with ID &_p"+id+"&_e already exists.");
			return;
		}
		val gd = GameData(id);
		gd.author = Utils.getUUID(p);
		gd.dateCreated = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Date());
		gd.location = p.getLocation();
		Data.games.put(id, gd);
		p.sendEscaped("&_p* &_dGame &_e"+id+"&_d created.");
	}
	@SubCommand(["w", "warp"], "Teleports to the game with ID &_e<id>&_d.") fun warp(@CommandPlayer p: Player, id: String) {
		if(!Data.games.containsKey(id)) {
			p.sendEscaped("&_p* &_eGame with ID &_p"+id+"&_e does not exist!");
		}
		p.teleport(Data.games.get(id)!!.location);
		p.sendEscaped("&_p* &_dTeleported to game &_e"+id+"&_d.");
	}
	@SubCommand(["i", "info"], "Displays info about the game with ID &_e<id>&_d.") fun info(@CommandPlayer p: Player, id: String) {
		if(!Data.games.containsKey(id)) {
			p.sendEscaped("&_p* &_eGame with ID &_p"+id+"&_e does not exist!");
			return;
		}
		val gd = Data.games.get(id)!!;
		p.sendEscaped("&_s-[ &_e"+gd.name+" &_s]-");
		p.sendEscaped("&_pID &_s- &_d"+gd.id);
		p.sendEscaped("&_pTime Created &_s- &_d"+gd.dateCreated);
		p.sendEscaped("&_pAuthor &_s- &_d"+Data.nameOf(gd.author));
		p.sendEscaped("&_pDescription &_s- &_d"+gd.description);
	}
	@SubCommand(["l", "list"], "Lists all existing games") fun list(@CommandPlayer p: Player) {
		val list = Utils.getSortedGames();
		p.sendEscaped(buildString {
			append("&_sList of all &_e${list.size}&_s games:\n&_d");
			list.forEachIndexed { i, game ->
				if(i != 0)
					append(", ");
				append(game);
			}
		});
	}
	enum class Property(val propName: String) {
		LOCATION("location"), L("location"), 
		NAME("name"), N("name"), 
		DESCRIPTION("description"), D("description")
	}
	@SubCommand(["s", "set"], "Sets a property on a game.") fun set(@CommandPlayer p: Player, id: String, prop: Property, vararg value: String) {
		if(!Data.games.containsKey(id)) {
			p.sendEscaped("&_p* &_eGame &_p"+id+"&_e does not exist!");
			return;
		}
		val gd = Data.games.get(id)!!;
		if(gd.author != Utils.getUUID(p)) {
			p.sendEscaped("&_p* &_eYou do not own game &_p"+id+"&_e!");
			return;
		}
		if(prop == Property.LOCATION || prop == Property.L) {
			gd.location = p.location;
		} else {
			val v = value.joinToString(" ");
			when(prop) {
				Property.NAME, Property.N -> gd.name = v
				Property.DESCRIPTION, Property.D -> gd.description = v
				else -> {
					p.sendEscaped("&_p* &_eUnknown property $prop."); 
					return;
				}
			}
		}
		p.sendEscaped("&_p* &_dSuccessfully set ${prop.propName}.");
	}
	@SubCommand(["d", "delete"], "Deletes the game with ID &_e<ID>&_d.") fun set(@CommandPlayer p: Player, id: String) {
		if(!Data.games.containsKey(id)) {
			p.sendEscaped("&_p* &_eGame with ID &_p"+id+"&_e does not exist!");
			return;
		}
		val gd = Data.games.get(id)!!;
		if(gd.author != Utils.getUUID(p)) {
			p.sendEscaped("&_p* &_eYou do not own game &_p"+id+"&_e!");
			return;
		}
		Data.games.remove(id);
		p.sendEscaped("&_p* &_dGame &_e"+id+"&_d has been removed.");
	}
}
