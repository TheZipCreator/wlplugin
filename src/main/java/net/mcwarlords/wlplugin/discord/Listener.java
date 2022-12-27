package net.mcwarlords.wlplugin.discord;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.mcwarlords.wlplugin.*;

public class Listener extends ListenerAdapter {
  @Override public void onReady(ReadyEvent e) {
    DiscordModule.message("&awldiscord enabled");
  }

  @Override public void onMessageReceived(MessageReceivedEvent e) {
    if(e.getChannel().getName().equals(DiscordModule.CHANNEL_NAME)) {
      User u = e.getAuthor();
      if(u.isBot())
        return;
      String msg = Utils.escapeText("&9discord &_s| &_e"+u.getName()+"&_p#"+u.getDiscriminator()+"&f: "+e.getMessage().getContentRaw());
      WlPlugin.info("[CHAT] "+msg);
      for(Player p : Bukkit.getOnlinePlayers()) {
        PlayerData pd = Data.getPlayerData(p);
        if(!pd.hideGlobal)
          p.sendMessage(msg);
      }
    }
  }

  @Override public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    switch(event.getName()) {
      case "players":
        new BukkitRunnable() {
          @Override public void run() {
            String res = "**List of all online players:**\n";
            for(Player p : Bukkit.getOnlinePlayers())
              res += p.getName()+"\n";
            event.reply(res).queue();
          }
        }.runTask(WlPlugin.instance);
        break;
      case "panda": {
        Member m = event.getMember();
        for(Role r : m.getRoles()) {
          if(r.getName().equals("Panda")) {
            new BukkitRunnable() {
              @Override public void run() {
                for(Player p : Bukkit.getOnlinePlayers()) {
                  p.getWorld().spawnEntity(p.getLocation(), EntityType.PANDA);
                  User u = m.getUser();
                  p.sendMessage(Utils.escapeText("&_p* &_dA wild panda appears! (Spawned by &_e"+u.getName()+"&_p#"+u.getDiscriminator()+"&_d.)"));
                }
                event.reply("Pandas have been spawned at every player!").queue();
              }
            }.runTask(WlPlugin.instance);
            return;
          }
        }
        event.reply("You don't have the panda role!").queue();
      }
    }
  }
}
