package net.mcwarlords.wlplugin.plot;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import net.mcwarlords.wlplugin.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class PlotModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlplot enabled");
    WlPlugin.addListener(new PlotListener());
    WlPlugin.addCommand("wlplot", new PlotCommand());
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
                msg += "&_e"+Utils.getPlayer(Data.plotOwners.get(plotId)).getName()+"&_s's Plot ";
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
