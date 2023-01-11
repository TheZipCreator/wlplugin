package net.mcwarlords.wlplugin.code;

import net.mcwarlords.wlplugin.*;
import net.mcwarlords.wlplugin.Module;

public class CodeModule implements Module {

  @Override public void onEnable() {
    WlPlugin.addCommand("wlcode", new CodeCommand());
    WlPlugin.info("wlcode enabled");
  }

  @Override public void onDisable() {
    WlPlugin.info("wlcode disabled");
  }
  
}
