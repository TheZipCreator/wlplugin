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

import java.util.*;

class Listener : ListenerAdapter() {
	override fun onReady(e: ReadyEvent) {
		DiscordModule.message("&awldiscord enabled");
	}

	fun sendMessage(sender: String, channel: String, message: String) {
		if(channel == "global") {
			for(p in Bukkit.getOnlinePlayers()) {
				val pd = Data.getPlayerData(p);
				if(!pd.hideGlobal && !pd.discordIgnored.contains(sender))
					p.sendMessage(Utils.escapeText(message));
			}
			WlPlugin.info("[CHAT] "+Utils.escapeTextAnsi(message));
			return;
		}
		for(p in Bukkit.getOnlinePlayers()) {
			val pd = Data.getPlayerData(p);
			if(pd.channel == channel)
				p.sendMessage(Utils.escapeText(message));
		}
	}

	override fun onMessageReceived(e: MessageReceivedEvent) {
		if(e.channel.name == DiscordModule.CHANNEL_NAME) {
			val u = e.author;
			if(u.isBot())
				return;
			var msg = e.getMessage();

			val str = buildString {
				append(msg.getContentDisplay());
				for(a in msg.attachments)
					append(" &_p[&_e${a.url}&_p]");
			}
			sendMessage(u.name, "global", "&9discord &_s| &_p@&_e${u.name}&f: $str");
		}
	}

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when(event.name) {
			"players" -> {
				object : BukkitRunnable() {
					override fun run() {
						val str = buildString {
							append("**List of all online players:**\n");
							for(p in Bukkit.getOnlinePlayers())
								append(p.name+"\n");
						};
						event.reply(str).queue();
					}
				}.runTask(WlPlugin.instance!!);
			}
			"panda" -> {
				val m = event.member!!;
				for(r in m.roles) {
					if(r.name == "Panda") {
						object : BukkitRunnable() {
							override fun run() {
								val u = m.user;
								for(p in Bukkit.getOnlinePlayers()) {
									p.world.spawnEntity(p.getLocation(), EntityType.PANDA);
									p.sendMessage(Utils.escapeText("&_p* &_dA wild panda appears! (Spawned by &_p@&_e${u.name}&_d.)"));
								}
								event.reply("Pandas have been spawned at every player!").queue();
							}
						}.runTask(WlPlugin.instance!!);
						return;
					}
				}
				event.reply("You don't have the panda role!").queue();
			}
		}
	}
}
