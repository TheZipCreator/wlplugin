package net.mcwarlords.wlplugin.shell;

import org.bukkit.command.*;

public class Shell {
  public CommandSender host;
  public native void run(String cmd);
  private native void ctor();
  public native void addCommand(String name, JavaCommand jc);
  public native Value getVar(String name);
  public native void setVar(String name, Value val);

  Shell(CommandSender host) {
    this.host = host;
    ctor();
  }
}
