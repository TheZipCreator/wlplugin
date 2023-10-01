package net.mcwarlords.wlplugin;

import org.bukkit.plugin.java.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;


import net.mcwarlords.wlplugin.chat.ChatModule;
import net.mcwarlords.wlplugin.code.CodeModule;
import net.mcwarlords.wlplugin.discord.DiscordModule;
import net.mcwarlords.wlplugin.game.GameModule;
import net.mcwarlords.wlplugin.item.ItemModule;
import net.mcwarlords.wlplugin.misc.MiscModule;
import net.mcwarlords.wlplugin.plot.PlotModule;
import net.mcwarlords.wlplugin.schema.SchemaModule;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import org.bukkit.command.*;
import org.bukkit.event.*;

class WlPlugin : JavaPlugin() {
	companion object {
		@JvmField val VERSION = "1.7.4";
		@JvmField var instance: WlPlugin? = null;
		@JvmField var rand: Random? = null;
		val prefixCol = '4';
		val defaultCol = '7';
		val separatorCol = '8';
		val errorCol = 'c';
		@JvmField val PLOT_SIZE = 10000;
		/** Add a listener to the server. */
		@JvmStatic fun addListener(l: Listener) {
			instance!!.getServer().getPluginManager().registerEvents(l, instance!!);
		}

		/** Add a CommandExecutor to the server */
		@JvmStatic fun addCommand(cmd: String, exec: CommandExecutor) {
			instance!!.getCommand(cmd)?.setExecutor(exec);
		}

		/** Add a CommandExecutor to the server */
		@JvmStatic fun addCommand(cmd: String, exec: CommandExecutor, tc: TabCompleter) {
			instance!!.getCommand(cmd)?.setExecutor(exec);
			instance!!.getCommand(cmd)?.setTabCompleter(tc);
		}

		/** Log to info. Equivalent to {@code WlPlugin.instance.getLogger().info(...)} */
		@JvmStatic fun info(s: String) {
			instance!!.getLogger().info(s);
		}

		/** Log to warning. Equivalent to {@code WlPlugin.instance.getLogger().warning(...)} */
		@JvmStatic fun warn(s: String) {
			instance!!.getLogger().warning(s);
		}

		/** Log to info and escape. Equivalent to {@code WlPlugin.info(Utils.escapeText(...))} */
		@JvmStatic fun infoe(s: String) {
			instance!!.getLogger().info(Utils.escapeText(s));
		}

		/** Log to warning and escape. Equivalent to {@code WlPlugin.warning(Utils.escapeText(...))} */
		@JvmStatic fun warninge(s: String) {
			instance!!.getLogger().warning(Utils.escapeText(s));
		}
	}	

	var modules = mutableListOf<Module>(
		ChatModule(), PlotModule(), GameModule(), SchemaModule(), DiscordModule(), CodeModule(), ItemModule(), MiscModule()
	);
	
	object TestCommand : ModuleCommand {

		override val name = "wltest";
		override val clazz = TestCommand::class;

		@SubCommand(["t", "thank"], "Says thank you to a player.") fun greet(@CommandPlayer p: Player, target: String, vararg reason: String) {
			Utils.sendMessage("global", "${p.name} says: Thank you, $target${if(reason.size == 0) "" else " for ${reason.joinToString(" ")}"}!");
		}
	}

	override fun onEnable() {
		instance = this;
		info("WlPlugin "+VERSION+" enabled");
		// create random
		rand = Random();
		for(m in modules) {
			if(m is SimpleModule) {
				info("Registering fields for ${m.name}...");
				m.registerFields();
			}
		}
		Data.onEnable();
		for(m in modules)
			m.onEnable();
		TestCommand.register();
		// add autosave every 30 min
		object : BukkitRunnable() {
			override fun run() {
				Data.saveData();
			}
		}.runTaskTimer(this, 20, 36000);
	}

	override fun onDisable() {
		for(m in modules) {
			try {
				m.onDisable();
			} catch(e: NoClassDefFoundError) {
				// JDA throws this for some reason, so I'll just catch it here.
				// I know you're not supposed to catch errors but this stops data saving and I believe that that is more important
				WlPlugin.info("NoClassDefFoundError caught.");
			}
		}
		Data.onDisable();
		info("Deleting temporary files...");
		for(f in File("plugins/wlplugin/tmp").listFiles())
			f.delete();
		info("WlPlugin "+VERSION+" disabled");
	}

}
