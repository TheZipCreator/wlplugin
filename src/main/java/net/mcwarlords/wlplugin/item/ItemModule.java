package net.mcwarlords.wlplugin.item;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcwarlords.wlplugin.*;

public class ItemModule implements Module {

  @Override public void onEnable() {
    WlPlugin.addCommand("wlitem", new ItemCommand());
    for(String alias : new String[]{"rename", "setlore", "addlore", "rmlore", "showflags", "hideflags", "unsign", "unbreakable"}) {
      WlPlugin.addCommand(alias, new CommandExecutor() {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
          if(!(sender instanceof Player)) {
            sender.sendMessage("wlitem can only be used by a player");
            return false;
          }
          // should use stringbuilder but I don't care. TODO
          String cmd = "wlitem "+alias+" ";
          for(int i = 0; i < args.length; i++) {
            if(i != 0)
              cmd += " ";
            cmd += args[i];
          }
          ((Player)sender).performCommand(cmd);
          return true;
        }
        
      });
    }
    WlPlugin.info("wlitem enabled");
  }

  @Override public void onDisable() {
    WlPlugin.info("wlitem disabled");
  }
  
}
