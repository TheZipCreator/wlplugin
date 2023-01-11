package net.mcwarlords.wlplugin.chat;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;
import org.bukkit.command.*;
import java.util.*;

public class ChatModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlchat enabled");
    WlPlugin.addListener(new ChatListener());
    WlPlugin.addCommand("wlchat", new ChatCommand(), new TabCompleter() {
			public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
				if(args.length == 1) {
					return List.of(
							"h", "help",
							"j", "join",
							"x", "exit",
						 	"p", "prefix",
							"n", "nick",
							"r", "realname",
							"l", "lock",
							"hg", "hideglobal",
							"sg", "showglobal",
							"i", "ignore",
							"il", "ignorelist",
							"di", "discordignore",
							"dil", "discordignorelist"
						);
				}
				return null;
			}
		});
  }

  @Override public void onDisable() {
    WlPlugin.info("wlchat disabled");
  }
  
}
