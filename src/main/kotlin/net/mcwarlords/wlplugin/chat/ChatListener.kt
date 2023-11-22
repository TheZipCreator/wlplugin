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
		pd.hideGlobal = false;
	}

	@EventHandler fun onPlayerQuit(e: PlayerQuitEvent) {
		val msg = "&c&l&o- &e"+e.player.name;
		e.setQuitMessage(Utils.escapeText(msg));
		DiscordModule.message(msg);
	}
	
	fun sendChat(p: Player, channel: String, msg: String, pd: PlayerData = p.data) {
		val preFormat = "&a$channel${if(Data.lockedChannels.containsKey(channel)) " &4X" else ""} &8| &7${if(pd.nick == null) p.name else pd.nick}&f: ${pd.prefix}$msg";
		Utils.sendMessage(channel, preFormat, p);
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
