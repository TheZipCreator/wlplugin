// this listens for events that spawn an Executor

package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;

import net.mcwarlords.wlplugin.*;



object ExecutorListener : Listener {	
	@EventHandler fun onPlayerJoin(e: PlayerJoinEvent) = CJoinEvent(e).execute();
	@EventHandler fun onPlayerQuit(e: PlayerQuitEvent) = CQuitEvent(e).execute();
	@EventHandler fun onPlayerDropItem(e: PlayerDropItemEvent) = CDropItemEvent(e).execute();
	@EventHandler fun onEntityPickupItem(e: EntityPickupItemEvent) { 
		if(e.entity is Player)
			CPickupItemEvent(e).execute();
	}
	@EventHandler fun onPlayerEditBook(e: PlayerEditBookEvent) = CEditBookEvent(e).execute();
	@EventHandler fun onPlayerGameModeChange(e: PlayerGameModeChangeEvent) = CGameModeChangeEvent(e).execute();
	@EventHandler fun onPlayerInteractEntity(e: PlayerInteractEntityEvent) = CInteractEntityEvent(e).execute();
	@EventHandler fun onPlayerItemBreak(e: PlayerItemBreakEvent) = CItemBreakEvent(e).execute();
	@EventHandler fun onPlayerItemConsume(e: PlayerItemConsumeEvent) = CItemConsumeEvent(e).execute();
	@EventHandler fun onPlayerItemDamage(e: PlayerItemDamageEvent) = CItemDamageEvent(e).execute();
	@EventHandler fun onPlayerItemHeld(e: PlayerItemHeldEvent) = CSlotChangeEvent(e).execute();
	@EventHandler fun onPlayerMove(e: PlayerMoveEvent) = CMoveEvent(e).execute();
	@EventHandler fun onPlayerRespawn(e: PlayerRespawnEvent) = CRespawnEvent(e).execute();
	@EventHandler fun onPlayerSwapHands(e: PlayerSwapHandItemsEvent) = CSwapHandsEvent(e).execute();
	@EventHandler fun onBlockPlace(e: BlockPlaceEvent) = CPlaceBlockEvent(e).execute();
	@EventHandler fun onBlockBreak(e: BlockBreakEvent) = CBreakBlockEvent(e).execute();
	@EventHandler fun onPlayerInteract(e: PlayerInteractEvent) {
		when(e.action) {
			Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR -> CLeftClickEvent(e).execute()
			Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR -> 
				if(e.hand == EquipmentSlot.HAND && !(e.useInteractedBlock() == Event.Result.DENY && e.player.getTargetBlock(null, 5).type != Material.AIR)) 
					CRightClickEvent(e).execute()	
			else -> {}
		}
	}
	@EventHandler(priority = EventPriority.LOW) fun onPlayerChat(e: AsyncPlayerChatEvent) {
		var msg = e.message;
		if(!msg.startsWith("%"))
			return;
		msg = msg.substring(1);
		val args = msg.split(" ");
		if(args.size == 0)
			return;
		val player = e.player;
		e.setCancelled(true);
		runTask { CCommandEvent(player, args[0], args.subList(1, args.size)).execute() }
	}
}
