package net.mcwarlords.wlplugin.schema;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import goldendelicios.lite2edit.Converter;
import net.mcwarlords.wlplugin.*;

public class SchemaCommand implements CommandExecutor {
  void sendHelpMessage(Player p) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLSCHEMA &_s]======"));
    p.sendMessage(Utils.escapeText("&e&lWARNING: &_dwlschema does &_e&onot&_d check if any downloaded schematics or litematics are valid, it only downloads them. Use at your own risk."));
    p.sendMessage(Utils.escapeText("&_p/wlschema s | schematic &_e<url> <name> &_s- &_dDownloads a schematic from the given URL as &_e<name>&_d."));
    p.sendMessage(Utils.escapeText("&_p/wlschema l | litematic &_e<url> <name> &_s- &_dDownloads a litematic from the given URL as &_e<name>&_d."));
  }

  static final String invalidArguments = Utils.escapeText("&_p* &_eInvalid arguments.");

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if(!(sender instanceof Player)) {
      sender.sendMessage("wlplot can only be used by a player");
      return false;
    }
    Player p = (Player)sender;
    if(args.length == 0) {
      sendHelpMessage(p);
      return true;
    }
    switch(args[0]) {
      case "s":
      case "schematic": {
        if(args.length != 3) {
          p.sendMessage(invalidArguments);
          return true;
        }
        new BukkitRunnable() {
          public void run() {
            try {
              Utils.downloadTo(args[1], "plugins/FastAsyncWorldEdit/schematics/"+args[2]+".schem");
              p.sendMessage(Utils.escapeText("&_p* &_dSuccessfully downloaded schematic to &_e"+args[2]+".schem&_d."));
            } catch(MalformedURLException e) {
              p.sendMessage(Utils.escapeText("&_p* &_eInvalid URL."));
            } catch(IOException e) {
              p.sendMessage(Utils.escapeText("&_p* &_eError downloading file."));
              e.printStackTrace();
            }
          }
        }.runTaskAsynchronously(WlPlugin.instance);
        break;
      }
      case "l":
      case "litematic": {
        if(args.length != 3) {
          p.sendMessage(invalidArguments);
          return true;
        }
        new BukkitRunnable() {
          public void run() {
            try {
              String tmp = Utils.tmpFileName();
              Utils.downloadTo(args[1], "plugins/wlplugin/tmp/"+tmp+".litematic");
              p.sendMessage(Utils.escapeText("&_p* &_dLitematic Downloaded. Converting..."));
              List<File> res = Converter.litematicToWorldEdit(new File("plugins/wlplugin/tmp/"+tmp+".litematic"), new File("plugins/wlplugin/tmp"));
              if(res.isEmpty()) {
                p.sendMessage(Utils.escapeText("&_p* &_eError converting to litematic."));
                return;
              }
              // idk why they decided to return a List<File>
              File f = res.get(0);
              Files.move(Paths.get(f.getAbsolutePath()), Paths.get("plugins/FastAsyncWorldEdit/schematics/"+args[2]+".schem"));
              p.sendMessage(Utils.escapeText("&_p* &_dCleaning up temporary files..."));
              // f.delete();
              Files.delete(Paths.get("plugins/wlplugin/tmp/"+tmp+".litematic"));
              p.sendMessage(Utils.escapeText("&_p* &_dSuccessfully converted and saved to &_e"+args[2]+"&_d.schem&_d."));
            } catch(MalformedURLException e) {
              p.sendMessage(Utils.escapeText("&_p* &_eInvalid URL."));
            } catch(IOException e) {
              p.sendMessage(Utils.escapeText("&_p* &_eIO Error."));
              e.printStackTrace();
            }
          }
        }.runTaskAsynchronously(WlPlugin.instance);
        break;
      }
      case "h":
      case "help":
        sendHelpMessage(p);
        break;
      default:
        p.sendMessage(Utils.escapeText("&_p* &_eInvalid subcommand "+args[0]+"."));
    }
    return true;
  }
}
