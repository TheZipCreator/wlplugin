package net.mcwarlords.wlplugin.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
							"g", "global",
							"hg", "hideglobal",
							"sg", "showglobal",
							"i", "ignore",
							"il", "ignorelist",
							"di", "discordignore",
							"dil", "discordignorelist"
						);
				}
				if(args.length >= 2 && (args[0].equals("g") || args[0].equals("global")))
					return List.of();
				return null;
			}
		});
    for(String alias : new String[]{"global"}) {
      WlPlugin.addCommand(alias, new CommandExecutor() {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
          if(!(sender instanceof Player)) {
            sender.sendMessage("wlchat can only be used by a player");
            return false;
          }
          StringBuilder cmd = new StringBuilder("wlchat "+alias+" ");
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
					return List.of();
				}
			});
    }
	}

	@Override public void onDisable() {
		WlPlugin.info("wlchat disabled");
	}
	
}
