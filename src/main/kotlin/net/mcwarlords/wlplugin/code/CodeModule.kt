package net.mcwarlords.wlplugin.code;

import net.mcwarlords.wlplugin.*;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;

import java.time.*;
import java.time.format.*;

import org.json.simple.*;

class CodeModule : SimpleModule {
	override val name = "wlcode";
	
	object Field : ServerDataField {
		val units: MutableMap<String, CodeUnit> = mutableMapOf(); /** Code units */
		override val name = "codeUnits";

		override fun load(obj: JSONObject) {
			if(!obj.containsKey("codeUnits"))
				return;
			val jsonCodeUnits = obj["codeUnits"]!! as JSONObject;
			jsonCodeUnits.forEach { k, v ->
				val name = k as String;
				units.put(name, CodeUnit.fromJSON(name, v as JSONObject));
			}
		}

		override fun save(obj: JSONObject) {
			val jsonCodeUnits = JSONObject();
			for(s in units.keys)
				jsonCodeUnits.put(s, units[s]!!.toJSON());
			obj.put("codeUnits", jsonCodeUnits);
		}
	}

	override fun registerFields() {
		Data.register(Field);
	}

	override fun enabled() {
		CodeCommand.register();
		WlPlugin.addListener(CodeListener);
		WlPlugin.addListener(ExecutorListener);
		WlPlugin.info("$name: building units...");
		for(name in Field.units.keys) {
			try {
				Field.units[name]!!.build();
			} catch(e: CodeException) {
				WlPlugin.warn(Utils.escapeTextAnsi("$name: Failed to build unit $name: ${e.toChatString()}"));
			}
		}
		Bukkit.getScheduler().runTaskTimer(WlPlugin.instance!!, Runnable {
			runTask.update();
			for(u in Field.units.values)
				u.update();
			CLoopEvent().execute();
		}, 20, 1);
	}

	override fun disabled() {
		for(p in WlPlugin.instance!!.server.onlinePlayers) {
			var pd = Data.getPlayerData(p);
			if(pd.codeMode)
				toggleCodeMode(p);
		}
		for(u in Field.units.values)
			u.halt();
	}
}

internal val codeUnits 
	get() = CodeModule.Field.units;

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
	FUNCTION(mkItem(Material.LAPIS_BLOCK, "&bFunction", "&7A reusable bit of code that", "&7can be called from anywhere.")),
	IMPORT(mkItem(Material.PURPUR_BLOCK, "&bImport", "&7Imports another code unit.")),
	BUILTIN(mkItem(Material.FURNACE, "&cBuiltin", "&7A built-in function.")),
	CALL(mkItem(Material.LAPIS_ORE, "&cCall", "&7Calls a function.")),
	IF(mkItem(Material.OAK_PLANKS, "&6If", "&7Checks whether a condition is true.")),
	DO(mkItem(Material.MANGROVE_PLANKS, "&6Do", "&7Does multiple commands at once.")),
	FOR(mkItem(Material.MAGMA_BLOCK, "&6For", "&7Iterates over a collection.")),
	WHILE(mkItem(Material.NETHER_WART_BLOCK, "&6While", "&7Iterates while a condition is true.")),
	RETURN(mkItem(Material.COAL_BLOCK, "&6Return", "&7Returns a value. If you do", "&7not wish to return anything,", "&7return Unit.")),
	BREAK(mkItem(Material.CHISELED_STONE_BRICKS, "&6Break", "&7Exits a loop prematurely.")),
	CONTINUE(mkItem(Material.CHISELED_QUARTZ_BLOCK, "&6Continue", "&7Skips to the next iteration of a loop.")),
	STRING(mkItem(Material.WHITE_WOOL, "&5String", "&7Stores text.")),
	NUMBER(mkItem(Material.WAXED_COPPER_BLOCK, "&5Number", "&7A numerical value.")),
	TRUE(mkItem(Material.LIME_TERRACOTTA, "&5True", "&7The boolean value 'true'.")),
	FALSE(mkItem(Material.RED_TERRACOTTA, "&5False", "&7The boolean value 'false'.")),
	UNIT(mkItem(Material.NETHERITE_BLOCK, "&5Unit", "&7A value holding no data.")),
	PARAMETER(mkItem(Material.PRISMARINE_BRICKS, "&5Parameter", "&7Variables associated with an event or function.")),
	ITEM(mkItem(Material.BARREL, "&5Item", "&7An item.")),
	LOCATION(mkItem(Material.TARGET, "&5Location", "&7A location in the world.")),
	LIST(mkItem(Material.END_STONE_BRICKS, "&5List", "&7A list of multiple values.")),
	MAP(mkItem(Material.MUD_BRICKS, "&5Map", "&7A mapping of strings to values.")),
	VARIABLE(mkItem(Material.OBSIDIAN, "&5Variable", "&7A value that can change.")),
	DECLARE(mkItem(Material.CRIMSON_HYPHAE, "&3Declare", "&7Declares a variable.")),
	SET(mkItem(Material.WARPED_HYPHAE, "&3Set", "&7Sets a previously declared variable.")),
	COMMENT(mkItem(Material.REDSTONE_LAMP, "&8Comment", "&7Text that is ignored. Useful for", "&7recording what something does."))
}

internal val blocksItem = mkItem(Material.DIAMOND, "&bCode Blocks");
internal val editingItems = listOf<ItemStack>(
	mkItem(Material.LIME_CANDLE, "&aAdd line", "&7Adds a line before a line.", "&7Left click to change."),
	mkItem(Material.RED_CANDLE, "&cRemove line", "&7Removes a line.", "&7Left click to change."),
	mkItem(Material.LIME_DYE, "&aAdd space", "&7Adds a space.", "&7Left click to change."),
	mkItem(Material.RED_DYE, "&cRemove space", "&7Removes a space.", "&7Left click to change.")
);

internal fun toggleCodeMode(p: Player) {
	var pd = Data.getPlayerData(p);
	pd.codeMode = !pd.codeMode;
	if(pd.codeMode) {
		pd.prevInv = p.inventory.contents;
		p.inventory.clear();
		p.inventory.addItem(blocksItem);
		p.inventory.addItem(editingItems[0]);
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

// Formats an instant
internal fun formatInstant(i: Instant) = LocalTime.ofInstant(i, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
