package net.mcwarlords.wlplugin.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import net.mcwarlords.wlplugin.*;

public class ChatListener implements Listener {
  @EventHandler void onPlayerJoin(PlayerJoinEvent e) {
    e.setJoinMessage(Utils.escapeText("&a&l&o+ &7"+e.getPlayer().getName()));
  }

  @EventHandler void onPlayerQuit(PlayerQuitEvent e) {
    e.setQuitMessage(Utils.escapeText("&c&l&o- &7"+e.getPlayer().getName()));
  }

  @EventHandler void onPlayerChat(AsyncPlayerChatEvent e) {
    // TODO: maybe change this to modifying e.getRecipients()
    e.setCancelled(true);
    PlayerData pd = Data.getPlayerData(e.getPlayer());
    String msg = Utils.escapeText("&a"+
      pd.channel+(Data.lockedChannels.containsKey(pd.channel) ? " &4X" : "")
      +" &8| &7"
      +(pd.nick == null ? e.getPlayer().getName() : pd.nick)
      +"&f: "+pd.prefix+e.getMessage());
    WlPlugin.info("[CHAT] "+msg);
    if(pd.channel == "global") {
      for(Player p : Bukkit.getOnlinePlayers()) {
        p.sendMessage(msg);
      }
    } else {
      for(Player p : Bukkit.getOnlinePlayers()) {
        if(Data.getPlayerData(p).channel.equals(pd.channel))
          p.sendMessage(msg);
      }
    }
  }
}
