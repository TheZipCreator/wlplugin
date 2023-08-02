package net.mcwarlords.wlplugin.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.discord.DiscordModule;

public class ChatListener implements Listener {
	@EventHandler void onPlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		PlayerData pd = Data.getPlayerData(p);
		String msg = "&a&l&o+ &e"+p.getName();
		e.setJoinMessage(Utils.escapeText(msg));
		DiscordModule.message(msg);
		if(pd.nick != null)
			p.setDisplayName(pd.nick);
	}

	@EventHandler void onPlayerQuit(PlayerQuitEvent e) {
		String msg = "&c&l&o- &e"+e.getPlayer().getName();
		e.setQuitMessage(Utils.escapeText(msg));
		DiscordModule.message(msg);
	}
	
	public static void sendChat(Player p, String channel, String msg) {
		sendChat(p, channel, msg, Data.getPlayerData(p));
	}

	public static void sendChat(Player p, String channel, String msg, PlayerData pd) {
		String preFormat = "&a"+
			channel+(Data.lockedChannels.containsKey(channel) ? " &4X" : "")
			+" &8| &7"
			+(pd.nick == null ? p.getName() : pd.nick)
			+"&f: "+pd.prefix+msg;
		Utils.sendMessage(p, channel, preFormat);
	}

	@EventHandler void onPlayerChat(AsyncPlayerChatEvent e) {
		if(e.isCancelled())
			return;
		// TODO: maybe change this to modifying e.getRecipients()
		e.setCancelled(true);
		PlayerData pd = Data.getPlayerData(e.getPlayer());
		sendChat(e.getPlayer(), pd.channel, e.getMessage(), pd);
	}
}
