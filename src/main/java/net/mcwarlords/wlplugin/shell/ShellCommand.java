package net.mcwarlords.wlplugin.shell;

import org.bukkit.command.*;
import org.bukkit.scheduler.BukkitRunnable;

import net.mcwarlords.wlplugin.Utils;
import net.mcwarlords.wlplugin.WlPlugin;

public class ShellCommand implements CommandExecutor {

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String cmd = "";
    for(int i = 0; i < args.length; i++) {
      if(i != 0)
        cmd += " ";
      cmd += args[i];
    }
    final String c = cmd;
    new BukkitRunnable() {
      public void run() {
        try {
          Shell s = new Shell(sender);
          s.run(c);
        } catch(RuntimeException e) {
          // exception from D
          sender.sendMessage(Utils.escapeText("&c")+e.getMessage());
        }
      }
    }.runTaskAsynchronously(WlPlugin.instance);
    return true;
  }
}
