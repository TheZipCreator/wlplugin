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

public class PlotModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlplot enabled");
    WlPlugin.addListener(new PlotListener());
    WlPlugin.addCommand("wlplot", new PlotCommand(), new TabCompleter() {
			public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
				if(args.length == 1)
					return List.of(
						"h", "help",
						"c", "claim",
						"w", "wild",
						"ce", "center"
					);
				if(args.length == 2) {
					if(Utils.isAny(args[0], "w", "wild"))
						return null;
				}
				return List.of();
			}	
		});
    new BukkitRunnable() {
      public void run() {
        for(Player p : Bukkit.getOnlinePlayers()) {
          Location l = p.getLocation();
          if(l.getWorld() == Bukkit.getWorlds().get(0)) {
            int plotId = Utils.getPlotAt((int)l.getX(), (int)l.getZ());
            String msg = "&_pCurrently in: ";
            if(plotId == -1)
              msg += "&_sFree Area";
            else {
              if(plotId < Data.plotOwners.size())
                msg += "&_e"+Data.nameOf(Data.plotOwners.get(plotId))+"&_s's Plot ";
              else
                msg += "&_s Unowned Plot ";
              msg += "(#"+Integer.toString(plotId)+")";
            }
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.escapeText(msg)));
          }
        }
      }
    }.runTaskTimer(WlPlugin.instance, 0, 10);
  }

  @Override public void onDisable() {
    WlPlugin.info("wlplot disabled");
  }
  
  
}
