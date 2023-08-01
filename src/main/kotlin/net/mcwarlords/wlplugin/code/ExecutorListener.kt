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
}
