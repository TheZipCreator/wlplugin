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
        PlayerData pd2 = Data.getPlayerData(p);
        if(!pd2.hideGlobal && !pd2.ignored.contains(Utils.getUUID(e.getPlayer())))
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
