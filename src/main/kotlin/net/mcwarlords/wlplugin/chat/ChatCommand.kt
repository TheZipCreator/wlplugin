package net.mcwarlords.wlplugin.chat;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import net.mcwarlords.wlplugin.*;

object ChatCommand : ModuleCommand {
	override val name = "wlchat";
	override val clazz = ChatCommand::class;
	

	fun changeChannel(p: Player, newChannel: String) {
		val pd = Data.getPlayerData(p);
		if(Data.lockedChannels.containsKey(pd.channel)) {
			if(Utils.channelPlayerCount(pd.channel) == 1)
				Data.lockedChannels.remove(pd.channel);
		}
		pd.channel = newChannel;
	}

	@SubCommand(["j", "join"], "Joins channel &_e<channel>&_d. &_e[pass] &_dis necessary if the channel is locked.") fun join(@CommandPlayer p: Player, channel: String, pass: String = "") {
		if(Data.lockedChannels.containsKey(channel)) {
			if(pass == "") {
				p.sendEscaped("&_p*&_e Channel &_p$channel&_e is locked. Please enter a password like so: &_p/wlchat join $channel <password>");
				return;
			}
			if(pass != Data.lockedChannels[channel]!!) {
				p.sendEscaped("&_p* &_eIncorrect password.");
				return;
			}
		}
		p.sendEscaped("&_p* &_dJoined channel &_e$channel");
		changeChannel(p, channel);
	}

	@SubCommand(["x", "exit"], "Exits your current channel and joins global.") fun exit(@CommandPlayer p: Player) {
		p.sendEscaped("&_p* &_dLeft channel &_e${p.data.channel}.");
		changeChannel(p, "global");
	}

	@SubCommand(["p", "prefix"], "Sets a prefix to appear before every message you send in chat (typically used for color codes). If no prefix is specified, it clears your current prefix.") fun prefix(@CommandPlayer p: Player, prefix: String = "") {
		if(prefix == "") {
			p.data.prefix = "";
			p.sendEscaped("&_p* &_dPrefix Cleared");
			return;
		}
		p.data.prefix = prefix;
		p.sendEscaped("&_p*&_dPrefix set to $prefix.");
	}

	@SubCommand(["n", "nick"], "Sets your nickname to &_e[nick]&_d if specified, otherwise clears it.") fun nick(@CommandPlayer p: Player, nick: String = "") {
		if(nick != "") {
			p.sendEscaped("&_p* &_dChanged nickname to &_e$nick&_d.");
			p.setDisplayName(Utils.escapeText(nick));
			p.data.nick = nick;
			return;
		}
		p.sendEscaped("&_p* &_dCleared nickname.");
		p.data.nick = null;
	}

	@SubCommand(["r", "realname"], "Tells you which online user has a given nick") fun realname(@CommandPlayer p: Player, nick: String) {
		val players = Bukkit.getOnlinePlayers().filter { p.data.nick != null && Utils.stripColorCodes(p.data.nick!!) == nick };
		if(players.size == 0) {
			p.sendEscaped("&_p* &_eCould not find any players with nickname '$nick'&_e.");
			return;
		}
		p.sendEscaped("&_p* &_d${players.map { it.getName() }.joinToString(", ")} is ${if(players.size == 1) players[0].data.nick else nick}&_d.");
	}

	@SubCommand(["l", "lock"], "Locks a channel with a given password. You must be the only one in the channel to do this.") fun lock(@CommandPlayer p: Player, password: String) {
		if(Utils.channelPlayerCount(p.data.channel) > 1) {
			p.sendEscaped("&_p* &_eCannot lock a channel while other players are in it.");
			return;
		}
		if(p.data.channel == "global") {
			p.sendEscaped("&_p* &_eCannot lock channel '${p.data.channel}'");
			return;
		}
		Data.lockedChannels.put(p.data.channel, password);
		p.sendEscaped("&_p* &_dSuccessfully locked channel.");
	}

	@SubCommand(["g", "global"], "Sends a message in global chat.") fun global(@CommandPlayer p: Player, vararg msg: String) {
		ChatListener.sendChat(p, "global", msg.joinToString(" "));
	}

	@SubCommand(["tg", "toggleglobal"], "Toggles global chat.") fun hideGlobal(@CommandPlayer p: Player) {
		p.data.hideGlobal = !p.data.hideGlobal;
		if(p.data.hideGlobal)
			p.sendEscaped("&_p* &_dGlobal chat is now hidden.");
		else
			p.sendEscaped("&_p* &_dGlobal chat is now shown.");
	}

	@SubCommand(["tp", "toggleping"], "Toggles ping. If enabled, when players mention you (username or nickname) you will get a ping noise.")
	fun togglePing(@CommandPlayer p: Player) {
		p.data.ping = !p.data.ping;
		if(p.data.ping)
			p.sendEscaped("&_p* &_dPing has been enabled.");
		else
			p.sendEscaped("&_p* &_dPing has been disabled.");
	}

	@SubCommand(["tag", "autoglobal"], "Toggles autoglobal. If enabled, you will automatically be put into global chat when you join the server.")
	fun autoGlobal(@CommandPlayer p: Player) {
		p.data.autoGlobal = !p.data.autoGlobal;
		if(p.data.autoGlobal)
			p.sendEscaped("&_p* &_dAutoglobal has been enabled.");
		else
			p.sendEscaped("&_p* &_dAutoglobal has been disabled.");
	}

	@SubCommand(["i", "ignore"], "Ignores a player. If they're already ignored, it unignores them") fun ignore(@CommandPlayer p: Player, name: String) {
		if(!Data.playerExists(name)) {
			p.sendEscaped("&_p* &_eUnknown player $name.");
			return;
		}
		val uuid = Data.uuidOf(name);
		if(uuid == null) {
			p.sendEscaped("&_p* &_ePlayer '$name' does not exist.");
			return;
		}
		if(p.data.ignored.contains(uuid)) {
			p.data.ignored.remove(uuid);
			p.sendEscaped("&_p* &_dUnignored player $name.");
			return;
		}
		p.data.ignored.add(uuid);
		p.sendEscaped("&_p* &_dIgnored player $name.");
	}

	@SubCommand(["il", "ignorelist"], "Displays all ignored players.") fun ignoreList(@CommandPlayer p: Player) {
		p.sendEscaped("&_p* &_dList of all ignored players: &_d${p.data.ignored.map { Data.nameOf(it) }.joinToString(", ")}");
	}

	@SubCommand(["di", "discordignore"], "Ignores a user on discord. If they're already ignored, it unignores them.") fun discordIgnore(@CommandPlayer p: Player, name: String) {
		if(p.data.discordIgnored.contains(name)) {
			p.data.discordIgnored.remove(name);
			p.sendEscaped("&_p* &_dUnignored user $name.");
			return;
		}
		p.data.discordIgnored.add(name);
		p.sendMessage(Utils.escapeText("&_p* &_dIgnored user $name."));
	}

	@SubCommand(["dil", "discordignorelist"], "Displays all ignored discord users.") fun discordIgnoreList(@CommandPlayer p: Player) {
		p.sendEscaped("&_p* &_dList of all ignored discord users: &_d${p.data.discordIgnored.joinToString(", ")}");
	}
}
