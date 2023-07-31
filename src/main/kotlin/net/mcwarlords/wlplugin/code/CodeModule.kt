package net.mcwarlords.wlplugin.code;

import net.mcwarlords.wlplugin.*;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;

class CodeModule : Module {
	override fun onEnable() {
    WlPlugin.addCommand("wlcode", CodeCommand, object : TabCompleter {
			override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): MutableList<String> {
					if(args.size == 1)
						return mutableListOf(
							"h", "help",
							"n", "new",
							"w", "warp",
							"b", "build",
							"m", "mode"
						);
					return mutableListOf();
				}	
		});
		WlPlugin.addListener(CodeListener);
		WlPlugin.addListener(ExecutorListener);
		WlPlugin.info("wlcode: building units...");
		for(name in Data.codeUnits.keys) {
			try {
				Data.codeUnits[name]!!.build();
			} catch(e: CodeException) {
				WlPlugin.info("Failed to build unit $name");
			}
		}
		WlPlugin.info("wlcode enabled");
	}

	override fun onDisable() {
		for(p in WlPlugin.instance.server.onlinePlayers) {
			var pd = Data.getPlayerData(p);
			if(pd.codeMode)
				toggleCodeMode(p);
		}
		WlPlugin.info("wlcode disabled");
	}
}

private fun mkItem(m: Material, name: String, vararg lore: String): ItemStack {
	var i = ItemStack(m);
	var meta = i.itemMeta!!;
	meta.setDisplayName(Utils.escapeText(name));
	meta.lore = lore.toList().map { Utils.escapeText(it) };
	i.itemMeta = meta;
	return i;
}

enum class CodeItem(val item: ItemStack) {
	LBRACK(mkItem(Material.PISTON, "&6[", "&7Opens a block.")),
	RBRACK(mkItem(Material.PISTON, "&6]", "&7Closes a block.")),
	EVENT(mkItem(Material.DIAMOND_BLOCK, "&bEvent", "&7An event triggered by a player.")),
	BUILTIN(mkItem(Material.FURNACE, "&cBuiltin", "&7A built-in function.")),
	IF(mkItem(Material.OAK_PLANKS, "&6If", "&7Checks whether a condition is true.")),
	STRING(mkItem(Material.WHITE_WOOL, "&5String", "&7Stores text.")),
	NUMBER(mkItem(Material.TARGET, "&5Number", "&7A numerical value.")),
	TRUE(mkItem(Material.LIME_TERRACOTTA, "&5True", "&7The boolean value 'true'.")),
	FALSE(mkItem(Material.RED_TERRACOTTA, "&5False", "&7The boolean value 'false'.")),
	VARIABLE(mkItem(Material.OBSIDIAN, "&5Variable", "&7A value that can change.")),
	COMMENT(mkItem(Material.REDSTONE_LAMP, "&8Comment", "&7Text that is ignored. Useful for", "&7recording what something does."))
}

internal val blocksItem = mkItem(Material.DIAMOND, "&bCode Blocks");

internal fun toggleCodeMode(p: Player) {
	var pd = Data.getPlayerData(p);
	pd.codeMode = !pd.codeMode;
	if(pd.codeMode) {
		pd.prevInv = p.inventory.contents;
		p.inventory.clear();
		p.inventory.addItem(blocksItem);
		p.sendMessage(Utils.escapeText("&_p* &_dEntered code mode."));
	} else {
		p.inventory.contents = pd.prevInv;
		pd.prevInv = null; // saves a bit of memory
		p.sendMessage(Utils.escapeText("&_p* &_dExited code mode."));
	}
}

// converts CONSTANT_CASE to lisp-case
internal fun <T : Enum<T>> Enum<T>.lispCase(): String {
	return this.name.lowercase().replace('_', '-');
}
