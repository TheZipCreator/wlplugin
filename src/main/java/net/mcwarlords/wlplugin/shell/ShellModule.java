package net.mcwarlords.wlplugin.shell;

import java.nio.file.*;

import net.mcwarlords.wlplugin.*;

public class ShellModule implements Module {
  static {
    System.loadLibrary("plugins/wlplugin");
  }
  
  @Override public void onEnable() {
    WlPlugin.info("wlshell enabled");
    WlPlugin.addCommand("wlshell", new ShellCommand());
  }

  @Override public void onDisable() {
    WlPlugin.info("wlshell disabled");
  }
}
