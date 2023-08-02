package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import kotlin.reflect.*;

import net.mcwarlords.wlplugin.*;

// NOTE: these are all prefixed with `C` to distinguish them from the spigot versions
// this is just an ease-of-use thing.

// A wlcode event
sealed interface CEvent {
	val name: String;
	// runs code for all modules
	fun execute() {
		for(u in Data.codeUnits.values)
			u.handleEvent(this);
	}
}

// for events containing just a name
abstract class CSimpleEvent(val name: String);

// ran when the unit is initialized
class CInitEvent() : CSimpleEvent("init");
// ran when data is being saved. this is a good place to store things to cache!
// note: this event is the only one that runs sychronously. do not do any large operations here.
class CCache() : CSimpleEvent("cache");

// a cancellable event
interface CCancellable {
	var cancelled: Boolean;
}

// a player event
interface CPlayerEvent : CEvent {
	val player: Player;
	// runs event for all players
	override fun execute() {
		val pd = Data.getPlayerData(player);
		for(unitName in pd.subscribed) {
			if(!Data.codeUnits.containsKey(unitName))
				continue;
			Data.codeUnits[unitName]!!.handleEvent(this);
		}
	}
}

// runs when a player joins
class CJoinEvent(private val impl: PlayerJoinEvent) : CPlayerEvent {
	override val name = "join"
	override val player = impl.player;
}

// runs when a player quits
class CQuitEvent(private val impl: PlayerQuitEvent) : CPlayerEvent {
	override val name = "quit"
	override val player = impl.player;
}

// interface for a click event
abstract class CClickEvent(private val impl: PlayerInteractEvent, override val name: String) : CPlayerEvent, CCancellable {
	override var cancelled: Boolean
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b)
	override val player = impl.player;
}

// when the player left clicks
class CLeftClickEvent(impl: PlayerInteractEvent) : CClickEvent(impl, "left-click");
// when the player right clicks
class CRightClickEvent(impl: PlayerInteractEvent) : CClickEvent(impl, "right-click");

// superclass for simple custom player events
abstract class CSimplePlayerEvent(override val player: Player, override val name: String) : CPlayerEvent;

// ran every tick
class CLoopEvent(p: Player) : CSimplePlayerEvent(p, "loop")
// ran when a player subscribes
class CSubscribeEvent(p: Player) : CSimplePlayerEvent(p, "subscribe")
// ran when a player unsubscribes
class CUnsubscribeEvent(p: Player) : CSimplePlayerEvent(p, "unsubscribe")

// when a unit command is run
class CCommandEvent(override val player : Player, val command: String, val args: List<String>) : CPlayerEvent {
	override val name = "command";
}

// doesn't seem like there's a way to automate this
internal val validEvents = setOf(
	"join", "quit", "left-click", "right-click", "loop", "subscribe", "unsubscribe", "command"
);
