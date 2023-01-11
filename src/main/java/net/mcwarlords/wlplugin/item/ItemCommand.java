package net.mcwarlords.wlplugin.item;

import java.util.*;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import net.mcwarlords.wlplugin.*;

public class ItemCommand implements CommandExecutor {
  private void sendHelpMessage(Player p) {
    p.sendMessage(Utils.escapeText("&_s======[ &_eWLITEM &_s]======"));
    p.sendMessage(Utils.escapeText("&_p/wlitem h | help &_s- &_dDisplays this help information."));
    p.sendMessage(Utils.escapeText("&_p/wlitem n | rename &_e<name> &_s- &_dRenames an item to a new name."));
    p.sendMessage(Utils.escapeText("&_p/wlitem l | setlore &_e<line> <name> &_s- &_dSets a specific line of lore to something."));
    p.sendMessage(Utils.escapeText("&_p/wlitem a | addlore &_e<name> &_s- &_dAppends lore to an item."));
    p.sendMessage(Utils.escapeText("&_p/wlitem r | rmlore &_e[line] &_s- &_dRemoves a given lore line. If no line is specified, it removes all lines."));
    p.sendMessage(Utils.escapeText("&_p/wlitem hf | hideflags &_s- &_dHides all flags."));
    p.sendMessage(Utils.escapeText("&_p/wlitem sf | showflags &_s- &_dShows all flags."));
    p.sendMessage(Utils.escapeText("&_p/wlitem u | unsign &_s- &_dUnsigns a book."));
    p.sendMessage(Utils.escapeText("&_p/wlitem ub | unbreakable &_s- &_dMakes an item unbreakable."));
  }

  static final String invalidArguments = Utils.escapeText("&_p* &_eInvalid arguments.");

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if(!(sender instanceof Player)) {
      sender.sendMessage("wlitem can only be used by a player");
      return false;
    }
    Player p = (Player)sender;
    if(args.length == 0) {
      sendHelpMessage(p);
      return true;
    }
    // local function to make string of all args after a given index
    Function<Integer, String> getText = (Integer i) -> {
      String ret = "";
      for(int j = i; j < args.length; j++) {
        if(j != i)
          ret += " ";
        ret += args[j];
      }
      return Utils.escapeText(ret);
    };
    ItemStack is = p.getInventory().getItemInMainHand();
    ItemMeta im = is.getItemMeta();
    switch(args[0]) {
      case "help":
      case "h":
        sendHelpMessage(p);
        return true;
      case "n":
      case "rename":
        im.setDisplayName(getText.apply(1));
        p.sendMessage(Utils.escapeText("&_p* &_dItem successfully renamed."));
        break;
      case "l":
      case "relore":
      case "setlore":
        if(args.length <= 1) {
          p.sendMessage(invalidArguments);
          return true;
        }
        try {
          int l = Integer.valueOf(args[1]);
          if(l < 0)
            break;
          List<String> lore = im.getLore();
          if(lore == null)
            lore = new ArrayList<String>();
          while(lore.size() < l)
            lore.add("");
          lore.set(l, getText.apply(2));
          im.setLore(lore);
          p.sendMessage(Utils.escapeText("&_p* &_dItem successfully relored."));
        } catch(NumberFormatException e) {
          p.sendMessage(Utils.escapeText("&_p* &_eInvalid number "+args[1]+"."));
          return true;
        }
        break;
      case "a":
      case "addlore": {
        List<String> lore = im.getLore();
        if(lore == null)
          lore = new ArrayList<String>();
        lore.add(getText.apply(1));
        im.setLore(lore);
        p.sendMessage(Utils.escapeText("&_p* &_dLore succesfully added to item."));
        break;
      }
      case "r":
      case "rmlore":
        if(args.length <= 1) {
          im.setLore(new ArrayList<String>());
          p.sendMessage(Utils.escapeText("&_p* &_dLore successfully removed."));
          break;
        }
        try {
          int l = Integer.valueOf(args[1]);
          List<String> lore = im.getLore();
          if(lore == null || l >= lore.size() || l < 0)
            break;
          lore.remove(l);
          im.setLore(lore);
          p.sendMessage(Utils.escapeText("&_p* &_dLore successfully removed."));
        } catch(NumberFormatException e) {
          p.sendMessage(Utils.escapeText("&_p* &_eInvalid number "+args[1]+"."));
          return true;
        }
        break;
      case "hf":
      case "hideflags":
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        p.sendMessage(Utils.escapeText("&_p* &_dFlags hidden."));
        break;
      case "sf":
      case "showflags":
        im.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        p.sendMessage(Utils.escapeText("&_p* &_dFlags shown."));
        break;
      case "u":
      case "unsign": {
        if(is.getType() != Material.WRITTEN_BOOK) {
          p.sendMessage(Utils.escapeText("&_p* &_eYou must be holding a signed book!"));
          return true;
        }
        BookMeta old = (BookMeta)im;
        ItemStack newItem = new ItemStack(Material.WRITABLE_BOOK, 1);
        BookMeta newMeta = (BookMeta)newItem.getItemMeta();
        newMeta.setPages(old.getPages());
        newItem.setItemMeta(newMeta);
        p.getInventory().setItemInMainHand(newItem);
        p.sendMessage(Utils.escapeText("&_p* &_dSuccesfully unsigned book."));
        return true;
      }
      case "ub":
      case "unbreakable":
        im.setUnbreakable(!im.isUnbreakable());
        p.sendMessage(Utils.escapeText("&_p* &_dSet unbreakable to "+im.isUnbreakable()+"."));
        break;
      default:
        p.sendMessage(Utils.escapeText("&_p* &_eUnknown subcommand: "+args[0]));
        return true;
    }
    if(is == null || is.getType() == Material.AIR)
      return true;
    is.setItemMeta(im);
    p.getInventory().setItemInMainHand(is);
    return true;
  }
}
