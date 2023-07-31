// this listens for events that spawn an Executor

package net.mcwarlords.wlplugin.code;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import kotlin.reflect.*;

import net.mcwarlords.wlplugin.*;

enum class CodeEvent(val event: KClass<*>) {
	JOIN(PlayerJoinEvent::class),
	QUIT(PlayerQuitEvent::class),
	INTERACT(PlayerInteractEvent::class)
}

object ExecutorListener : Listener {
	private fun handleEvent(e: PlayerEvent) {
		val pd = Data.getPlayerData(e.player);
		for(unitName in pd.subscribed) {
			if(!Data.codeUnits.containsKey(unitName))
				continue;
			Data.codeUnits[unitName]!!.handleEvent(e);
		}
	}
	
	// maybe there's a way I could auto-generate these methods?

	@EventHandler fun onPlayerJoin(e: PlayerJoinEvent) = handleEvent(e);
	@EventHandler fun onPlayerQuit(e: PlayerQuitEvent) = handleEvent(e);
	@EventHandler fun onPlayerInteract(e: PlayerInteractEvent) = handleEvent(e);
}
