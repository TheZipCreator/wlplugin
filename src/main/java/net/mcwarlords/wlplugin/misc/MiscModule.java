package net.mcwarlords.wlplugin.misc;


import net.mcwarlords.wlplugin.*;

public class MiscModule implements Module {

  @Override public void onEnable() {
    WlPlugin.addCommand("wlmisc", new MiscCommand());
    WlPlugin.info("wlmisc enabled");
  }

  @Override public void onDisable() {
    WlPlugin.info("wlmisc disabled");
  }
  
}
