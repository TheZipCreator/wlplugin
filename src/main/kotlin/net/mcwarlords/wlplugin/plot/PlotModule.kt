package net.mcwarlords.wlplugin.plot;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.scheduler.BukkitRunnable;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class PlotModule : SimpleModule {
	override val name = "wlplot";

	override fun registerFields() {}

	override fun enabled() {
		PlotCommand.register();
		
		object : BukkitRunnable() {
			override fun run() {
				for(p in Bukkit.getOnlinePlayers()) {
					if(p.getInventory().getItemInMainHand().getType() == Material.DEBUG_STICK)
						return; // don't interfere with debug stick
					val l = p.getLocation();
					if(l.getWorld() == Bukkit.getWorlds().get(0)) {
						val plotId = Utils.getPlotAt(l.getX().toInt(), l.getZ().toInt());
						val msg = buildString {
							append("&_pCurrently in: ");
							if(plotId == -1)
								append("&_sFree Area");
							else {
								if(plotId < Data.plotOwners.size)
									append("&_e"+Data.nameOf(Data.plotOwners.get(plotId))+"&_s's Plot ");
								else
									append("&_s Unowned Plot ");
								append("(#"+Integer.toString(plotId)+")");
							}
							
						}
						p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(Utils.escapeText(msg)));
					}
				}
			}
		}.runTaskTimer(WlPlugin.instance!!, 0, 10);
		WlPlugin.info("wlplot enabled");
	}

	override fun disabled() {
		WlPlugin.info("wlplot disabled");
	}
	
	
}
