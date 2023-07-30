package net.mcwarlords.wlplugin.misc;

import java.util.*;
import java.util.function.Consumer;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;

import net.mcwarlords.wlplugin.*;

public class MiscCommand implements CommandExecutor {
  private void sendHelpMessage(Player p) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLMISC &_s]======"));
    p.sendMessage(Utils.escapeText("&_p/wlmisc h | help &_s- &_dDisplays this help information."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc r | roll <# of dice> <# of faces on dice> &_s- &_dRolls dice."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc rh | rollhere <# of dice> <# of faces on dice> &_s- &_dRolls dice and displays the result in your current channel."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc s | select &_s- &_dToggles selection mode."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc 1 | selstart &_s- &_dSets your current position to the selection start."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc 2 | selend &_s- &_dSets your current position to the selection end."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc c | compactify &_s- &_dCompactifies the current selection into a single block."));
    p.sendMessage(Utils.escapeText("&_p/wlmisc cs | csolid &_s- &_dChanges the solidity of a compactified structure."));
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
        if(count < 1 || count > 1000) {
          p.sendMessage(Utils.escapeText("&_p* &_dThe amount of dice must be between 1 and 1000"));
          return;
        }
        int faces = Integer.parseInt(args[2]);
        if(faces < 1) {
          p.sendMessage(Utils.escapeText("&_p* &_dFaces must be > 0"));
        }
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
        p.sendMessage(Utils.escapeText("&_p* &_dUnknown subcommand."));
        break;
			case "c":
			case "compactify": {
				if(!pd.hasSelection()) {
        	p.sendMessage(Utils.escapeText("&_p* &_eYour selection is empty. Use /wlmisc select to create a selection first."));
					break;
				}
				int[] size = pd.selectionSize();
				if(size[0]*size[1]*size[2] > 10000) {
        	p.sendMessage(Utils.escapeText("&_p* &_eMaximum volume of a compactified structure is 10000."));
					break;
				}
				// check total number of blocks
				{
					int amt = 0;
					Location a = pd.getSelection()[0];
					World w = a.getWorld();
					int x = a.getBlockX(), y = a.getBlockY(), z = a.getBlockZ();
					for(int i = 0; i < size[0]; i++)
						for(int j = 0; j < size[1]; j++)
							for(int k = 0; k < size[2]; k++) {
								Material m = w.getBlockAt(x+i, y+j, z+k).getType();
								if(m != Material.AIR && m != Material.BARRIER)
									amt++;
							}
					if(amt > 4096) {
						p.sendMessage(Utils.escapeText("&_p* &_eMaximum number of visible blocks for a structure is 4096."));
						break;
					}
				}
				p.sendMessage(Utils.escapeText("&_p* &_dSuccessfully compactified block."));
				Compactified c = new Compactified(pd.getSelection());
				p.getInventory().addItem(c.toItem());
				break;
			}
			case "csolid":
			case "cs": {
				PlayerInventory inv = p.getInventory();
				ItemStack is = inv.getItemInMainHand();
				if(!Compactified.is(is)) {
					p.sendMessage(Utils.escapeText("&_p* &_eYou must be holding a structure to do this command."));
					break;
				}
				Compactified c;
				try {
					c = new Compactified(is);
				} catch(IllegalArgumentException e) {
					p.sendMessage(Utils.escapeText("&_p* &_eInvalid structure: "+e.getMessage()));
					break;
				}
				c.solid = !c.solid;
				inv.setItemInMainHand(c.toItem());
				p.sendMessage(Utils.escapeText("&_p* &_dStructure is now "+(c.solid ? "solid" : "not solid")));
				break;
			}
			case "1":
			case "selstart":
				MiscListener.setSelectionPos(true, p, p.getLocation());
				break;
			case "2":
			case "selend":
				MiscListener.setSelectionPos(false, p, p.getLocation());
				break;
			case "s":
			case "select":
				pd.selecting = !pd.selecting;
				if(pd.selecting)
					p.sendMessage(Utils.escapeText("&_p* &_dStarted selecting. Right click to set the first position, left click to set the second position."));
				else
					p.sendMessage(Utils.escapeText("&_p* &_dStopped selecting."));
      	break;
			case "help":
        sendHelpMessage(p);
        break;
    }
    return true;
  }
}
