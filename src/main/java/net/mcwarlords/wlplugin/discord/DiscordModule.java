package net.mcwarlords.wlplugin.discord;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.bukkit.event.*;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;

import java.nio.charset.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.*;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.mcwarlords.wlplugin.*;

public class DiscordModule implements Module {
  public static String key;
  public static JDA jda;
  public static final String CHANNEL_NAME = "chat-mirror";

  @Override public void onEnable() {
    WlPlugin.info("wldiscord enabled");
    // load key
    try {
      key = Files.readAllLines(Paths.get("plugins/wlplugin/discordkey.txt"), Charset.defaultCharset()).get(0);
    } catch(IOException e) {
      WlPlugin.info("Key not found. Aborting...");
      return;
    }
    jda = JDABuilder.createDefault(key)
      .enableIntents(GatewayIntent.MESSAGE_CONTENT)
      .addEventListeners(new Listener())
      .setActivity(Activity.playing("unknown player count"))
      .build();
    WlPlugin.addListener(new org.bukkit.event.Listener() {
      @EventHandler
      public void onBroadcast(BroadcastMessageEvent e) {
        message(e.getMessage());
      }
    });
    new BukkitRunnable() {
      @Override public void run() {
        int numPlayers = Bukkit.getOnlinePlayers().size();
        if(numPlayers != 1)
          jda.getPresence().setActivity(Activity.playing(numPlayers+" players online."));
        else
          jda.getPresence().setActivity(Activity.playing(numPlayers+" player online."));
      }
    }.runTaskTimer(WlPlugin.instance, 0, 20);
    jda.updateCommands()
      .addCommands(
        Commands.slash("players", "Displays all online players")
      )
    .queue();
  }

  @Override public void onDisable() {
    WlPlugin.info("wldiscord disabled");
    message("&cwldiscord disabled");
    jda.shutdown();
  }

  public static void message(String msg) {
    message(msg, true);
  }

  public static void message(String msg, boolean doEscape) {
    if(jda == null)
      return;
    List<Guild> guilds = jda.getGuilds();
    if(doEscape)
      msg = "```ansi\n"+Utils.escapeTextAnsi(msg)+"\n```";
    for(Guild g : guilds) {
      for(GuildChannel gc : g.getChannels()) {
        if(gc.getName().equals(CHANNEL_NAME)) {
          TextChannel tc = (TextChannel)gc;
          tc.sendMessage(msg).queue(null, err -> {
            WlPlugin.warn("Error sending message: "+err.getMessage());
          });
          break;
        }
      }
    }
  }
}
