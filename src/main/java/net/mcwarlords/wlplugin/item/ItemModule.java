package net.mcwarlords.wlplugin.item;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;
import org.bukkit.command.*;
import java.util.*;

public class ItemModule implements Module {

  @Override public void onEnable() {
    WlPlugin.addCommand("wlitem", new ItemCommand(), new TabCompleter() {
		public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
			if(args.length == 1) {
				return List.of(
					"h", "help",
					"n", "rename",
					"l", "setlore",
					"a", "addlore",
					"r", "rmlore",
					"hf", "hideflags",
					"sf", "showflags",
					"u", "unsign",
					"ub", "unbreakable"
				);
			}
			if(args.length == 3 && (args[0].equals("l") || args[0].equals("setlore")) && sender instanceof Player) {
				Player p = (Player)sender;
				try {
					int index = Integer.valueOf(args[1])-1;
					if(index < 0)
						return List.of();
					ItemStack is = p.getInventory().getItemInMainHand();
					ItemMeta im = is.getItemMeta();
					if(im == null)
						return List.of();
          List<String> lore = im.getLore();
					if(lore == null)
						return List.of();
					if(index >= lore.size())
						return List.of();
					return List.of(Utils.unescapeText(lore.get(index)));	
				} catch(NumberFormatException e) {}
			}
			return List.of();
		}	
		});
    for(String alias : new String[]{"rename", "setlore", "addlore", "rmlore", "insertlore", "inlore", "showflags", "hideflags", "unsign", "unbreakable"}) {
      WlPlugin.addCommand(alias, new CommandExecutor() {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
          if(!(sender instanceof Player)) {
            sender.sendMessage("wlitem can only be used by a player");
            return false;
          }
          StringBuilder cmd = new StringBuilder("wlitem "+alias+" ");
          for(int i = 0; i < args.length; i++) {
            if(i != 0)
              cmd.append(" ");
            cmd.append(args[i]);
          }
          ((Player)sender).performCommand(cmd.toString());
        	return true;
				}  
      }, new TabCompleter() {
				public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
					// bad code duplication
					// TODO: deduplicate this when I port to kotlin
					if(args.length == 2 && (label.equals("setlore") || label.equals("relore")) && sender instanceof Player) {
						Player p = (Player)sender;
						try {
							int index = Integer.valueOf(args[0])-1;
							if(index < 0)
								return List.of();
							ItemStack is = p.getInventory().getItemInMainHand();
							ItemMeta im = is.getItemMeta();
							if(im == null)
								return List.of();
							List<String> lore = im.getLore();
							if(lore == null)
								return List.of();
							if(index >= lore.size())
								return List.of();
							return List.of(Utils.unescapeText(lore.get(index)));	
						} catch(NumberFormatException e) {}
					}
					return List.of();
				}
				
			});
    }
    WlPlugin.info("wlitem enabled");
  }

  @Override public void onDisable() {
    WlPlugin.info("wlitem disabled");
  }
  
}
