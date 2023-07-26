package net.mcwarlords.wlplugin.misc;


import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;

import org.bukkit.event.*;
import org.bukkit.command.*;
import java.util.*;

public class MiscModule implements Module {

  @Override public void onEnable() {
    WlPlugin.addCommand("wlmisc", new MiscCommand(), new TabCompleter() {
		public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
				if(args.length == 1)
					return List.of(
						"h", "help",
						"r", "roll",
						"rh", "rollhere",
						"r", "region",
						"c", "compacify"
					);
				return List.of();
			}	
		});
		WlPlugin.addListener(new MiscListener());
    WlPlugin.info("wlmisc enabled");
  }

  @Override public void onDisable() {
    WlPlugin.info("wlmisc disabled");
  }
  
}
