// this listens for events that spawn an Executor

package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;

import net.mcwarlords.wlplugin.*;



object ExecutorListener : Listener {	
	@EventHandler fun onPlayerJoin(e: PlayerJoinEvent) = CJoinEvent(e).execute();
	@EventHandler fun onPlayerQuit(e: PlayerQuitEvent) = CQuitEvent(e).execute();
	@EventHandler fun onPlayerInteract(e: PlayerInteractEvent) {
		when(e.action) {
			Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR -> CLeftClickEvent(e).execute()
			Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR -> if(e.getHand() != EquipmentSlot.OFF_HAND) CRightClickEvent(e).execute()
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
