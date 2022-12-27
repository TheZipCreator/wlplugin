package net.mcwarlords.wlplugin.misc;

import java.util.*;
import java.util.function.Consumer;

import org.bukkit.command.*;
import org.bukkit.entity.*;

import net.mcwarlords.wlplugin.*;

public class MiscCommand implements CommandExecutor {
  private void sendHelpMessage(Player p) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLMISC &_s]======"));
    p.sendMessage(Utils.escapeText("&_p/wlmisc h | help &_s- &_dDisplays this help information."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc r | roll <# of dice> <# of faces on dice> &_s- &_dRolls dice."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc rh | rollhere <# of dice> <# of faces on dice> &_s- &_dRolls dice and displays the result in your current channel."));
  }

  static final String invalidArguments = Utils.escapeText("&_p* &_eInvalid arguments.");

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if(!(sender instanceof Player)) {
      sender.sendMessage("wlmisc can only be used by a player");
      return false;
    }
    Player p = (Player)sender;
    if(args.length == 0) {
      sendHelpMessage(p);
      return true;
    }
    PlayerData pd = Data.getPlayerData(p);
    Consumer<String> roll = (String channel) -> {
      if(args.length != 3) {
        p.sendMessage(invalidArguments);
        return;
      }
      try {
        int count = Integer.parseInt(args[1]);
        int faces = Integer.parseInt(args[2]);
        int sum = 0;
        String msg = "&_p* &_e"+count+"d"+faces+"&_d: ";
        for(int i = 0; i < count; i++) {
          int r = WlPlugin.rand.nextInt(faces)+1;
          msg += r+" ";
          sum += r;
        }
        msg += "\n&_p* &_dTotal: "+sum;
        if(channel != null) {
          Utils.sendMessage(channel, msg);
          return;
        }
        p.sendMessage(Utils.escapeText(msg));
      } catch(NumberFormatException e) {
        sender.sendMessage(Utils.escapeText("&_p* &_eInvalid number(s)."));
      }
    };
    switch(args[0]) {
      case "roll":
      case "r":
        roll.accept(null);
        break;
      case "rollhere":
      case "rh":
        roll.accept(pd.channel);
        break;
      default:
        p.sendMessage("&_p* &_dUnknown subcommand.");
        break;
      case "help":
        sendHelpMessage(p);
        break;
    }
    return true;
  }
}
