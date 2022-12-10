package net.mcwarlords.wlplugin.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.discord.DiscordModule;

public class ChatListener implements Listener {
  @EventHandler void onPlayerJoin(PlayerJoinEvent e) {
    String msg = "&a&l&o+ &7"+e.getPlayer().getName();
    e.setJoinMessage(Utils.escapeText(msg));
    DiscordModule.message(msg);
  }

  @EventHandler void onPlayerQuit(PlayerQuitEvent e) {
    String msg = "&c&l&o- &7"+e.getPlayer().getName();
    e.setQuitMessage(Utils.escapeText(msg));
    DiscordModule.message(msg);
  }

  @EventHandler void onPlayerChat(AsyncPlayerChatEvent e) {
    // TODO: maybe change this to modifying e.getRecipients()
    e.setCancelled(true);
    PlayerData pd = Data.getPlayerData(e.getPlayer());
    String preFormat = "&a"+
      pd.channel+(Data.lockedChannels.containsKey(pd.channel) ? " &4X" : "")
      +" &8| &7"
      +(pd.nick == null ? e.getPlayer().getName() : pd.nick)
      +"&f: "+pd.prefix+e.getMessage();
    String msg = Utils.escapeText(preFormat);
    WlPlugin.info("[CHAT] "+msg);
    if(pd.channel == "global") {
      for(Player p : Bukkit.getOnlinePlayers()) {
        p.sendMessage(msg);
      }
      DiscordModule.message(preFormat);
    } else {
      for(Player p : Bukkit.getOnlinePlayers()) {
        if(Data.getPlayerData(p).channel.equals(pd.channel))
          p.sendMessage(msg);
      }
    }
  }
}
