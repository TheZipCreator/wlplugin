package net.mcwarlords.wlplugin.code;

import net.mcwarlords.wlplugin.*;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;

class CodeModule : Module {
	override fun onEnable() {
    WlPlugin.addCommand("wlcode", CodeCommand(), object : TabCompleter {
			override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): MutableList<String> {
					if(args.size == 1)
						return mutableListOf(
							"h", "help"
						);
					return mutableListOf();
				}	
		});
		WlPlugin.addListener(CodeListener());
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

private fun mkItem(m: Material, name: String): ItemStack {
	var i = ItemStack(m);
	var meta = i.itemMeta!!;
	meta.setDisplayName(Utils.escapeText(name));
	i.itemMeta = meta;
	return i;
}

enum class CodeItem(val item: ItemStack) {
	LBRACE(mkItem(Material.PISTON, "&6{")),
	RBRACE(mkItem(Material.PISTON, "&6}")),
	EVENT(mkItem(Material.DIAMOND_BLOCK, "&bEvent")),
	BUILTIN(mkItem(Material.FURNACE, "&cBuiltin")),
	IF(mkItem(Material.OAK_PLANKS, "&6If"))
}

internal fun toggleCodeMode(p: Player) {
	var pd = Data.getPlayerData(p);
	pd.codeMode = !pd.codeMode;
	if(pd.codeMode) {
		pd.prevInv = p.inventory.contents;
		p.inventory.clear();
		for(item in CodeItem.values()) {
			p.inventory.addItem(item.item);
		}
		p.sendMessage(Utils.escapeText("&_p* &_dEntered code mode."));
	} else {
		p.inventory.contents = pd.prevInv;
		pd.prevInv = null; // saves a bit of memory
		p.sendMessage(Utils.escapeText("&_p* &_dExited code mode."));
	}
}
