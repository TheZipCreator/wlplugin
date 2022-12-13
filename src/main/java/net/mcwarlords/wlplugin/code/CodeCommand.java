package net.mcwarlords.wlplugin.code;

import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.*;

import net.mcwarlords.wlplugin.*;

public class CodeCommand implements CommandExecutor {
  private void sendHelpMessage(CommandSender p) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLCODE &_s]======"));
    p.sendMessage(Utils.escapeText("&eNote: &_dWherever you see <location> or [location], you either need to put 'x,y,z' (e.g. /wlcode trigger 63,50,4238) or 'cursor', which will autofill the location of the block you're facing. In commands where it's the last argument, you can also omit it, which assumes 'cursor'"));
    p.sendMessage(Utils.escapeText("&_p/wlcode h | help &_s- &_dShows this help message."));
    p.sendMessage(Utils.escapeText("&_s---< &_eExecution Commands &_s>---"));
    p.sendMessage(Utils.escapeText("&_p/wlcode t | trigger &_e[location] &_s- &_dTriggers a script."));
    p.sendMessage(Utils.escapeText("&_s---< &_eEditing Commands &_s>---"));
    p.sendMessage(Utils.escapeText("&_p/wlcode c | commands &_s- &_dOpens an inventory of commands you can choose from."));
    p.sendMessage(Utils.escapeText("&_p/wlcode e | expression &_e<expression> &_s- &_dCreates an expression."));
    p.sendMessage(Utils.escapeText("&_p/wlcode l | lvalue &_e<expression> &_s- &_dCreates an lvalue."));
  }

  class LocException extends RuntimeException {
    LocException(String msg) {
      super(msg);
    }
  }

  static final String invalidArguments = Utils.escapeText("&_p* &_eInvalid arguments.");

  @Override public boolean onCommand(CommandSender s, Command command, String label, String[] args) {
    // java doesn't have local functions so I have to do this
    Function<Integer, Location> getLoc = (Integer idx) -> {
      if(args.length < idx || args[idx].equals("cursor")) {
        if(!(s instanceof Player))
          throw new LocException("&cYou must be a player to use cursor location.");
        return ((Player)s).getTargetBlock(null, 20).getLocation();
      }
      return Utils.stringToLocation(args[idx]);
    };
    if(args.length == 0) {
      sendHelpMessage(s);
      return true;
    }
    try {
      switch(args[0]) {
        case "h":
        case "help":
          sendHelpMessage(s);
          break;
        case "t":
        case "trigger": {
          if(args.length != 1 && args.length != 2) {
            s.sendMessage(invalidArguments);
            break;
          }
          Location l = getLoc.apply(2);
          Interpreter interpreter = new Interpreter(l, (String msg) -> { s.sendMessage(msg); });
          new BukkitRunnable() {
            public void run() {
              try {
                interpreter.run();
              } catch(InterpreterException e) {
                interpreter.err(e.getMessage());
              }
            }
          }.runTaskAsynchronously(WlPlugin.instance);
          break;
        }
        case "c":
        case "command": {
          if(args.length != 1) {
            s.sendMessage(invalidArguments);
            break;
          }
          Inventory inv = Bukkit.createInventory(null, 9, "Commands");
          inv.addItem(Utils.createItem(Material.OAK_LOG, "&6LOG", new String[]{"&7Command", "&7Logs all arguments given"}));
          inv.addItem(Utils.createItem(Material.ENDER_CHEST, "&5SET", new String[]{"&7Command", "&7Takes an lvalue as the first argument (what to set)", " and an expression as the second argument (what to set it to)."}));
          if(s instanceof Player)
            ((Player)s).openInventory(inv);
          break;
        }
        case "e":
        case "expression": {
          if(args.length < 2) {
            s.sendMessage(invalidArguments);
            break;
          }
          String expr = args[1];
          for(int i = 2; i < args.length; i++)
            expr += " "+args[i];
          ItemStack item = Utils.createItem(Material.GOLD_INGOT, "&e"+expr, new String[]{"&7Expression"});
          if(s instanceof Player)
            ((Player)s).getInventory().addItem(item);
          break;
        }
        case "l":
        case "lvalue": {
          if(args.length < 2) {
            s.sendMessage(invalidArguments);
            break;
          }
          String lvalue = args[1];
          for(int i = 2; i < args.length; i++)
          lvalue += " "+args[i];
          ItemStack item = Utils.createItem(Material.NAME_TAG, "&6"+lvalue, new String[]{"&7Lvalue"});
          if(s instanceof Player)
            ((Player)s).getInventory().addItem(item);
        }
        default:
          s.sendMessage(Utils.escapeText("&_p* &_eUnknown subcommand: "+args[0]));
          break;
      }
    } catch(LocException e) {
      s.sendMessage(Utils.escapeText(e.getMessage()));
    }
    return true;
  }
  
}
