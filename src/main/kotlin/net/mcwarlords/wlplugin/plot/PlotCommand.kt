package net.mcwarlords.wlplugin.plot;

import org.bukkit.command.*;
import org.bukkit.entity.*;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.util.*;

object PlotCommand : ModuleCommand {
	override val name = "wlplot";
	override val clazz = PlotCommand::class;
	
	@SubCommand(["c", "claim"], "Claims a plot and teleports you to its center.") fun claim(@CommandPlayer p: Player) {
		if(p.data.plots <= 0) {
			p.sendEscaped("&_p* &_eYou cannot claim any more plots.");
			return;
		}
		p.data.plots--;
		val id = Data.plotOwners.size;
		Data.plotOwners.add(Utils.getUUID(p));
		val bb = Utils.plot(id);
		p.teleport(bb.center().toLocation());
		p.sendEscaped("&_p* &_dSuccessfully claimed plot. (Plot ID &_e#${id}&_d)");
		p.sendEscaped("&_p* &_dYou have &_e${p.data.plots}&_d plots remaining.");
	}

	@SubCommand(["w", "wild"], "Teleports you to a random location within one of your plots. If [player] is specified, it teleports you to a random location within one of that player's plots.") fun wild(@CommandPlayer p: Player, player: String = "") {
		val plots = if(player == "") Utils.plotsOwnedBy(p) else run {
			if(!Data.playerExists(player)) {
				p.sendEscaped("&_p* &_eUnknown player &_e${player}&_d.");
				return;
			}
			return@run Utils.plotsOwnedBy(Data.uuidOf(player)!!);
		}
		if(plots.size == 0) {
			p.sendEscaped("&_p* &_e${if(player == "") "You" else "They"} do not own any plots!");
			return;
		}
		p.teleport(Utils.plot(plots[Utils.randInt(0, plots.size)]).randomWithin().toLocation());
	}

	@SubCommand(["wi", "wildin"], "Teleports you to a random location in the given plot ID.") fun wildin(@CommandPlayer p: Player, plot: Int) {
		p.teleport(Utils.plot(plot).randomWithin().toLocation())
	}

	@SubCommand(["ce", "center"], "Teleports you to the center of a given plot ID.") fun center(@CommandPlayer p: Player, plot: Int) {
		p.teleport(Utils.plot(plot).center().toLocation())
	}
}
