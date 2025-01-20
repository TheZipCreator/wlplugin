package net.mcwarlords.wlplugin.game;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;
import org.bukkit.command.*;
import java.util.*;

public class GameModule : SimpleModule {
	override val name = "wlgame";

	override fun registerFields() {
		// TODO: migrate game data to fields
	}

  override fun enabled() {
    WlPlugin.info("wlgame enabled");
		GameCommand.register();
  }

  override fun disabled() {
    WlPlugin.info("wlgame disabled");
  }
  
}
