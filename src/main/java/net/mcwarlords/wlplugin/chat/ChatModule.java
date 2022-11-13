package net.mcwarlords.wlplugin.chat;

import net.mcwarlords.wlplugin.*;

public class ChatModule implements Module {
  @Override public void onEnable() {
    WlPlugin.info("wlchat enabled");
    WlPlugin.addListener(new ChatListener());
    WlPlugin.addCommand("wlchat", new ChatCommand());
  }

  @Override public void onDisable() {
    WlPlugin.info("wlchat disabled");
  }
  
}
