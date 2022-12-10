package net.mcwarlords.wlplugin.discord;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.*;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.*;
import net.dv8tion.jda.api.requests.ErrorResponse;
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
      .build();
    message("&awldiscord enabled");
  }

  @Override public void onDisable() {
    WlPlugin.info("wldiscord disabled");
    message("&cwldiscord disabled");
    jda.shutdown();
  }

  public static void message(String msg) {
    List<Guild> guilds = jda.getGuilds();
    for(Guild g : guilds) {
      for(GuildChannel gc : g.getChannels()) {
        if(gc.getName().equals(CHANNEL_NAME)) {
          TextChannel tc = (TextChannel)gc;
          tc.sendMessage("```ansi\n"+Utils.escapeTextAnsi(msg)+"\n```").queue(null, err -> {
            WlPlugin.warn("Error sending message: "+err.getMessage());
          });
          break;
        }
      }
    }
  }
}
