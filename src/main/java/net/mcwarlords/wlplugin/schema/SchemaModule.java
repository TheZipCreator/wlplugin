package net.mcwarlords.wlplugin.schema;

import java.nio.file.*;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;

import org.bukkit.command.*;
import java.util.*;

public class SchemaModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlschema enabled");
    if(!Files.exists(Paths.get("plugins/FastAsyncWorldEdit"))) {
      WlPlugin.warn("FAWE is not installed. In order for wlschema to work, please install FAWE.");
      return;
    }
    WlPlugin.addCommand("wlschema", new SchemaCommand(), new TabCompleter() {
			public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
				if(args.length == 1)
					return List.of(
						"s", "schematic",
						"l", "litematic"
					);
				return List.of();
			}	
		});
  }

  @Override public void onDisable() {
    WlPlugin.info("wlschema disabled");
  }
}
