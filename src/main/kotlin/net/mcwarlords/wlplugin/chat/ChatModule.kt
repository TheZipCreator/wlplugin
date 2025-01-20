package net.mcwarlords.wlplugin.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;
import org.bukkit.command.*;
import java.util.*;

class ChatModule : SimpleModule {
	override val name = "wlchat";

	override fun registerFields() {
		// TODO
	}

	override fun enabled() {
		ChatCommand.register();
		WlPlugin.addListener(ChatListener);
		for(alias in listOf("global")) {
			WlPlugin.addCommand(alias, object : CommandExecutor {
				override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
					if(sender !is Player) {
						sender.sendMessage("wlchat can only be used by a player");
						return false;
					}
					sender.performCommand("wlchat $alias ${args.joinToString(" ")}");
					return true;
				}
			}, object: TabCompleter {
				override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String> {
					return listOf();
				}
			});
		}
		WlPlugin.info("wlchat enabled");
	}

	override fun disabled() {
		WlPlugin.info("wlchat disabled");
	}
	
}
