package net.mcwarlords.wlplugin.misc;

import java.util.function.*;

import net.mcwarlords.wlplugin.*;

import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.block.*;
import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.persistence.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

public class MiscListener implements Listener {
	// probably should be somewhere else but I can't be bothered
	public static void setSelectionPos(boolean start, Player p, Location l) {
		// round location
		l = l.getBlock().getLocation();
		// create message and set selection
		PlayerData pd = Data.getPlayerData(p);
		String msg = "&_p* &_dSet selection ";
		if(start) {
			pd.selStart = l;
			msg += "start";
		} else {
			pd.selEnd = l;
			msg += "end";
		}
		msg += " to &_e("+l.getX()+", "+l.getY()+", "+l.getZ()+")";
		if(pd.hasSelection()) {
			Integer[] size = pd.selectionSize();
			msg += " ["+size[0]+"×"+size[1]+"×"+size[2]+"]";
		}
		p.sendMessage(Utils.escapeText(msg));
	}
	@EventHandler 
	public void onPlayerInteract(PlayerInteractEvent e) {
		if(e.isCancelled())
			return;
		Action a = e.getAction();
		if(a != Action.RIGHT_CLICK_BLOCK && a != Action.LEFT_CLICK_BLOCK)
			return;
		if(e.getHand() != EquipmentSlot.HAND)
			return;
		Player p = e.getPlayer();
		PlayerData pd = Data.getPlayerData(p);
		if(!pd.selecting)
			return;
		e.setCancelled(true);
		setSelectionPos(a == Action.LEFT_CLICK_BLOCK, p, p.getTargetBlock(null, 20).getLocation());
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		if(e.isCancelled())
			return;
		ItemStack is = e.getItemInHand();
		if(!Compactified.is(is))
			return;
		// place compactified structures
		e.setCancelled(true);
		Compactified c = null;
		try {
			c = new Compactified(is);
		} catch(IllegalArgumentException ex) {
			e.getPlayer().sendMessage(Utils.escapeText("&_p* &_cError deserializing structure: "+ex.getMessage()));
			return;
		}
		Block b = e.getBlockPlaced();
		// this shouldn't be necessary but doing
		// b.setType() alone doesn't fucking work for some reason. I have no idea why.
		final boolean solid = c.solid;
		Bukkit.getScheduler().runTaskLater(WlPlugin.instance, () -> {
			b.setType(solid ? Material.BARRIER : Material.STRUCTURE_VOID);
		}, 1);
		c.place(b.getLocation());
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		if(b.getType() != Material.BARRIER && b.getType() != Material.STRUCTURE_VOID)
			return;
		// destroy compactified structures
		for(Entity bd : b.getWorld().getEntitiesByClass(BlockDisplay.class)) {
			if(bd.getLocation().getBlock().equals(b))
				bd.remove();
		}
	}

}
