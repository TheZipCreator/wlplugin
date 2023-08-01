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
}

interface CCancellable {
	var cancelled: Boolean;
}

interface CPlayerEvent : CEvent {
	val player: Player;
	fun execute() {
		val pd = Data.getPlayerData(player);
		for(unitName in pd.subscribed) {
			if(!Data.codeUnits.containsKey(unitName))
				continue;
			Data.codeUnits[unitName]!!.handleEvent(this);
		}
	}
}

class CJoinEvent(private val impl: PlayerJoinEvent) : CPlayerEvent {
	override val name = "join"
	override val player = impl.player;
}

class CQuitEvent(private val impl: PlayerQuitEvent) : CPlayerEvent {
	override val name = "quit"
	override val player = impl.player;
}

abstract class CClickEvent(private val impl: PlayerInteractEvent, override val name: String) : CPlayerEvent, CCancellable {
	override var cancelled: Boolean
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b)
	override val player = impl.player;
}

class CLeftClickEvent(impl: PlayerInteractEvent) : CClickEvent(impl, "left-click");
class CRightClickEvent(impl: PlayerInteractEvent) : CClickEvent(impl, "right-click");

abstract class CSimplePlayerEvent(override val player: Player, override val name: String) : CPlayerEvent;

class CLoopEvent(p: Player) : CSimplePlayerEvent(p, "loop")
class CSubscribeEvent(p: Player) : CSimplePlayerEvent(p, "subscribe")
class CUnsubscribeEvent(p: Player) : CSimplePlayerEvent(p, "unsubscribe")

// doesn't seem like there's a way to automate this
internal val validEvents = setOf(
	"join", "quit", "left-click", "right-click", "loop", "subscribe", "unsubscribe"
);
