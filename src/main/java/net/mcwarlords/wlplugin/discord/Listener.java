package net.mcwarlords.wlplugin.discord;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.mcwarlords.wlplugin.*;

public class Listener extends ListenerAdapter {
  @Override public void onMessageReceived(MessageReceivedEvent e) {
    if(e.getChannel().getName().equals(DiscordModule.CHANNEL_NAME)) {
      User u = e.getAuthor();
      if(u.isBot())
        return;
      String msg = Utils.escapeText("&9discord &_s| &_e"+u.getName()+"&_p#"+u.getDiscriminator()+"&f: "+e.getMessage().getContentRaw());
      WlPlugin.info("[CHAT] "+msg);
      for(Player p : Bukkit.getOnlinePlayers())
        p.sendMessage(msg);
    }
  }
}
