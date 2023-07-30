package net.mcwarlords.wlplugin;

import org.bukkit.plugin.java.*;
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

public class WlPlugin extends JavaPlugin {
	public static final String VERSION = "1.6.1";
	public static WlPlugin instance;
	public static Random rand;
	public static final char prefixCol		= '4';
	public static final char defaultCol	 = '7';
	public static final char errorCol		 = 'c';
	public static final char seperatorCol = '8';
	public static final int PLOT_SIZE		 = 10000;
	

	ArrayList<Module> modules;

	@Override public void onEnable() {
		instance = this;
		info("WlPlugin "+VERSION+" enabled");
		// create random
		rand = new Random();
		Data.onEnable();
		modules = new ArrayList<Module>();
		modules.add(new ChatModule());
		modules.add(new PlotModule());
		modules.add(new GameModule());
		modules.add(new SchemaModule());
		modules.add(new DiscordModule());
		// modules.add(new CodeModule());
		modules.add(new ItemModule());
		modules.add(new MiscModule());
		for(Module m : modules)
			m.onEnable();
		// add autosave every 30 min
		new BukkitRunnable() {
			public void run() {
				Data.saveData();
			}
		}.runTaskTimer(this, 0, 36000);
	}

	@Override public void onDisable() {
		for(Module m : modules)
			m.onDisable();
		Data.onDisable();
		info("Deleting temporary files...");
		for(File f : new File("plugins/wlplugin/tmp").listFiles())
			f.delete();
		info("WlPlugin "+VERSION+" disabled");
	}

	/** Add a listener to the server. */
	public static void addListener(Listener l) {
		instance.getServer().getPluginManager().registerEvents(l, instance);
	}

	/** Add a CommandExecutor to the server */
	public static void addCommand(String cmd, CommandExecutor exec) {
		instance.getCommand(cmd).setExecutor(exec);
	}

	/** Add a CommandExecutor to the server */
	public static void addCommand(String cmd, CommandExecutor exec, TabCompleter tc) {
		instance.getCommand(cmd).setExecutor(exec);
		instance.getCommand(cmd).setTabCompleter(tc);
	}

	/** Log to info. Equivalent to {@code WlPlugin.instance.getLogger().info(...)} */
	public static void info(String s) {
		instance.getLogger().info(s);
	}

	/** Log to warning. Equivalent to {@code WlPlugin.instance.getLogger().warning(...)} */
	public static void warn(String s) {
		instance.getLogger().warning(s);
	}

	/** Log to info and escape. Equivalent to {@code WlPlugin.info(Utils.escapeText(...))} */
	public static void infoe(String s) {
		instance.getLogger().info(Utils.escapeText(s));
	}

	/** Log to warning and escape. Equivalent to {@code WlPlugin.warning(Utils.escapeText(...))} */
	public static void warninge(String s) {
		instance.getLogger().warning(Utils.escapeText(s));
	}
}
