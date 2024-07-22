package net.mcwarlords.wlplugin.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.discord.DiscordModule;

object ChatListener : Listener {
	@EventHandler fun onPlayerJoin(e: PlayerJoinEvent) {
		val p = e.player;
		val pd = p.data;
		val msg = "&a&l&o+ &e"+p.getName();
		e.setJoinMessage(Utils.escapeText(msg));
		DiscordModule.message(msg);
		if(pd.nick != null)
			p.setDisplayName(pd.nick);
		if(pd.autoGlobal) {
			pd.channel = "global";
			pd.hideGlobal = false;
		}
	}

	@EventHandler fun onPlayerQuit(e: PlayerQuitEvent) {
		val msg = "&c&l&o- &e"+e.player.name;
		e.setQuitMessage(Utils.escapeText(msg));
		DiscordModule.message(msg);
	}
	
	fun sendChat(p: Player, channel: String, msg: String, pd: PlayerData = p.data) {
		val preFormat = buildString {
			append("&${if(channel == "global") "a" else "#"+Utils.hexColorOf(channel) }$channel");
			if(Data.lockedChannels.containsKey(channel))
				append(" &4X");
			append(" &8| &7${if(pd.nick == null) p.name else pd.nick}");
			append("&f: ${pd.prefix}$msg");
		};
		Utils.sendMessage(channel, preFormat, p, msg);
	}


	@EventHandler fun onPlayerChat(e: AsyncPlayerChatEvent) {
		if(e.isCancelled())
			return;
		// TODO: maybe change this to modifying e.getRecipients()
		e.setCancelled(true);
		val pd = e.player.data;
		sendChat(e.player, pd.channel, e.message, pd);
	}
}
