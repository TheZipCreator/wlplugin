package net.mcwarlords.wlplugin.game;

import net.mcwarlords.wlplugin.*;

public class GameModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlgame enabled");
    WlPlugin.addCommand("wlgame", new GameCommand());
  }

  @Override public void onDisable() {
    WlPlugin.info("wlgame disabled");
  }
  
}
