package net.mcwarlords.wlplugin.game;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;
import org.bukkit.command.*;
import java.util.*;

public class GameModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlgame enabled");
    WlPlugin.addCommand("wlgame", new GameCommand(), new TabCompleter() {
			public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
				if(args.length == 1)
					return List.of(
						"h", "help",
						"n", "new",
						"w", "warp",
						"i", "info",
						"l", "list",
						"s", "set"
					);
				if(args.length == 2) {
					if(Utils.isAny(args[0], "warp", "w", "info", "i"))
						return Utils.getSortedGames();
				}
				if(args.length == 3) {
					if(Utils.isAny(args[0], "s", "set"))
						return List.of("name", "description", "location");
				}
				return List.of();
			}
		});
  }

  @Override public void onDisable() {
    WlPlugin.info("wlgame disabled");
  }
  
}
