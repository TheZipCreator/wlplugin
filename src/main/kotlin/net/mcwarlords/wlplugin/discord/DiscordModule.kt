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
import net.mcwarlords.wlplugin.Module;

object DiscordModule : SimpleModule {
	override val name = "wldiscord";

	lateinit var key: String
	var jda: JDA? = null;
	val CHANNEL_NAME = "chat-mirror";

	override fun enabled() {
		WlPlugin.info("wldiscord enabled");
		// load key
		try {
			key = Files.readAllLines(Paths.get("plugins/wlplugin/discordkey.txt"), Charset.defaultCharset()).get(0);
		} catch(e: IOException) {
			WlPlugin.info("Key not found. Aborting...");
			return;
		}
		val j = JDABuilder.createDefault(key)
			.enableIntents(GatewayIntent.MESSAGE_CONTENT)
			.addEventListeners(Listener())
			.setActivity(Activity.playing(": Unknown/100"))
			.build();
		jda = j;
		WlPlugin.addListener(object : org.bukkit.event.Listener {
			@EventHandler
			fun onBroadcast(e: BroadcastMessageEvent) {
				message(e.getMessage());
			}
		});
		object : BukkitRunnable() {
			override fun run() {
				val numPlayers = Bukkit.getOnlinePlayers().size;
				j.presence.setActivity(Activity.playing(": $numPlayers/100"));
			}
		}.runTaskTimer(WlPlugin.instance!!, 0, 20);
		j.updateCommands()
			.addCommands(
				Commands.slash("players", "Displays all online players."),
				Commands.slash("panda", "Puts a panda at every player. (You must have the Panda role to do this.)")
			)
		.queue();
	}

	override fun disabled() {
		WlPlugin.info("wldiscord disabled");
		message("&cwldiscord disabled");
		jda?.shutdown();
	}

	override fun registerFields() {

	}

	fun message(message: String, doEscape: Boolean = true) {
		val j = jda ?: return
		var msg = message;
		val guilds = j.getGuilds();
		if(doEscape)
			msg = "```ansi\n"+Utils.escapeTextAnsi(msg)+"\n```";
		for(g in guilds) {
			for(gc in g.channels) {
				if(gc.name == CHANNEL_NAME) {
					val tc = gc as TextChannel;
					tc.sendMessage(msg).queue(null, { err ->
						WlPlugin.warn("Error sending message: ${err.message}");
					});
					break;
				}
			}
		}
	}
}
