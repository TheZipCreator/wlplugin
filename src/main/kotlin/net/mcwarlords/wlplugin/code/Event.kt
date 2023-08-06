package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
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
abstract class CSimpleEvent(override val name: String) : CEvent;

// ran when the unit is initialized
class CInitEvent() : CSimpleEvent("init");
// ran when data is being saved. this is a good place to store things to cache!
// note: this event is the only one that runs sychronously. do not do any large operations here.
class CCacheEvent() : CSimpleEvent("cache") {
	override fun execute() {
		for(u in Data.codeUnits.values)
			u.handleEvent(this, true);
	}
}
// ran every tick
class CLoopEvent() : CSimpleEvent("loop")

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

interface CLocationEvent : CEvent {
	val location: Location;
}

interface CItemEvent : CEvent {
	val item: ItemStack?;
}

interface CEntityEvent : CEvent {
	val entity: Entity;
}

// interface for a click event
abstract class CClickEvent(private val impl: PlayerInteractEvent, override val name: String) : CPlayerEvent, CCancellable, CLocationEvent, CItemEvent {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val player = impl.player;
	override val location = impl.clickedBlock?.location ?: impl.player.getTargetBlock(null, 5).location;
	override val item = impl.item;
}

// when the player left clicks
class CLeftClickEvent(impl: PlayerInteractEvent) : CClickEvent(impl, "left-click");
// when the player right clicks
class CRightClickEvent(impl: PlayerInteractEvent) : CClickEvent(impl, "right-click");

// superclass for simple player events
abstract class CSimplePlayerEvent(override val player: Player, override val name: String) : CPlayerEvent;

// ran when a player subscribes
class CSubscribeEvent(p: Player) : CSimplePlayerEvent(p, "subscribe")
// ran when a player unsubscribes
class CUnsubscribeEvent(p: Player) : CSimplePlayerEvent(p, "unsubscribe")

// when a unit command is run
class CCommandEvent(override val player: Player, val command: String, val args: List<String>) : CPlayerEvent {
	override val name = "command";
}

// when a player drops an item
class CDropItemEvent(private val impl: PlayerDropItemEvent) : CSimplePlayerEvent(impl.player, "drop-item"), CCancellable, CItemEvent {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val item = impl.itemDrop.itemStack;
}

class CPickupItemEvent(private val impl: EntityPickupItemEvent) : CSimplePlayerEvent(impl.entity as Player, "pickup-item"), CCancellable, CItemEvent {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val item = impl.item.itemStack;
}

// when a player edits a book
class CEditBookEvent(private val impl: PlayerEditBookEvent) : CSimplePlayerEvent(impl.player, "edit-book"), CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b)
}

// when a player's gamemode changes
class CGameModeChangeEvent(private val impl: PlayerGameModeChangeEvent) : CSimplePlayerEvent(impl.player, "game-mode-change"), CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	val gamemode = impl.newGameMode;
}

// when a player interacts with an entity
class CInteractEntityEvent(private val impl: PlayerInteractEntityEvent) : CSimplePlayerEvent(impl.player, "interact-entity"), CEntityEvent, CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val entity = impl.rightClicked;
}

class CItemBreakEvent(private val impl: PlayerItemBreakEvent) : CSimplePlayerEvent(impl.player, "break-item"), CItemEvent {
	override val item = impl.brokenItem;
}

class CItemConsumeEvent(private val impl: PlayerItemConsumeEvent) : CSimplePlayerEvent(impl.player, "consume-item"), CItemEvent, CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val item = impl.item;
}

class CItemDamageEvent(private val impl: PlayerItemDamageEvent) : CSimplePlayerEvent(impl.player, "damage-item"), CItemEvent, CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val item = impl.item;
}

interface CSlotEvent : CEvent {
	val slot: Int;
}

class CSlotChangeEvent(private val impl: PlayerItemHeldEvent) : CSimplePlayerEvent(impl.player, "slot-change"), CSlotEvent, CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val slot = impl.newSlot;
}

class CMoveEvent(private val impl: PlayerMoveEvent) : CSimplePlayerEvent(impl.player, "move"), CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
}

class CRespawnEvent(impl: PlayerRespawnEvent) : CSimplePlayerEvent(impl.player, "respawn");

class CSwapHandsEvent(private val impl: PlayerSwapHandItemsEvent) : CSimplePlayerEvent(impl.player, "swap-hands"), CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
}

interface CBlockEvent : CEvent, CLocationEvent {
	val block: Block
}

class CPlaceBlockEvent(private val impl: BlockPlaceEvent) : CSimplePlayerEvent(impl.player, "place-block"), CBlockEvent, CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val block = impl.block;
	override val location = impl.block.location;
}

class CBreakBlockEvent(private val impl: BlockBreakEvent) : CSimplePlayerEvent(impl.player, "break-block"), CBlockEvent, CLocationEvent, CCancellable {
	override var cancelled
		get() = impl.isCancelled()
		set(b) = impl.setCancelled(b);
	override val block = impl.block;
	override val location = impl.block.location;
}

// doesn't seem like there's a way to automate this
internal val validEvents = setOf(
	// player
	"join", "quit", "left-click", "right-click", "drop-item", "edit-book", "game-mode-change", "interact-entity", "break-item", "consume-item", "damage-item", 
	"slot-change", "move", "pickup-item", "respawn", "swap-hands", "place-block", "break-block",
	"subscribe", "unsubscribe", "command", 
	// unit
	"loop", "init", "cache"
);
